/**
 *@author Nikolaus Knop
 */

package hextant.undo


import bundles.PublicProperty
import bundles.property

/**
 * The UndoManager is responsible for managing stacks of edits and undo and redo them
 */
interface UndoManager {
    /**
     * Indicates whether this [UndoManager] can currently undo an [Edit].
     */
    val canUndo: Boolean

    /**
     * Indicates whether this [UndoManager] can currently redo an [Edit].
     */
    val canRedo: Boolean

    /**
     * Indicates whether this [UndoManager] currently records undoable edits.
     */
    var isActive: Boolean

    /**
     * Undo the last done or redone [Edit].
     */
    fun undo()

    /**
     * Redo the last undone [Edit].
     */
    fun redo()

    /**
     * Record the specified [edit] such that [undo] would undo the [Edit].
     * * All redoable edits are discarded
     * * If the [UndoManager] is not active then this method has no effect.
     * @throws IllegalStateException if the [edit] is not undoable
     */
    fun record(edit: Edit)

    /**
     * Begins a compound edit.
     * Edits pushed between calls of [beginCompoundEdit] and [finishCompoundEdit] are combined in a single compound edit.
     * @throws IllegalStateException if [finishCompoundEdit] has already been called after the last call to [finishCompoundEdit].
     */
    fun beginCompoundEdit()

    /**
     * Finishes a compound edit began with [beginCompoundEdit].
     *
     * If no edits have been pushed since or if the [UndoManager] is not active then this method has no effect.
     * @param description the [Edit.actionDescription] of the compound edit
     * @throws IllegalStateException if [beginCompoundEdit] was not called previously.
     */
    fun finishCompoundEdit(description: String)

    /**
     * The human-readable description of the currently undoable [Edit]
     */
    val undoText: String

    /**
     * The human-readable description of the currently undoable [Edit]
     */
    val redoText: String

    companion object : PublicProperty<UndoManager> by property("undo manager") {
        /**
         * Return a new undo manager.
         */
        fun newInstance(): UndoManager = UndoManagerImpl()

        /**
         * Return a synchronized undo manager which delegates to the given [undoManager]
         */
        fun concurrent(undoManager: UndoManager = newInstance()): UndoManager = ConcurrentUndoManager(undoManager)
    }
}