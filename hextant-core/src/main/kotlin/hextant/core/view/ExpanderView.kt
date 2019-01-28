/**
 *@author Nikolaus Knop
 */

package hextant.core.view

import hextant.Editable
import hextant.EditorView

interface ExpanderView: EditorView {
    fun textChanged(newText: String)

    fun reset()

    fun expanded(newContent: Editable<*>)
}