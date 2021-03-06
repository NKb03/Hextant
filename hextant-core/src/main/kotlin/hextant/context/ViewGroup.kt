/**
 * @author Nikolaus Knop
 */

package hextant.context

import bundles.Bundle
import hextant.core.Editor
import hextant.core.EditorView

/**
 * A group of [EditorView]'s of a common base type [V] that display a hierarchy of editors
 */
interface ViewGroup<V : EditorView> {
    /**
     * **Queries** the editor view that displays the given [editor] in this [ViewGroup]
     * @throws NoSuchElementException if [editor] is not displayed by this [ViewGroup]
     */
    fun getViewOf(editor: Editor<*>): V

    /**
     * **Creates** an editor view for the given editor
     */
    fun createViewFor(editor: Editor<*>, context: Context, arguments: Bundle): V

    /**
     * Returns `true` only if this [ViewGroup] has cached a view for the given [editor]
     */
    fun hasViewFor(editor: Editor<*>): Boolean
}