/**
 *@author Nikolaus Knop
 */

package hextant.undo

import hextant.bundle.CorePermissions.Public
import hextant.bundle.Property

/**
 * The UndoManager is responsible for managing stacks of edits and undo and redo them
 */
interface UndoManager {
    /**
     * @return `true` only if this [UndoManager] can currently undo an [Edit]
     */
    val canUndo: Boolean

    /**
     * @return `true` only if this [UndoManager] can currently redo an [Edit]
     */
    val canRedo: Boolean

    /**
     * Undo the last done or redone [Edit]
     */
    fun undo()

    /**
     * Redo the last undone [Edit]
     */
    fun redo()

    /**
     * Push the specified [edit] such that [undo] would undo the [Edit]
     * * All redoable edits are discarded
     * @throws IllegalStateException if the [edit] is not undoable
     */
    fun push(edit: Edit)

    /**
     * The human-readable description of the currently undoable [Edit]
     */
    val undoText: String

    /**
     * The human-readable description of the currently undoable [Edit]
     */
    val redoText: String

    companion object : Property<UndoManager, Public, Public>("undo manager") {
        fun newInstance(): UndoManager = UndoManagerImpl()

        fun concurrent(undoManager: UndoManager = newInstance()): UndoManager = ConcurrentUndoManager(undoManager)
    }
}