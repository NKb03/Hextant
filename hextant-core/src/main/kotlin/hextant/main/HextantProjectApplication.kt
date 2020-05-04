/**
 *@author Nikolaus Knop
 */

package hextant.main

import hextant.*
import hextant.base.EditorControl
import hextant.bundle.Internal

import hextant.core.view.ListEditorControl
import hextant.fx.*
import hextant.impl.SelectionDistributor
import hextant.serial.SerialProperties
import hextant.undo.UndoManager
import javafx.application.Application
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import reaktive.value.now
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.cast

abstract class HextantProjectApplication<R : Editor<*>> : Application() {
    private val rootType = extractRootEditorType(this::class)

    private val platform = HextantPlatform.rootContext()
    private val toplevelContext = Context.newInstance(platform) {
        defaultConfig()
        set(Internal, PathChooser, FXPathChooser())
    }

    private val projects = PathListEditor(toplevelContext)

    init {
        platform[Internal, ProjectType] = this.projectType()
        val path = projectsPath()
        if (Files.exists(path)) {
            val input = platform.createInput(path)
            input.readInplace(projects)
            input.close()
        }
    }

    override fun stop() {
        val output = platform.createOutput(projectsPath())
        output.writeUntyped(projects)
        output.close()
    }

    private fun projectsPath() = configurationDirectory().resolve("recent.ks")

    protected abstract fun configurationDirectory(): Path

    protected abstract fun projectType(): ProjectType<R>

    private fun createContext(): Context = Context.newInstance {
        defaultConfig()
        configure()
    }

    protected open fun Context.configure() {}

    protected abstract fun createView(editor: R): Parent

    protected open fun MenuBarBuilder.configureMenuBar(editor: R) {}

    override fun start(primaryStage: Stage) {
        platform[Internal, HextantApplication.stage] = primaryStage
        primaryStage.isResizable = false
        showStartView()
    }

    private fun Context.defaultConfig() {
        set(UndoManager, UndoManager.newInstance())
        set(SelectionDistributor, SelectionDistributor.newInstance())
        set(EditorControlGroup, EditorControlGroup())
    }

    private fun showStartView() {
        val context = toplevelContext
        val logo = Image(
            HextantProjectApplication::class.java.getResource("preloader-image.jpg").toExternalForm(),
            400.0,
            400.0,
            true, true
        )
        val v = context.createView(projects) {
            set(ListEditorControl.ORIENTATION, ListEditorControl.Orientation.Vertical)
            set(ListEditorControl.EMPTY_DISPLAY) { Button("Open or create new project") }
            set(ListEditorControl.CELL_FACTORY, ::Cell)
        }
        val root = HBox(v, ImageView(logo))
        val stage = platform[Internal, HextantApplication.stage]
        stage.scene = Scene(root)
        stage.scene.initHextantScene(context)
        stage.show()
    }

    private inner class Cell : ListEditorControl.Cell<EditorControl<*>>() {
        override fun updateItem(item: EditorControl<*>) {
            root = item
            check(item is PathEditorControl)
            item.root.onAction {
                val editor = item.editor
                val path = editor.result.now.force()
                showProject(path)
            }
        }
    }

    private fun showProject(path: Path) {
        val input = toplevelContext.createInput(path.resolve(PROJECT_FILE))
        val ctx = createContext()
        ctx[SerialProperties.projectRoot] = path
        val project = rootType.cast(input.readObject())
        showProject(project)
    }

    private fun showProject(root: R) {
        val view = createView(root)
        val menu = createMenuBar(root)
        val stage = platform[Internal, HextantApplication.stage]
        stage.scene.root = VBox(menu, view)
    }

    private fun createMenuBar(root: R) = menuBar {
        menu("File") {
            item("Close project") {
                saveProject(root)
                showStartView()
            }
        }
        configureMenuBar(root)
    }

    private fun saveProject(editor: R) {
        val path = editor.context[SerialProperties.projectRoot].resolve(PROJECT_FILE)
        val output = editor.context.createOutput(path)
        output.writeObject(editor)
    }

    companion object {
        private const val PROJECT_FILE = "project.hxt"

        @Suppress("UNCHECKED_CAST")
        private fun <R : Editor<*>> extractRootEditorType(
            cls: KClass<out HextantProjectApplication<R>>
        ): KClass<R> {
            val supertype = cls.allSupertypes.find {
                it.classifier == HextantProjectApplication::class
            } ?: throw AssertionError()
            val r = supertype.arguments[0].type?.classifier
            if (r !is KClass<*>) throw AssertionError()
            return r as KClass<R>
        }
    }
}
