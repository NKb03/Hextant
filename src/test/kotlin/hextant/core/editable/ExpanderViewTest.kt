/**
 *@author Nikolaus Knop
 */

package hextant.core.editable

import hextant.HextantPlatform
import hextant.core.EditorControlFactory
import hextant.core.configure
import hextant.core.expr.editable.*
import hextant.core.expr.view.FXIntLiteralEditorView
import hextant.core.expr.view.FXTextEditorView
import hextant.fx.hextantScene
import hextant.get
import hextant.view.FXExpanderView
import javafx.application.Application
import javafx.scene.Parent
import javafx.stage.Stage

class ExpanderViewTest : Application() {
    override fun start(stage: Stage) {
        stage.scene = hextantScene(createContent())
        stage.setOnHidden { System.exit(0) }
        stage.show()
    }

    companion object {
        private fun createContent(): Parent {
            val platform = HextantPlatform.configured()
            platform[EditorControlFactory].configure {
                register(EditableIntLiteral::class) { editable, ctx -> FXIntLiteralEditorView(editable, ctx) }
                register(EditableText::class) { editable, ctx -> FXTextEditorView(editable, ctx) }
            }
            val ex = ExpandableExpr()
            return FXExpanderView(ex, platform)
        }

        @JvmStatic fun main(args: Array<String>) {
            launch(ExpanderViewTest::class.java, *args)
        }
    }
}
