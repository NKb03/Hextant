package hextant.core.view

import hextant.core.EditorView

/**
 * An [EditorView] that displays a [hextant.core.editor.ChoiceEditor]
 */
interface ChoiceEditorView<C> : EditorView {
    /**
     * Is called when the given [choice] was selected in the associated editor
     */
    fun selected(choice: C)
}
