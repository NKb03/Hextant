/**
 *@author Nikolaus Knop
 */

package hackus.editor

import hackus.editable.EditableCompound
import hextant.*
import hextant.base.ParentEditor

class CompoundEditor(editable: EditableCompound, context: Context) :
    ParentEditor<EditableCompound, EditorView>(editable, context) {
    init {
        addChildren(editable.subNodes)
    }

    override fun accepts(child: Editor<*>): Boolean = child is SubNodeListEditor
}