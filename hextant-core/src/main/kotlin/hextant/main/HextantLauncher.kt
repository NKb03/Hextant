package hextant.main

import bundles.SimpleProperty
import hextant.command.line.CommandLine
import hextant.command.line.SingleCommandSource
import hextant.context.Context
import hextant.context.createControl
import hextant.core.view.EditorControl
import hextant.fx.*
import hextant.main.HextantApp.Companion.stage
import javafx.geometry.Pos.CENTER
import javafx.scene.text.Font

internal class HextantLauncher(private val context: Context) {
    private val root by lazy {
        hbox {
            setPrefSize(400.0, 400.0)
            alignment = CENTER
            add(vbox()) {
                setPrefSize(200.0, 400.0)
                alignment = CENTER
                spacing = 30.0
                add(label("Hextant")) {
                    font = Font(24.0)
                }
                add(createCommandLine(context))
            }
        }
    }

    private fun createCommandLine(localContext: Context): EditorControl<*> {
        val receiver = ProjectManager(localContext)
        val src = SingleCommandSource(localContext, receiver)
        val cl = CommandLine(localContext, src)
        return localContext.createControl(cl)
    }

    fun launch() {
        context[stage].scene.root = root
    }

    companion object : SimpleProperty<HextantLauncher>("hextant launcher")
}