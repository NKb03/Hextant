/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core.base

import javafx.application.Platform
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.input.*
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.W
import org.nikok.hextant.*
import org.nikok.hextant.core.command.gui.commandContextMenu
import org.nikok.hextant.core.fx.*
import org.nikok.hextant.core.inspect.Inspections
import org.nikok.hextant.core.inspect.gui.InspectionPopup

/**
 * An [EditorView] represented as a [javafx.scene.control.Control]
 * @param R the type of the root-[Node] of this control
 */
abstract class EditorControl<R : Node> : Control(), EditorView {
    private var _root: R? = null

    /**
     * Creates the default root for this control
     */
    protected abstract fun createDefaultRoot(): R

    /**
     * The current root of this control
     * * Initially it has the value of [createDefaultRoot]
     * * Setting it updates the look of this control
     */
    var root: R
        get() = _root ?: throw IllegalStateException("root not yet initialized")
        protected set(newRoot) {
            _root = newRoot
            setRoot(newRoot)
        }

    override fun requestFocus() {
        root.requestFocus()
    }

    /**
     * Initialize this [EditorControl]
     * * Must be called exactly once after constructor logic, otherwise behaviour is undefined
     * @param editable the editable represented by this view
     * @param editor the editor represented by this view
     * @param platform the [HextantPlatform]
     */
    protected fun initialize(editable: Editable<*>, editor: Editor<*>, platform: HextantPlatform) {
        check(!initialized) { "already initialized" }
        root = createDefaultRoot()
        activateContextMenu(editable, platform)
        activateInspections(editable, platform)
        activateSelectionExtension(editor)
        initialized = true
    }

    private fun activateSelectionExtension(editor: Editor<*>) {
        addEventHandler(KeyEvent.KEY_RELEASED) { k ->
            if (EXTEND_SELECTION.match(k) && !editor.isSelected) {
                editor.select()
                k.consume()
            }
        }
    }


    private fun activateInspections(inspected: Any, platform: HextantPlatform) {
        val inspections = platform[Inspections]
        val p = InspectionPopup(this) { inspections.getProblems(inspected) }
        registerShortcut(KeyCodeCombination(ENTER, KeyCombination.ALT_DOWN)) { p.show(this) }
    }

    private fun <T : Any> activateContextMenu(target: T, platform: HextantPlatform) {
        val contextMenu = target.commandContextMenu(platform)
        setOnContextMenuRequested { contextMenu.show(this, Side.BOTTOM, 0.0, 0.0) }
    }

    private var initialized = false

    override fun select(isSelected: Boolean) {
        pseudoClassStateChanged(PseudoClasses.SELECTED, isSelected)
    }

    override fun error(isError: Boolean) {
        pseudoClassStateChanged(PseudoClasses.ERROR, isError)
    }

    /**
     * [requestFocus]
     */
    override fun focus() {
        requestFocus()
    }

    /**
     * Run the specified [action] on the JavaFX Application Thread
     */
    override fun onGuiThread(action: () -> Unit) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(action)
        } else action()
    }

    companion object {
        private val EXTEND_SELECTION = KeyCodeCombination(W, KeyCombination.SHORTCUT_DOWN)
    }
}