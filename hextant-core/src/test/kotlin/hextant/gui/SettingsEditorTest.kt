/**
 *@author Nikolaus Knop
 */

package hextant.gui

import bundles.SimpleProperty
import hextant.context.*
import hextant.fx.registerShortcuts
import hextant.main.HextantApplication
import hextant.serial.makeRoot
import hextant.settings.editor.SettingsEditor
import hextant.settings.model.ConfigurableProperties
import hextant.settings.model.Settings
import javafx.scene.Parent

class SettingsEditorTest : HextantApplication() {
    private val test = SimpleProperty.withDefault("name", "<default>")

    override fun createView(context: Context): Parent {
        context[ConfigurableProperties].register(test)
        val editor = SettingsEditor(context)
        editor.makeRoot()
        context[Internal, Settings] = editor.settings
        return context.createControl(editor).apply {
            registerShortcuts {
                on("Ctrl+D") {
                    println(context[Settings][test])
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(SettingsEditorTest::class.java)
        }
    }
}