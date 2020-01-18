/**
 *@author Nikolaus Knop
 */

package hextant

import hextant.base.AbstractContext
import hextant.bundle.Bundle
import hextant.bundle.CoreProperties
import hextant.command.Commands
import hextant.impl.*
import hextant.inspect.Inspections
import hextant.main.InputMethod
import hextant.plugin.PluginRegistry
import hextant.serial.HextantSerialContext
import hextant.serial.SerialProperties
import hextant.undo.UndoManager
import kserial.KSerial
import java.util.concurrent.*
import java.util.logging.Logger

/**
 * The hextant platform, mainly functions as a [Bundle] to manage properties of the hextant platform
 */
interface HextantPlatform : Context {
    /**
     * Enqueues the specified [action] in the Hextant main thread
     */
    fun <T> runLater(action: () -> T): Future<T>

    /**
     * Stops all threads owned by this [HextantPlatform]
     */
    fun exit()

    /**
     * The default instance of the [HextantPlatform]
     */
    private abstract class Base(bundle: Bundle) : HextantPlatform, AbstractContext(null, bundle) {
        override val platform: HextantPlatform
            get() = this
    }

    private class SingleThreaded(bundle: Bundle) : Base(bundle) {
        override fun exit() {}

        override fun <T> runLater(action: () -> T): Future<T> = CompletableFuture.completedFuture(action())
    }

    private class MultiThreaded(bundle: Bundle) : Base(bundle) {
        private val executor = Executors.newSingleThreadExecutor()

        override fun exit() {
            executor.shutdown()
        }

        override fun <T> runLater(action: () -> T): Future<T> {
            val future = executor.submit(action)
            return CompletableFuture.supplyAsync { future.get() }.exceptionally { it.printStackTrace(); throw it }
        }
    }

    companion object {
        /**
         * Return a configured Platform with a
         * * [SelectionDistributor]
         * * [EditorControlFactory]
         * * [Commands] registrar
         * * [Inspections] registrar
         * * [CoreProperties.logger]
         * * [Stylesheets] configurator
         * * [EditorControlGroup]
         * * [PluginRegistry]
         * * [SerialProperties.serialContext]
         * * [CoreProperties.clipboard]
         * * [CoreProperties.classLoader]
         * * [UndoManager]
         * * [InputMethod]
         */
        fun configured(bundle: Bundle = Bundle.newInstance()): HextantPlatform =
            unconfigured(bundle).apply { configure() }

        fun newInstance(): HextantPlatform = unconfigured().apply {
            set(EditorControlFactory, EditorControlFactory.newInstance())
            set(EditorFactory, EditorFactory.newInstance())
            set(Commands, Commands.newInstance())
            set(Inspections, Inspections.newInstance())
            set(Stylesheets, Stylesheets())
            val plugins = PluginRegistry(this, Settings.plugins)
            set(PluginRegistry, plugins)
            set(SerialProperties.serialContext, HextantSerialContext(this, plugins.compoundClassLoader))
            set(SerialProperties.serial, KSerial.newInstance())
        }

        private fun HextantPlatform.configure() {
            set(SelectionDistributor, SelectionDistributor.newInstance())
            set(EditorControlFactory, EditorControlFactory.newInstance())
            set(EditorFactory, EditorFactory.newInstance())
            set(Commands, Commands.newInstance())
            set(Inspections, Inspections.newInstance())
            set(CoreProperties.logger, Logger.getLogger(javaClass.name))
            set(Stylesheets, Stylesheets())
            set(EditorControlGroup, EditorControlGroup())
            val plugins = PluginRegistry(this, Settings.plugins)
            set(PluginRegistry, plugins)
            set(SerialProperties.serial, KSerial.newInstance())
            set(SerialProperties.serialContext, HextantSerialContext(this, plugins.compoundClassLoader))
            set(CoreProperties.classLoader, plugins.compoundClassLoader)
            set(UndoManager, UndoManager.newInstance())
            set(InputMethod, InputMethod.REGULAR)
        }

        /**
         * Return a [HextantPlatform] that only has a [EditorFactory] property.
         * Note that this property always returns the same [HextantPlatform]
         */
        val forTesting = unconfigured().apply {
            set(EditorFactory, EditorFactory.newInstance())
        }

        /**
         * Return a [HextantPlatform] without any properties
         */
        fun unconfigured(bundle: Bundle = Bundle.newInstance()): HextantPlatform = SingleThreaded(bundle)
    }
}