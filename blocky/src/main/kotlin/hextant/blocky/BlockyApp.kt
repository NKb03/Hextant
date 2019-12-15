/**
 *@author Nikolaus Knop
 */

package hextant.blocky

import hextant.*
import hextant.blocky.editor.ProgramEditor
import hextant.fx.*
import hextant.fx.ModifierValue.DOWN
import hextant.main.HextantApplication
import hextant.undo.UndoManager
import javafx.scene.Parent
import javafx.scene.input.KeyCode.E
import reaktive.value.now

class BlockyApp : HextantApplication() {
    override fun createContext(platform: HextantPlatform): Context = Context.newInstance(platform) {
        set(UndoManager, UndoManager.newInstance())
    }

    override fun createView(context: Context): Parent {
        val e = ProgramEditor(context)
        val view = context.createView(e)
        view.registerShortcuts {
            on(shortcut(E) { control(DOWN) }) {
                val prog = e.result.now.ifErr { return@on }
                execute(prog)
            }
        }
        return view
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(BlockyApp::class.java)
        }
    }
}