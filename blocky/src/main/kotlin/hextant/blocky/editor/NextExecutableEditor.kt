/**
 *@author Nikolaus Knop
 */

package hextant.blocky.editor

import hextant.*
import hextant.base.AbstractEditor
import hextant.base.EditorSnapshot
import hextant.blocky.End
import hextant.blocky.Executable
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class NextExecutableEditor(context: Context) :
    AbstractEditor<Executable, EditorView>(context) {
    private val next = reactiveVariable(null as ExecutableEditor<*>?)

    override val result: EditorResult<Executable> = next.flatMap {
        it?.result?.map { res -> res.or(childErr()) } ?: reactiveValue(ok(End))
    }

    fun setNext(next: ExecutableEditor<*>) {
        this.next.set(next)
    }

    fun clearNext() {
        next.set(null)
    }

    private class Snapshot(original: NextExecutableEditor) : EditorSnapshot<NextExecutableEditor>(original) {
        private val nxt = original.next.now?.createSnapshot()

        override fun reconstruct(editor: NextExecutableEditor) {
            val e = nxt?.reconstruct(editor.context)
            if (e is ExecutableEditor<*>) editor.setNext(e)
        }
    }
}