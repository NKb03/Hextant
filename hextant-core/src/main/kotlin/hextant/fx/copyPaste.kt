package hextant.fx

import hextant.context.Clipboard
import hextant.context.ClipboardContent.MultipleEditors
import hextant.context.Context
import hextant.context.SelectionDistributor
import hextant.context.executeSafely
import hextant.core.Editor
import hextant.serial.snapshot
import javafx.scene.Scene

private val COPY_MANY = "Ctrl + Shift + C".shortcut

private fun copyManyToClipboard(context: Context) {
    val selected = context[SelectionDistributor].selectedTargets.now
    if (selected.any { it !is Editor<*> }) return
    val snapshots = selected.map {
        context.executeSafely("copying", null) {
            (it as Editor<*>).snapshot(recordClass = true)
        } ?: return
    }
    context[Clipboard].copy(MultipleEditors(snapshots))
}

internal fun Scene.registerCopyPasteShortcuts(context: Context) {
    registerShortcuts {
        on(COPY_MANY) {
            copyManyToClipboard(context)
        }
    }
}

