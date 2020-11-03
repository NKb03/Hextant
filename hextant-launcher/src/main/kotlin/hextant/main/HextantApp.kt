/**
 *@author Nikolaus Knop
 */

package hextant.main

import hextant.context.Properties.classLoader
import hextant.context.Properties.defaultContext
import hextant.core.Core
import hextant.main.GlobalDirectory.Companion.GLOBAL_PLUGINS
import hextant.main.HextantPlatform.marketplace
import hextant.main.HextantPlatform.projectContext
import hextant.main.HextantPlatform.stage
import hextant.main.plugins.PluginManager
import hextant.plugin.PluginBuilder.Phase
import hextant.plugin.initializePluginsFromClasspath
import javafx.application.Application
import javafx.stage.Stage
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.io.IOException

internal class HextantApp : Application() {
    private val globalContext = HextantPlatform.globalContext()
    private val launcherContext = defaultContext(projectContext(globalContext))

    override fun start(primaryStage: Stage) {
        globalContext[stage] = primaryStage
        globalContext[ProjectManager] = ProjectManager(launcherContext)
        val globalPlugins = readGlobalPlugins()
        val manager = PluginManager(globalContext[marketplace], emptyList())
        globalContext[PluginManager] = manager
        manager.enableAll(globalPlugins)
        launcherContext.setProjectRoot(launcherContext[GlobalDirectory].root.toPath())
        initializePluginsFromClasspath(launcherContext, launcherContext[classLoader])
        val args = parameters.raw
        when (args.size) {
            0 -> {
                val cl = HextantClassLoader(globalContext, plugins = emptyList())
                cl.executeInNewThread("hextant.main.HextantLauncher", globalContext, launcherContext)
            }
            1 -> {
                val name = args[0]
                val project = tryGetFile(name)
                if (!project.exists()) Main.fail("File with name $project does not exist")
                globalContext[ProjectManager].openProject(name)
            }
            2 -> Main.fail("Too many arguments")
        }
        primaryStage.sceneProperty().addListener { sc -> if (sc != null) primaryStage.show() }
    }

    override fun stop() {
        Core.apply(launcherContext, Phase.Close, project = null)
        HextantMain.apply(launcherContext, Phase.Close, project = null)
        saveGlobalPlugins()
    }

    private fun readGlobalPlugins(): List<String> {
        val file = globalContext[GlobalDirectory][GLOBAL_PLUGINS]
        if (!file.exists()) return emptyList()
        return Json.tryParse(GLOBAL_PLUGINS) { file.readText() } ?: emptyList()
    }

    private fun saveGlobalPlugins() {
        val enabled = globalContext[PluginManager].enabledPlugins().map { it.id }
        val file = globalContext[GlobalDirectory][GLOBAL_PLUGINS]
        file.writeText(Json.encodeToString(serializer(), enabled))
    }

    private fun tryGetFile(s: String): File = try {
        val f = File(s)
        f.canonicalPath
        f
    } catch (ex: IOException) {
        Main.fail("Invalid path $s")
    }
}