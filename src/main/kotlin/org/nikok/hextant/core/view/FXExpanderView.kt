/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core.view

import javafx.scene.control.Control
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.R
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination.SHORTCUT_ANY
import org.nikok.hextant.Editable
import org.nikok.hextant.HextantPlatform
import org.nikok.hextant.core.CorePermissions.Public
import org.nikok.hextant.core.EditorViewFactory
import org.nikok.hextant.core.editable.Expandable
import org.nikok.hextant.core.editor.Expander
import org.nikok.hextant.core.fx.*
import org.nikok.reaktive.value.now

class FXExpanderView<E : Editable<*>, Ex : Expandable<*, E>>(
    private val expandable: Ex,
    createExpander: (FXExpanderView<E, Ex>) -> Expander<E>
) : ExpanderView, FXEditorView, Control() {
    private val textField = createExpanderTextField(expandable.text.now)

    init {
        styleClass.add("expander")
        textField.textProperty().addListener { _, _, new -> expandable.setText(new) }
    }

    private val views = HextantPlatform[Public, EditorViewFactory]

    private var view: FXEditorView? = null

    private val expander: Expander<*> = createExpander(this)

    private fun createExpanderTextField(text: String?): TextField = HextantTextField(text).apply {
        setOnAction { expander.expand() }
    }

    override fun textChanged(newText: String) {
        textField.text = newText
    }

    override fun reset() {
        view = null
        setRoot(textField)
        textField.requestFocus()
    }

    override fun expanded(newContent: Editable<*>) {
        val v = views.getFXView(newContent)
        view = v
        v.node.registerShortcut(RESET_SHORTCUT) { expander.reset() }
        setRoot(v.node)
        v.focus()
    }

    override val node: Control
        get() = this

    override fun requestFocus() {
        if (expandable.isExpanded.now) view!!.focus()
        else textField.requestFocus()
    }

    companion object {
        private val RESET_SHORTCUT = KeyCodeCombination(R, SHORTCUT_ANY)
    }
}