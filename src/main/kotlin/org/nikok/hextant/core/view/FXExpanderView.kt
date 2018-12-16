/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core.view

import javafx.scene.Node
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.R
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination.SHORTCUT_ANY
import org.nikok.hextant.*
import org.nikok.hextant.core.EditorViewFactory
import org.nikok.hextant.core.ExpanderFactory
import org.nikok.hextant.core.base.EditorControl
import org.nikok.hextant.core.editable.Expandable
import org.nikok.hextant.core.fx.HextantTextField
import org.nikok.hextant.core.fx.registerShortcut
import org.nikok.reaktive.value.now

class FXExpanderView(
    private val expandable: Expandable<*, *>,
    platform: HextantPlatform
) : ExpanderView, EditorControl<Node>() {

    private val expander = platform[ExpanderFactory].getExpander(expandable)

    private val views = platform[EditorViewFactory]

    private var view: EditorControl<*>? = null

    private val textField = createExpanderTextField(expandable.text.now)

    override fun createDefaultRoot(): Node = textField

    init {
        expander.addView(this)
        initialize(expandable, expander, platform)
    }

    override fun select(isSelected: Boolean) {
        if (isSelected) {
            textField.style = "-fx-background-color: #292929;"
        } else {
            textField.style = null
        }
    }

    private fun createExpanderTextField(text: String?): TextField = HextantTextField(text).apply {
        setOnAction { expander.expand() }
        textProperty().addListener { _, _, new -> expander.setText(new) }
    }

    override fun textChanged(newText: String) {
        if (newText != textField.text) {
            textField.text = newText
        }
    }

    override fun reset() {
        view = null
        root = textField
        textField.requestFocus()
    }

    override fun expanded(newContent: Editable<*>) {
        val v = views.getFXView(newContent)
        view = v
        v.registerShortcut(RESET_SHORTCUT) { expander.reset() }
        root = v
        v.focus()
    }

    override fun requestFocus() {
        if (expandable.isExpanded.now) view!!.focus()
        else textField.requestFocus()
    }

    companion object {
        private val RESET_SHORTCUT = KeyCodeCombination(R, SHORTCUT_ANY)
    }
}