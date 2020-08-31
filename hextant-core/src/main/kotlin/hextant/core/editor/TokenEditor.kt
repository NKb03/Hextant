/**
 *@author Nikolaus Knop
 */

package hextant.core.editor

import hextant.completion.Completion
import hextant.context.Context
import hextant.context.executeSafely
import hextant.core.view.TokenEditorView
import hextant.serial.VirtualEditor
import hextant.serial.virtualize
import hextant.undo.*
import reaktive.value.*
import validated.*
import validated.reaktive.ReactiveValidated

/**
 * A token editor transforms text to tokens.
 * When setting the text it is automatically compiled to a token.
 */
abstract class TokenEditor<out R, in V : TokenEditorView>(context: Context) : AbstractEditor<R, V>(context),
                                                                              TokenType<R> {
    constructor(context: Context, text: String) : this(context) {
        setText(text, undoable = false)
    }

    private val _result = reactiveVariable(this.tryCompile(""))

    override val result: ReactiveValidated<R> get() = _result

    private var _text = reactiveVariable("")

    /**
     * A [ReactiveValue] holding the current textual content of this editor
     */
    val text: ReactiveString get() = _text

    private val undo = context[UndoManager]

    /**
     * Make a result from the given completion item.
     */
    protected open fun compile(item: Any): Validated<R> = invalidComponent()

    override fun compile(token: String): Validated<R> = invalidComponent()

    private fun tryCompile(item: Any): Validated<R> =
        context.executeSafely("compiling item", invalidComponent) { compile(item) }

    private fun tryCompile(text: String): Validated<R> =
        context.executeSafely("compiling item", invalidComponent) { compile(text) }

    override fun viewAdded(view: V) {
        view.displayText(text.now)
    }

    override fun createSnapshot(): EditorSnapshot<*> = Snapshot(this)

    /**
     * Set the text of this editor, such that the result is automatically updated
     */
    fun setText(newText: String, undoable: Boolean = true) {
        if (undoable) {
            val edit = TextEdit(virtualize(), text.now, newText)
            undo.push(edit)
        }
        _text.now = newText
        _result.set(tryCompile(text.now))
        views { displayText(newText) }
    }

    /**
     * Set the text of this editor to the completion text of the given [completion] and then compile the completed item.
     */
    fun complete(completion: Completion<*>) {
        val t = completion.completionText
        val edit = TextEdit(virtualize(), text.now, t)
        undo.push(edit)
        _text.now = t
        _result.set(tryCompile(completion.completion).orElse { tryCompile(completion.completionText) })
        views { displayText(t) }
    }

    private class TextEdit(
        private val editor: VirtualEditor<TokenEditor<*, *>>,
        private val old: String,
        private val new: String
    ) : AbstractEdit() {
        override fun doRedo() {
            editor.get().setText(new, undoable = false)
        }

        override fun doUndo() {
            editor.get().setText(old, undoable = false)
        }

        override val actionDescription: String
            get() = "Editing"

        override fun mergeWith(other: Edit): Edit? =
            if (other !is TextEdit || other.editor !== this.editor) null
            else TextEdit(editor, this.old, other.new)
    }

    private class Snapshot(original: TokenEditor<*, *>) : EditorSnapshot<TokenEditor<*, *>>(original) {
        private val text = original.text.now

        override fun reconstruct(editor: TokenEditor<*, *>) {
            editor.setText(text, undoable = false)
        }
    }
}