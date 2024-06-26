/**
 *@author Nikolaus Knop
 */

package hextant.core.editor

import hextant.completion.Completion
import hextant.context.Context
import hextant.context.executeSafely
import hextant.context.withoutUndo
import hextant.core.Editor
import hextant.core.editor.Expander.State.Expanded
import hextant.core.editor.Expander.State.Text
import hextant.core.view.ExpanderView
import hextant.core.view.ListEditorControl
import hextant.serial.*
import hextant.undo.AbstractEdit
import hextant.undo.UndoManager
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.safeCast
import kotlin.reflect.jvm.jvmErasure

/**
 * Expanders can be imagined as placeholders for more specific editors.
 * They allow the user to type in some text and then *expand* this text into a new editor,
 * which is then substituted for the typed in text.
 */
abstract class Expander<out R, E : Editor<R>>(context: Context) : AbstractEditor<R, ExpanderView>(context),
                                                                  TokenType<R> {
    private val editorClass = this::class.memberFunctions.first { it.name == "expand" }.returnType.jvmErasure

    private val resultType = this::class.memberFunctions.first { it.name == "defaultResult" }.returnType

    constructor(context: Context, editor: E?) : this(context) {
        if (editor != null) withoutUndo { expand(editor) }
    }

    constructor(context: Context, text: String) : this(context) {
        context.withoutUndo { setText(text) }
    }

    private val state: ReactiveVariable<State<E>> = reactiveVariable(initial)

    /**
     * A [ReactiveValue] holding the current text of the editor or `null` if it is expanded
     */
    val text: ReactiveValue<String?> get() = state.map { (it as? Text)?.text }

    /**
     * A [ReactiveValue] holding the currently wrapped editor or `null` if the expander is not expanded
     */
    val editor: ReactiveValue<E?> get() = state.map { (it as? Expanded)?.content }

    /**
     * @return `true` only if the expander is expanded
     */
    val isExpanded: ReactiveBoolean = state.map { it is Expanded }

    override val result: ReactiveValue<R> by lazy {
        state.flatMap { s ->
            when (s) {
                is Text ->
                    if (s.completion == null) reactiveValue(tryCompile(s.text))
                    else reactiveValue(tryCompile(s.completion) ?: tryCompile(s.text))
                is Expanded -> s.content.result
            }
        }
    }

    /**
     * Return the editor that should be wrapped if the expander
     * is expanded with the given [text] or `null` if the text is not valid.
     */
    protected open fun expand(text: String): E? = null

    /**
     * Return the editor that should be wrapped if the given [text] is typed into this expander
     * or `null` if the text is not auto-expandable.
     */
    protected open fun autoExpand(text: String): Boolean {
        if (text.endsWith(",") && parent is ListEditor<*, *>) {
            val listEditor = parent as ListEditor<*, *>
            val (index) = accessor.now as IndexAccessor
            val addWithComma = listEditor.viewManager.listeners.any { v ->
                v is ListEditorControl && v.arguments[ListEditorControl.ADD_WITH_COMMA]
            }
            if (addWithComma) {
                listEditor.addAt(index + 1)
                notifyViews { setText(text.removeSuffix(",")) }
                return true
            }
        }
        return false
    }

    protected fun autoExpandTo(editor: E): Boolean {
        executeEdit("AutoExpand") {
            expand(editor)
        }
        return true
    }

    /**
     * Create an editor from the given [completion] or return `null` if it is not possible.
     *
     * The default implementation returns `null`.
     */
    protected open fun expand(completion: Any): E? = null

    override fun compile(token: String): R = defaultResult()

    /**
     * Can be overwritten by extending classes to be notified when [expand] was successfully called
     */
    protected open fun onExpansion(editor: E) {}

    /**
     * Can be overwritten by extending classes to be notified when [reset] was successfully called
     */
    protected open fun onReset(editor: E) {}

    /**
     * Returns the result that this token editor should have if it is not expanded.
     *
     * You must override this method if the result type of your editor is not nullable.
     * Otherwise the default implementation will throw an [IllegalStateException].
     * If the default implementation is called on a token editor whose result type is nullable it just returns null.
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun defaultResult(): R =
        if (resultType.isMarkedNullable) null as R
        else error("Expander ${this::class}: non-nullable result type and defaultResult() was not overwritten")

    /**
     * Return `true` iff the given editor can be the content of this expander.
     * The defaults implementation always returns `true`
     */
    protected open fun accepts(editor: E): Boolean = true

    /**
     * Return the [Context] that expanded editors should be using.
     *
     * The default implementation returns the [context] of this Expander.
     */
    protected open fun expansionContext(): Context = context

    private fun forceText(): String = text.now ?: error("Expected expander to be unexpanded")

    private fun forceEditor(): E = editor.now ?: error("Expected expander to be expanded")

    private fun tryExpand(text: String) = context.executeSafely("expanding", null) { expand(text) }

    private fun tryExpand(item: Any) = context.executeSafely("expanding", null) { expand(item) }

    private fun tryCompile(text: String): R =
        context.executeSafely("compiling", ::defaultResult) { compile(text) }

    @Suppress("UNCHECKED_CAST")
    private fun tryCompile(item: Any): R? = resultType.jvmErasure.safeCast(item) as R?

    protected fun executeEdit(description: String, action: () -> Unit) {
        val undo = context[UndoManager]
        if (!undo.isActive) action()
        else {
            val before = state.now
            action()
            val after = state.now
            val edit = StateTransition(virtualize(), before, after, description)
            undo.record(edit)
        }
    }

    /**
     * Set the text of this expander.
     * @throws IllegalStateException if the expander is expanded
     */
    fun setText(newText: String) {
        forceText()
        val autoExpanded = context.executeSafely("auto-expanding", false) { autoExpand(newText) }
        if (!autoExpanded) {
            executeEdit("Type") {
                state.now = Text(newText, null)
                notifyViews { displayText(newText) }
            }
        }
    }

    /**
     * Expand the current text.
     * @throws IllegalStateException if already expanded
     */
    fun expand() {
        val text = forceText()
        val editor = tryExpand(text)
        if (editor != null) {
            executeEdit("Expand") {
                expand(editor)
            }
        }
    }

    /**
     * Called when the user selects a [completion].
     *
     * Either creates an editor from the given completion and expands this expander
     * or simply compiles the completion and sets the displayed text.
     * @throws IllegalStateException if the expander is already expanded
     */
    fun complete(completion: Completion<*>) {
        executeEdit("Complete") {
            complete(completion.item, completion.completionText)
        }
    }

    private fun complete(item: Any, text: String) {
        forceText()
        val editor = tryExpand(item) ?: tryExpand(text)
        if (editor != null) expand(editor)
        else {
            state.set(Text(text, item))
            notifyViews { displayText(text) }
        }
    }

    /**
     * Expand this expander to the given [editor].
     *
     * If the given [editor] doesn't have the right [context] it is copied.
     */
    fun expand(editor: E) {
        val e = editor.moveTo(expansionContext())
        state.set(Expanded(e))
        this.parent?.let { editor.initParent(it) }
        @Suppress("DEPRECATION")
        editor.initExpander(this)
        @Suppress("DEPRECATION")
        editor.setAccessor(ExpanderContent)
        notifyViews { expanded(e) }
        onExpansion(editor)
    }

    /**
     * Reset the expander by setting the text to the empty string
     * @throws IllegalStateException if not expanded
     */
    fun reset() {
        executeEdit("Reset") {
            val old = forceEditor()
            state.set(initial)
            onReset(old)
            notifyViews { reset() }
        }
    }

    private fun reconstructState(state: State<E>) {
        when (state) {
            is Text     -> {
                if (isExpanded.now) reset()
                if (state.completion == null) setText(state.text)
                else complete(state.completion, state.text)
            }
            is Expanded -> expand(state.content)
        }
    }

    @Deprecated("Treat as internal")
    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override fun initParent(parent: Editor<*>) {
        super.initParent(parent)
        editor.now?.initParent(parent)
    }

    @Suppress("UNCHECKED_CAST")
    override fun paste(snapshot: Snapshot<out Editor<*>>): Boolean =
        if (snapshot is Snap) {
            snapshot.reconstructObject(this)
            true
        } else {
            val editor = context.withoutUndo { snapshot.reconstructEditor(expansionContext()) }
            if (editorClass.isInstance(editor) && accepts(editor as E)) {
                expand(editor)
                true
            } else false
        }

    override fun createSnapshot(): Snapshot<*> = Snap()

    override fun supportsCopyPaste(): Boolean = true

    override fun viewAdded(view: ExpanderView) {
        when (val st = state.now) {
            is Text     -> view.displayText(st.text)
            is Expanded -> view.expanded(st.content)
        }
    }

    private sealed class State<out E> {
        class Text(val text: String, val completion: Any?) : State<Nothing>()

        class Expanded<out E>(val content: E) : State<E>()
    }

    override fun getSubEditor(accessor: EditorAccessor): Editor<*> {
        if (accessor !is ExpanderContent) throw InvalidAccessorException(accessor)
        return editor.now ?: throw InvalidAccessorException(accessor)
    }

    @OptIn(InternalSerializationApi::class)
    class Snap : Snapshot<Expander<*, *>>() {
        private lateinit var state: State<Snapshot<Editor<*>>>

        override fun doRecord(original: Expander<*, *>) {
            state = when (val st = original.state.now) {
                is Expanded -> Expanded(st.content.snapshot(recordClass = true))
                is Text     -> st
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun reconstructObject(original: Expander<*, *>) {
            original as Expander<*, Editor<*>>
            when (val st = state) {
                is Expanded -> {
                    val editor = original.context.withoutUndo {
                        st.content.reconstructEditor(original.expansionContext())
                    }
                    original.expand(editor)
                }
                is Text     -> original.setText(st.text)
            }
        }

        override fun encode(builder: JsonObjectBuilder) {
            when (val st = state) {
                is Expanded -> builder.put("editor", st.content.encodeToJson())
                is Text     -> {
                    if (st.completion != null) {
                        val cls = st.completion.javaClass
                        builder.put("completionClass", cls.name)
                        builder.put("completion", Json.encodeToJsonElement(cls.kotlin.serializer(), st.completion))
                    }
                    builder.put("text", st.text)
                }
            }
        }

        override fun decode(element: JsonObject) {
            state = when {
                "editor" in element -> {
                    val editor = decodeFromJson<Editor<*>>(element.getValue("editor"))
                    Expanded(editor)
                }
                "text" in element   -> {
                    val text = element.getValue("text").string
                    val cls = element["completionClass"]?.string?.loadClass()
                    val completion = if (cls != null) {
                        Json.decodeFromJsonElement(cls.kotlin.serializer(), element.getValue("completion"))
                    } else null
                    Text(text, completion)
                }
                else                -> initial
            }
        }
    }

    private class StateTransition<E : Editor<*>>(
        val ref: VirtualEditor<Expander<*, E>>,
        val before: State<E>, val after: State<E>,
        override val actionDescription: String
    ) : AbstractEdit() {
        override fun doUndo() {
            ref.get().reconstructState(before)
        }

        override fun doRedo() {
            ref.get().reconstructState(after)
        }
    }

    companion object {
        private val initial = Text("", null)
    }
}