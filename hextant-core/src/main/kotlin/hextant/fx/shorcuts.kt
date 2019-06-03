/**
 * @author Nikolaus Knop
 */

package hextant.fx

import hextant.fx.ModifierValue.*
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

enum class ModifierValue { DOWN, UP, MAYBE }

class Shortcut(val key: KeyCode, val control: ModifierValue, val alt: ModifierValue, val shift: ModifierValue) {
    override fun toString(): String = buildString {
        append(key.name)
        for ((name, value) in listOf("Ctrl" to control, "Alt" to alt, "Shift" to shift)) {
            when (value) {
                DOWN  -> append(" + $name")
                UP    -> {
                }
                MAYBE -> append(" + ($name)")
            }
        }
    }
}

class ShortcutBuilder @PublishedApi internal constructor(private val key: KeyCode) {
    private var control = UP
    private var alt = UP
    private var shift = UP

    fun control(v: ModifierValue) {
        control = v
    }

    fun alt(v: ModifierValue) {
        alt = v
    }

    fun shift(v: ModifierValue) {
        shift = v
    }

    fun build() = Shortcut(key, control, alt, shift)
}

inline fun shortcut(key: KeyCode, block: ShortcutBuilder.() -> Unit) = ShortcutBuilder(key).apply(block).build()

private suspend fun SequenceScope<Matcher>.decidedAll(
    ctrl: Boolean,
    alt: Boolean,
    shift: Boolean,
    shortcut: Shortcut
) {
    yield(Matcher(shortcut.key, ctrl, alt, shift))
}

private suspend fun SequenceScope<Matcher>.decidedControl(ctrl: Boolean, shortcut: Shortcut) {
    when (shortcut.alt) {
        DOWN  -> decidedAlt(ctrl, true, shortcut)
        UP    -> decidedAlt(ctrl, false, shortcut)
        MAYBE -> {
            decidedAlt(ctrl, true, shortcut)
            decidedAlt(ctrl, false, shortcut)
        }
    }
}

private suspend fun SequenceScope<Matcher>.decidedAlt(ctrl: Boolean, alt: Boolean, shortcut: Shortcut) {
    when (shortcut.shift) {
        DOWN  -> decidedAll(ctrl, alt, true, shortcut)
        UP    -> decidedAll(ctrl, alt, false, shortcut)
        MAYBE -> {
            decidedAll(ctrl, alt, true, shortcut)
            decidedAll(ctrl, alt, false, shortcut)
        }
    }
}

private fun matchers(shortcut: Shortcut): Sequence<Matcher> = sequence {
    when (shortcut.control) {
        DOWN  -> decidedControl(true, shortcut)
        UP    -> decidedControl(false, shortcut)
        MAYBE -> {
            decidedControl(true, shortcut)
            decidedControl(false, shortcut)
        }
    }
}

data class Matcher(val key: KeyCode, val control: Boolean, val alt: Boolean, val shift: Boolean)

private fun KeyEvent.getMatcher(): Matcher =
    Matcher(code, isControlDown, isAltDown, isShiftDown)

class ShortcutRegistrar : EventHandler<KeyEvent> {
    private val handlers = mutableMapOf<Matcher, () -> Boolean>()

    override fun handle(event: KeyEvent) {
        val handler = handlers[event.getMatcher()] ?: return
        if (handler.invoke()) event.consume()
    }

    fun maybeOn(shortcut: Shortcut, handler: () -> Boolean) {
        matchers(shortcut).forEach { matcher ->
            handlers[matcher] = handler
        }
    }
}

inline fun ShortcutRegistrar.on(shortcut: Shortcut, crossinline handler: () -> Unit) {
    maybeOn(shortcut) { handler(); true }
}

inline fun Node.registerShortcuts(handlers: ShortcutRegistrar.() -> Unit) {
    val handler = ShortcutRegistrar()
    handler.handlers()
    addEventHandler(KeyEvent.KEY_RELEASED, handler)
}