/**
 *@author Nikolaus Knop
 */

package hextant.core.editor

import hextant.Context
import hextant.base.AbstractEditor
import hextant.core.editable.EditableText
import hextant.core.view.TextEditorView
import hextant.runLater
import reaktive.value.now

class TextEditor(
    editableText: EditableText,
    private val context: Context
) : AbstractEditor<EditableText, TextEditorView>(editableText, context) {
    override fun viewAdded(view: TextEditorView) {
        view.onGuiThread { view.displayText(editable.text.now) }
    }

    fun setText(new: String) {
        context.runLater {
            editable.text.set(new)
            views { displayText(new) }
        }
    }
}