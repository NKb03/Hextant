/**
 * @author Nikolaus Knop
 */

@file:Suppress("UNCHECKED_CAST")

package hextant.fx

import bundles.Bundle
import bundles.createBundle
import hextant.command.Commands
import hextant.command.line.CommandLine
import hextant.context.Context
import hextant.context.EditorControlGroup
import hextant.context.createControl
import hextant.core.Editor
import hextant.core.view.CompoundEditorControl.Layout
import hextant.core.view.EditorControl
import hextant.serial.makeRoot
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.stage.PopupWindow
import javafx.stage.Stage
import reaktive.value.now
import kotlin.concurrent.thread

internal fun control(skin: Skin<out Control>): Control {
    return object : Control() {
        init {
            setSkin(skin)
        }
    }
}

fun Control.setRoot(node: Node) {
    skin = null
    skin = skin(this, node)
}

internal fun skin(control: Control, node: Node): Skin<Control> = SimpleSkin(control, node)


private class SimpleSkin(
    private val control: Control, private val node: Node
) : Skin<Control> {
    override fun getSkinnable(): Control = control

    override fun getNode(): Node = node

    override fun dispose() {}
}

internal inline fun Node.registerShortcut(s: KeyCombination, crossinline action: () -> Unit) {
    addEventHandler(KeyEvent.KEY_RELEASED) { k ->
        if (s.match(k)) {
            action()
            k.consume()
        }
    }
}

internal fun PopupWindow.show(node: Node) {
    val p = node.localToScreen(0.0, node.prefHeight(-1.0)) ?: return
    show(node, p.x, p.y)
}

internal fun TextField.smartSetText(new: String) {
    val previous = text
    if (previous != new) {
        text = new
        if (new.startsWith(previous)) {
            positionCaret(new.length)
        }
    }
}

internal fun Node.onAction(action: () -> Unit) {
    addEventHandler(KeyEvent.KEY_RELEASED) { ev ->
        if (ev.code == ENTER) {
            action()
            ev.consume()
        }
    }
    addEventHandler(MouseEvent.MOUSE_PRESSED) { ev ->
        if (ev.clickCount >= 2) {
            action()
            ev.consume()
        }
    }
}

/**
 * Return a [Label] with the given text and with the 'hextant-text' and the 'keyword' style class.
 */
fun keyword(name: String) = Label(name).apply {
    styleClass.add("hextant-text")
    styleClass.add("keyword")
}

/**
 * Return a [Label] with the given text and with the 'hextant-text' and the 'operator' style class.
 */
fun operator(name: String) = Label(name).apply {
    styleClass.add("hextant-text")
    styleClass.add("operator")
}

internal fun Region.fixWidth(value: Double) {
    prefWidth = value
    minWidth = value
    maxWidth = value
}

internal fun <N : Node> N.withStyleClass(vararg names: String) = apply { styleClass.addAll(*names) }

internal fun <N : Node> N.withStyle(style: String) = also { it.style = style }

internal fun <C : Control> C.withTooltip(tooltip: Tooltip) = apply { this.tooltip = tooltip }

internal fun <C : Control> C.withTooltip(text: String) = withTooltip(Tooltip(text))

internal fun hextantLabel(text: String, graphic: Node? = null) = Label(text, graphic).withStyleClass("hextant-text")

/**
 * Add the editor control for the given [editor] to this compound view.
 * The [config] block is used to initialize properties of the [hextant.core.EditorView.arguments] bundle.
 */
fun Layout.view(
    editor: Editor<*>,
    bundle: Bundle = createBundle(),
    cached: Boolean = true,
    config: Bundle.() -> Unit
) = view(editor, bundle.apply(config), cached)

fun Dialog<*>.setDefaultButton(type: ButtonType) {
    for (tp in dialogPane.buttonTypes) {
        val button = dialogPane.lookupButton(tp) as Button
        button.isDefaultButton = tp == type
    }
}

inline fun <R, D : Dialog<R>> D.showDialog(config: D.() -> Unit = {}): R? {
    config()
    isResizable = true
    setOnShown {
        runFXWithTimeout(delay = 100) {
            isResizable = false
        }
    }
    return showAndWait().orElse(null)
}

fun <R> showDialog(config: Dialog<R>.() -> Unit): R? = Dialog<R>().showDialog(config)

inline fun showConfirmationAlert(yesButton: ButtonType = ButtonType.YES, config: Alert.() -> Unit): Boolean =
    Alert(Alert.AlertType.CONFIRMATION).showDialog(config) == yesButton

/**
 * Gets input from the user by showing the given [editor] in a [Dialog] to him.
 *
 * If the user cancels the dialog `null` is returned.
 * @param control the [EditorControl] that shows the editor to the user.
 * @param buttonTypes the possible button types.
 */
fun <R> getUserInput(
    title: String,
    editor: Editor<R>,
    control: Node = editor.context.createControl(editor),
    buttonTypes: List<ButtonType> = listOf(ButtonType.OK, ButtonType.CANCEL),
    applyStyle: Boolean = true
): R? {
    editor.makeRoot()
    return showDialog<R> {
        dialogPane.content = control
        dialogPane.buttonTypes.setAll(buttonTypes)
        dialogPane.scene.initHextantScene(editor.context, applyStyle)
        val ok = dialogPane.lookupButton(ButtonType.OK) as Button
        ok.isDefaultButton = false
        dialogPane.registerShortcuts {
            on("Ctrl+Enter") { ok.fire() }
        }
        setResultConverter { btn ->
            when (btn) {
                ButtonType.OK -> editor.result.now
                ButtonType.CANCEL -> null
                else -> error("Unexpected button type: $btn")
            }
        }
        setOnShown {
            runFXWithTimeout {
                editor.context[EditorControlGroup].getViewOf(editor).receiveFocus()
            }
        }
        this.title = title
    }
}

fun KeyEventHandlerBody<*>.handleCommands(target: Any, context: Context, commandLine: CommandLine) {
    for (command in context[Commands].applicableOn(target)) {
        val shortcut = command.shortcut
        if (shortcut != null) {
            on(shortcut, consume = false) { ev ->
                commandLine.expand(command)
                if (command.parameters.isEmpty()) {
                    val result = commandLine.execute(byShortcut = true)
                    if (result != false) ev.consume()
                } else {
                    context[EditorControlGroup].getViewOf(commandLine).receiveFocus()
                    ev.consume()
                }
            }
        }
    }
}

/**
 * Shows a new [Stage] with the given node as the root.
 */
fun showStage(root: Parent, context: Context, applyStyle: Boolean): Stage = Stage().apply {
    scene = Scene(root)
    scene.initHextantScene(context, applyStyle)
    show()
}

/**
 * Shows a [Stage] with the view of the given [editor] as the root.
 */
fun showStage(editor: Editor<*>, applyStyle: Boolean) =
    showStage(editor.context.createControl(editor), editor.context, applyStyle)

/**
 * Enqueues the given [action] into the JavaFX application thread after some [delay] which is given in milliseconds.
 */
fun runFXWithTimeout(delay: Long = 10, action: () -> Unit) {
    thread {
        Thread.sleep(delay)
        Platform.runLater(action)
    }
}

/**
 * Runs [EditorControl.receiveFocus] on the JavaFX application thread after the given [delay] which is measured in milliseconds.
 * @see runFXWithTimeout
 */
fun EditorControl<*>.receiveFocusLater(delay: Long = 10) {
    runFXWithTimeout(delay) { receiveFocus() }
}
