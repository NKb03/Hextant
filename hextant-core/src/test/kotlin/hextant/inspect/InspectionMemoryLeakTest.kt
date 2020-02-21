/**
 *@author Nikolaus Knop
 */

package hextant.inspect

import com.natpryce.hamkrest.absent
import com.sun.javafx.application.PlatformImpl
import hextant.*
import hextant.expr.editor.ExprExpander
import hextant.expr.editor.IntLiteralEditor
import hextant.test.shouldBe
import hextant.undo.NoUndoManager
import hextant.undo.UndoManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.nikok.kref.*
import reaktive.value.*
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

class InspectionMemoryLeakTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun startupPlatform() {
            PlatformImpl.startup {}
        }
    }

    @Test
    fun `get problems of editor causes no memory leak`() {
        class Inspected(val error: ReactiveBoolean)

        val w = wrapper(strong(Inspected(reactiveValue(true))))
        val e by w
        val i = Inspections.newInstance()
        i.of<Inspected>().registerInspection {
            location(inspected)
            description = "An inspection"
            message { "error" }
            isSevere(true)
            preventingThat(inspected.error)
            addFix {
                description = "Fix"
                fixingBy { println(inspected) }
            }
        }
        i.getProblems(e!!)
        w.ref = weak(e!!)
        System.gc()
        e shouldBe absent()
    }

    @Test
    fun `constructing view of editor causes no memory leak`() {
        val ctx = HextantPlatform.rootContext()
        val w = wrapper(strong(IntLiteralEditor(ctx)))
        val e by w
        val v = WeakReference(ctx.createView(e!!), ReferenceQueue())
        w.ref = weak(e!!)
        gc()
        e shouldBe absent()
        v.get() shouldBe absent()
    }

    @Test
    fun `expanding and resetting expander causes no leak`() {
        val ctx = HextantPlatform.rootContext()
        ctx[UndoManager] = NoUndoManager
        val exp = ExprExpander(ctx)
        val view = ctx.createView(exp)
        exp.setText("1")
        exp.expand()
        val e by weak(exp.editor.now!!)
        val v by weak(ctx[EditorControlGroup].getViewOf(e!!))
        exp.reset()
        gc()
        e shouldBe absent()
        v shouldBe absent()
        println(view)
    }

    private fun gc() {
        repeat(10) {
            Thread.sleep(10)
            System.gc()
        }
    }
}