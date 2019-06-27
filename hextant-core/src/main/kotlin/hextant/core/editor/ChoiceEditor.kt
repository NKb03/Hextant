/**
 *@author Nikolaus Knop
 */

package hextant.core.editor

import hextant.*
import hextant.base.AbstractEditor
import hextant.core.view.ChoiceEditorView
import reaktive.value.reactiveVariable

/**
 * An [Editor] which supports choosing different items of type [C]
 */
abstract class ChoiceEditor<C : Any>(default: C, context: Context) : AbstractEditor<C, ChoiceEditorView<C>>(context) {
    private val selected = reactiveVariable(ok(default))

    override val result: EditorResult<C>
        get() = selected

    /**
     * Select the given [choice]
     */
    fun select(choice: C) {
        selected.set(ok(choice))
        views { selected(choice) }
    }
}