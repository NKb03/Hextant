/**
 *@author Nikolaus Knop
 */

package hextant.core.editor

import hextant.Context
import hextant.core.CorePermissions.Public
import hextant.core.base.AbstractEditor
import hextant.core.editable.EditableToken
import hextant.core.expr.view.TextEditorView
import hextant.core.undo.*
import hextant.runLater
import org.nikok.reaktive.value.now

/**
 * An editor for tokens
 */
abstract class TokenEditor<E : EditableToken<*>, V : TextEditorView>(
    editable: E,
    private val context: Context
) : AbstractEditor<E, V>(editable, context) {
    private val undo = context[Public, UndoManagerFactory].get(AContext)

    override fun viewAdded(view: V) {
        view.onGuiThread { view.displayText(editable.text.now) }
    }

    /**
     * Set the text on the platform thread and notify the views
     */
    fun setText(new: String) {
        if (new != editable.text.now) {
            val edit = doSetText(new)
            undo.push(edit)
        }
    }

    private fun doSetText(new: String): TextEdit {
        val edit = TextEdit(this, editable.text.get(), new)
        context.runLater {
            editable.text.set(new)
            views { displayText(new) }
        }
        return edit
    }

    private class TextEdit(private val editor: TokenEditor<*, *>, private val old: String, private val new: String) :
        AbstractEdit() {
        override fun doRedo() {
            editor.doSetText(new)
        }

        override fun doUndo() {
            editor.doSetText(old)
        }

        override val actionDescription: String
            get() = "Editing"

        override fun mergeWith(other: Edit): Edit? =
            if (other !is TextEdit || other.editor !== this.editor) null
            else TextEdit(editor, this.old, other.new)
    }
}