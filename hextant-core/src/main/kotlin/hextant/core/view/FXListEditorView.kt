/**
 *@author Nikolaus Knop
 */

package hextant.core.view

import hextant.Editor
import hextant.base.EditorControl
import hextant.bundle.Bundle
import hextant.core.editor.ListEditor
import hextant.createView
import hextant.fx.ModifierValue.DOWN
import hextant.fx.registerShortcuts
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.INSERT
import reaktive.list.fx.asObservableList

class FXListEditorView(private val editor: ListEditor<*, *>, arguments: Bundle) :
    EditorControl<ListView<Editor<*>>>(editor, arguments) {
    override fun createDefaultRoot(): ListView<Editor<*>> = ListView(editor.editors.asObservableList()).apply {
        setCellFactory { Cell() }
        selectionModel.selectionMode = MULTIPLE
        registerShortcuts {
            on(DELETE) {
                val idx = selectionModel.selectedIndex
                if (idx != -1) {
                    editor.removeAt(idx)
                }
            }
            on(INSERT) {
                val idx = selectionModel.selectedIndex
                editor.addAt(idx + 1) //also works if idx == -1
            }
            on(INSERT, { shift(DOWN) }) {
                val idx = selectionModel.selectedIndex
                if (idx != -1) editor.addAt(idx)
            }
        }
    }

    private inner class Cell : ListCell<Editor<*>>() {
        override fun updateItem(item: Editor<*>?, empty: Boolean) {
            super.updateItem(item, empty)
            if (item == null || empty) graphic = null else {
                val v = context.createView(item)
                graphic = v
                v.receiveFocus()
            }
        }
    }
}