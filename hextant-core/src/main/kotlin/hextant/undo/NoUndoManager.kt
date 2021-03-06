package hextant.undo

/**
 * An [UndoManager] that never records edits
 */
object NoUndoManager : UndoManager {
    override val canUndo: Boolean
        get() = false
    override val canRedo: Boolean
        get() = false

    override var isActive: Boolean = true

    override fun undo() {}

    override fun redo() {}

    override fun record(edit: Edit) {}

    override val undoText: String
        get() = "No undo"
    override val redoText: String
        get() = "No redo"

    override fun beginCompoundEdit() {}

    override fun finishCompoundEdit(description: String) {}
}