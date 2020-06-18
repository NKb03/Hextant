/**
 *@author Nikolaus Knop
 */

package hextant.base

import hextant.context.Context
import hextant.core.Editor
import hextant.core.Snapshot
import hextant.core.editor.getSimpleEditorConstructor
import kotlin.reflect.KClass

/**
 * A special [Snapshot] for [Editor]s
 */
abstract class EditorSnapshot<Original : Editor<*>>(original: KClass<out Original>) :
    Snapshot<Original, Context> {
    constructor(original: Original) : this(original::class)

    private val originalClass = original.java.name ?: error("Class has no name")

    /**
     * Reconstruct the given [editor] from this snapshot.
     */
    abstract fun reconstruct(editor: Original)

    @Suppress("UNCHECKED_CAST")
    override fun reconstruct(info: Context): Original {
        val cls = Class.forName(originalClass).kotlin
        val e = cls.getSimpleEditorConstructor().invoke(info) as Original
        reconstruct(e)
        return e
    }
}