package hextant

import hextant.base.EditorSnapshot
import hextant.core.Clipboard
import hextant.core.ClipboardContent.OneEditor
import hextant.core.editor.TransformedEditor
import validated.Validated
import validated.valid
import kotlin.reflect.typeOf

/**
 * Synonym for [apply]
 */
inline fun EditorControlFactory.configure(config: EditorControlFactory.() -> Unit) {
    apply(config)
}

/**
 * Syntactic sugar for `register(typeOf<T>(), factory)`
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> EditorFactory.register(noinline factory: (Context, T) -> Editor<T>) {
    @Suppress("UNCHECKED_CAST")
    register(typeOf<T>(), factory as (Context, Any) -> Editor<Any>)
}

/**
 * Syntactic sugar for `register(typeOf<T>(), factory)`
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> EditorFactory.register(noinline factory: (Context) -> Editor<T>) {
    register(typeOf<T>(), factory)
}

/**
 * Syntactic sugar for `createEditor(typeOf<T>(), context)`
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> EditorFactory.createEditor(context: Context) = createEditor(typeOf<T>(), context)

/**
 * Syntactic sugar for `createEditor(typeOf<T>(), result, context)`
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> EditorFactory.createEditor(result: T, context: Context) =
    createEditor(typeOf<T>(), result, context)

/**
 * Return a sequence iterating over all immediate and recursive children of this editor
 */
val Editor<*>.allChildren: Sequence<Editor<*>>
    get() {
        val directChildren = children.now.asSequence()
        return directChildren.asSequence() + directChildren.flatMap { it.allChildren }
    }

/**
 * If this editor is already in the specified [newContext] just returns it, otherwise copies the editor to the new context
 */
fun <E : Editor<*>> E.moveTo(newContext: Context): E =
    if (this.context == newContext) this else this.copyFor(newContext)

/**
 * Copy this editor for the given [newContext]
 */
fun <E : Editor<*>> E.copyFor(newContext: Context): E = snapshot().reconstruct(newContext)

/**
 * Copy this [Editor] to the [Clipboard], if this is supported by the editor.
 * Returns `true` only if the action was successful.
 */
fun Editor<*>.copyToClipboard(): Boolean {
    if (!supportsCopyPaste()) return false
    context[Clipboard].copy(OneEditor(snapshot()))
    return true
}

/**
 * Paste the [Clipboard]-content into this editor.
 * Returns `true` only if the action was successful.
 */
fun Editor<*>.pasteFromClipboard(): Boolean {
    val content = context[Clipboard].get()
    if (content !is OneEditor) return false
    return paste(content.snapshot)
}

/**
 * Returns a copy of the given Editor for the same [Context]
 */
inline fun <reified E : Editor<*>> E.copy(): E = copyFor(context)

/**
 * Return an editor that transforms the [Editor.result] of this editor with the given function.
 */
fun <T : Any, R : Any> Editor<T>.map(f: (T) -> Validated<R>): Editor<R> = TransformedEditor(this, f)

/**
 * Return an editor that transforms the [Editor.result] of this editor with the given function.
 */
@JvmName("simpleMap")
fun <T : Any, R : Any> Editor<T>.map(f: (T) -> R): Editor<R> = map { valid(f(it)) }

/**
 * Typesafe version of [Editor.createSnapshot]
 */
@Suppress("UNCHECKED_CAST")
fun <E : Editor<*>> E.snapshot(): EditorSnapshot<E> = createSnapshot() as EditorSnapshot<E>