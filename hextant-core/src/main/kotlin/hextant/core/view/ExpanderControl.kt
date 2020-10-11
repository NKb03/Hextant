/**
 *@author Nikolaus Knop
 */

package hextant.core.view

import bundles.Bundle
import bundles.SimpleProperty
import hextant.codegen.ProvideImplementation
import hextant.completion.Completer
import hextant.completion.NoCompleter
import hextant.completion.gui.CompletionPopup
import hextant.context.ControlFactory
import hextant.context.createControl
import hextant.core.Editor
import hextant.core.InputMethod
import hextant.core.editor.Expander
import hextant.fx.*
import javafx.scene.Node
import reaktive.Observer

/**
 * JavaFX implementation of a [ExpanderView]
 */
open class ExpanderControl @ProvideImplementation(ControlFactory::class) constructor(
    private val expander: Expander<*, *>,
    args: Bundle
) : ExpanderView, EditorControl<Node>(expander, args) {
    constructor(expander: Expander<*, *>, args: Bundle, completer: Completer<Expander<*, *>, Any>) :
            this(expander, args.also { it[COMPLETER] = completer })

    private var view: EditorControl<*>? = null

    private val textField = HextantTextField(initialInputMethod = context[InputMethod]).withStyleClass("expander-text")

    private val textObserver: Observer

    private val popup = CompletionPopup(context, expander) { arguments[COMPLETER] }

    private val completionObserver: Observer

    override fun createDefaultRoot(): Node = textField

    override fun setEditorParent(parent: EditorControl<*>?) {
        super.setEditorParent(parent)
        view?.setEditorParent(parent)
    }

    override fun setNext(nxt: EditorControl<*>?) {
        super.setNext(nxt)
        view?.setNext(nxt)
    }

    override fun setPrevious(prev: EditorControl<*>?) {
        super.setPrevious(prev)
        view?.setPrevious(prev)
    }

    init {
        with(textField) {
            registerShortcuts {
                on("Ctrl + Space") { popup.show(root) }
                on("Enter") { expander.expand() }
            }
            textObserver = userUpdatedText.observe { _, new ->
                expander.setText(new)
                popup.updateInput(new)
                popup.show(root)
            }
        }
        styleClass.add("expander")
        expander.addView(this)
        completionObserver = popup.completionChosen.observe { _, completion ->
            if (!expander.isExpanded) expander.complete(completion)
        }
    }

    override fun displayText(text: String) {
        if (text != textField.text) {
            textField.text = text
            popup.show(root)
        }
    }

    override fun receiveFocus() {
        if (view != null) view!!.receiveFocus()
        else textField.requestFocus()
    }

    override fun reset() {
        removeChild(0)
        view = null
        root = textField
        textField.text = ""
        requestFocus()
    }

    final override fun expanded(editor: Editor<*>) {
        if (root is EditorControl<*>) removeChild(0)
        val v = context.createControl(editor)
        addChild(v, 0)
        v.registerShortcuts {
            on("Ctrl? + R") { expander.reset() }
        }
        v.setNext(next)
        v.setPrevious(previous)
        v.setEditorParent(editorParent)
        v.root //initialize root
        view = v
        root = v
        v.receiveFocus()
        onExpansion(editor, v)
    }

    /**
     * Called when the [Expander] has been expanded to the given [editor].
     * The default implementation does nothing.
     */
    protected open fun onExpansion(editor: Editor<*>, control: EditorControl<*>) {}

    companion object {
        /**
         * This property controls the completer of the expander control
         */
        val COMPLETER = SimpleProperty.withDefault<Completer<Expander<*, *>, Any>>("expander.completer", NoCompleter)
    }
}