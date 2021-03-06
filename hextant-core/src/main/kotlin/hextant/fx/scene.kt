/**
 * @author Nikolaus Knop
 */

package hextant.fx

import hextant.context.Context
import hextant.core.view.EditorControl
import javafx.scene.Scene
import javafx.scene.input.ContextMenuEvent

/**
 * Initializes this scene with the given [context] by registering top level shortcuts and applying registered stylesheets.
 */
fun Scene.initHextantScene(context: Context, applyStyle: Boolean = true) {
    registerNavigationShortcuts()
    registerCopyPasteShortcuts(context)
    addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED) { ev ->
        focusedEditorControl?.let {
            it.commandsPopup.show(root)
            ev.consume()
        }
    }
    if (applyStyle) context[Stylesheets].manage(this)
}

internal val Scene.focusedEditorControl: EditorControl<*>?
    get() = editorControlInParentChain(focusOwner)
