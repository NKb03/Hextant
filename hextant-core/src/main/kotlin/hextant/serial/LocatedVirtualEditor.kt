/**
 *@author Nikolaus Knop
 */

package hextant.serial

import hextant.core.Editor
import java.lang.ref.WeakReference

internal class LocatedVirtualEditor<E : Editor<*>>(
    ref: E,
    private val root: VirtualFile<Editor<*>>,
    private val location: EditorLocation<E>
) : VirtualEditor<E> {
    private var weak = WeakReference(ref)

    override fun get(): E {
        weak.get()?.let { return it }
        val r = root.get()
        val e = location.locateIn(r)
        weak = WeakReference(e)
        return e
    }
}