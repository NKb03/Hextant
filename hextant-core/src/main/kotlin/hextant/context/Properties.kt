/**
 *@author Nikolaus Knop
 */

package hextant.context

import bundles.*
import hextant.command.Commands
import hextant.command.line.*
import hextant.config.FeatureRegistrar
import hextant.config.PropertyRegistrar
import hextant.fx.*
import hextant.inspect.Inspections
import hextant.plugins.Aspects
import hextant.plugins.HextantClassLoader
import hextant.plugins.LocalPluginRepository
import hextant.plugins.Marketplace
import hextant.serial.Files
import hextant.undo.UndoManager
import javafx.stage.Stage
import java.util.logging.Logger

/**
 * The hextant platform is responsible for creating the root context.
 */
object Properties {
    /**
     * The logger property
     */
    val logger = property<Logger, Internal>("top level logger")


    /**
     * The [PropertyChangeHandler] is used to register properties for views.
     */
    val propertyChangeHandler = property<PropertyChangeHandler, Internal>("property change handlers")

    /**
     * The class loader to be used by all plugins.
     */
    val classLoader = property<ClassLoader, Internal>("class loader")

    val marketplace = publicProperty<Marketplace>("marketplace")

    val stage = publicProperty<Stage>("stage")

    val globalContext = publicProperty<Context>("global context")

    /**
     * Create the global context, that is, the root of all contexts.
     */
    fun globalContext(): Context = Context.newInstance {
        val gd = Files.inUserHome()
        set(Files, gd)
        set(marketplace, LocalPluginRepository(gd[Files.PLUGIN_CACHE]))
    }

    /**
     * Create the root context for a project.
     */
    fun projectContext(global: Context): Context {
        val cl = Thread.currentThread().contextClassLoader
        val hcl = if (cl is HextantClassLoader) cl else HextantClassLoader(global, emptyList(), cl)
        return projectContext(global, hcl).apply {
            set(globalContext, global)
        }
    }

    /**
     * Initialize some common properties on the project level.
     */
    fun projectContext(parent: Context, loader: ClassLoader) =
        parent.extend {
            set(Internal, Commands, Commands.newInstance())
            set(Internal, Inspections, Inspections.newInstance())
            set(Internal, Stylesheets, Stylesheets(loader))
            set(Internal, logger, Logger.getLogger(javaClass.name))
            set(Internal, propertyChangeHandler, PropertyChangeHandler())
            set(PropertyRegistrar, PropertyRegistrar())
            set(Internal, Aspects, Aspects())
            set(ResultStyleClasses, ResultStyleClasses())
            set(Internal, classLoader, loader)
        }

    /**
     * Extend the given [context] with some core properties.
     */
    fun defaultContext(context: Context) = context.extend {
        set(FeatureRegistrar, FeatureRegistrar(this))
        set(SelectionDistributor, SelectionDistributor.newInstance())
        set(EditorControlGroup, EditorControlGroup())
        set(UndoManager, UndoManager.newInstance())
        set(Clipboard, SimpleClipboard())
        set(InputMethod, InputMethod.REGULAR)
        val clContext = extend {
            set(SelectionDistributor, SelectionDistributor.newInstance())
        }
        set(CommandLine, CommandLine(clContext, ContextCommandSource(this, *CommandReceiverType.values())))
    }
}