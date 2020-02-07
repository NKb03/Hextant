/**
 * @author Nikolaus Knop
 */

package hextant.fx

import com.sun.javafx.scene.traversal.*
import com.sun.javafx.scene.traversal.Direction.*
import com.sun.javafx.scene.traversal.Direction.DOWN
import com.sun.javafx.scene.traversal.Direction.LEFT
import com.sun.javafx.scene.traversal.Direction.RIGHT
import com.sun.javafx.scene.traversal.Direction.UP
import hextant.*
import hextant.base.EditorControl
import hextant.bundle.CoreProperties
import hextant.core.view.FXExpanderView
import hextant.impl.SelectionDistributor
import hextant.impl.Stylesheets
import javafx.scene.*
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent

internal var isShiftDown = false; private set

fun hextantScene(
    root: (Context) -> Parent,
    createContext: (HextantPlatform) -> Context
): Scene {
    val platform = HextantPlatform.configured()
    val context = createContext(platform)
    val scene = Scene(root(context))
    scene.initHextantScene(context)
    return scene
}

fun Scene.initHextantScene(context: Context) {
    initEventHandlers(context)
    context[Stylesheets].apply(this)
}

private fun Scene.initEventHandlers(ctx: Context) {
    listenForShift()
    changeTraversalEngine()
    traverseOnArrowWithCtrl()
    registerCopyPaste(ctx)
}

private fun Scene.registerCopyPaste(context: Context) {
    registerShortcuts {
        on("Ctrl + C") {
            val view = getFocusedEditorControl()
            val editor = view?.target
            if (editor is Editor<*>) editor.copyToClipboard()
        }
        on("Ctrl + Shift + C") {
            val selected = context[SelectionDistributor].selectedTargets.now
            if (selected.any { it !is Editor<*> }) return@on
            context[CoreProperties.clipboard] = selected.toList()
        }
        on("Ctrl + V") {
            val view = getFocusedEditorControl()
            val editor = view?.target
            if (editor is Editor<*>) {
                val success = editor.pasteFromClipboard()
                if (!success) editor.expander?.pasteFromClipboard()
            }
        }
    }
}

private fun Scene.listenForShift() {
    addEventFilter(KeyEvent.KEY_PRESSED) {
        if (it.code == SHIFT) {
            isShiftDown = true
        }
    }
    addEventFilter(KeyEvent.KEY_RELEASED) { ev ->
        if (ev.code == SHIFT) {
            isShiftDown = false
        }
    }
}

internal fun Scene.traverseOnArrowWithCtrl() {
    addEventHandler(KeyEvent.KEY_RELEASED) { ev ->
        if (ev.code == KeyCode.LEFT) {
            if (focusPrevious()) ev.consume()
        } else if (ev.code == KeyCode.RIGHT) {
            if (focusNext()) ev.consume()
        }
    }
}

internal fun Scene.focusNext(): Boolean {
    val next = getFocusedEditorControl()?.next ?: return false
    val firstChild = generateSequence(next) {
        if (it is FXExpanderView) it.root as? EditorControl<*>
        else it.editorChildren().firstOrNull()
    }.last()
    firstChild.focus()
    return true
}

internal fun Scene.focusPrevious(): Boolean {
    val prev = getFocusedEditorControl()?.previous ?: return false
    val lastChild = generateSequence(prev) {
        if (it is FXExpanderView) it.root as? EditorControl<*>
        else it.editorChildren().lastOrNull()
    }.last()
    lastChild.focus()
    return true
}

private fun Scene.getFocusedEditorControl(): EditorControl<*>? {
    val editorControl = generateSequence(focusOwner) { it.parent }.firstOrNull { it is EditorControl<*> }
    return editorControl as EditorControl<*>?
}

@Suppress("DEPRECATION")
private fun Scene.changeTraversalEngine() {
    root.impl_traversalEngine = ParentTraversalEngine(root, object : Algorithm {
        override fun select(owner: Node, dir: Direction, context: TraversalContext?): Node? {
            val editorControl = generateSequence(owner) { it.parent }.first { it is EditorControl<*> }
            editorControl as EditorControl<*>
            return when (dir) {
                UP, DOWN, NEXT_IN_LINE -> owner
                LEFT, PREVIOUS         -> editorControl.previous
                RIGHT, NEXT            -> editorControl.next
            }
        }

        override fun selectFirst(context: TraversalContext?): Node? =
            firstEditorControl(root)

        private fun firstEditorControl(node: Node): EditorControl<*>? = when (node) {
            is Parent -> {
                for (c in node.childrenUnmodifiable) {
                    firstEditorControl(c)?.let { return it }
                }
                if (node is EditorControl<*>) node else null
            }
            else      -> node as? EditorControl<*>
        }

        override fun selectLast(context: TraversalContext?): Node? =
            lastEditorControl(root)

        private fun lastEditorControl(node: Node): EditorControl<*>? = when (node) {
            is EditorControl<*> -> node
            is Parent           -> {
                for (c in node.childrenUnmodifiable.asReversed()) {
                    firstEditorControl(c)?.let { return it }
                }
                if (node is EditorControl<*>) node else null
            }
            else                -> node as? EditorControl<*>
        }
    })
}


fun lastShortcutLabel(scene: Scene): Label {
    val shortcutDisplay = Label().apply {
        style = "-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 20;"
    }
    scene.addEventFilter(KeyEvent.KEY_RELEASED) { e ->
        if (e.isShortcut() || e.code == ENTER || e.code == TAB) {
            shortcutDisplay.text = e.getShortcutString()
        }
    }
    return shortcutDisplay
}

private fun KeyEvent.getShortcutString(): String = buildString {
    if (isControlDown) append("Ctrl + ")
    if (isAltDown) append("Alt + ")
    if (isShiftDown) append("Shift + ")
    if (isMetaDown) append("Meta + ")
    append(code)
}

private fun KeyEvent.isShortcut() = isAltDown || isControlDown || isShortcutDown || isMetaDown