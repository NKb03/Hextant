/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant

import org.nikok.hextant.core.*
import org.nikok.hextant.core.command.Commands
import org.nikok.hextant.core.impl.SelectionDistributor
import org.nikok.hextant.core.inspect.Inspections
import org.nikok.hextant.prop.*
import org.nikok.hextant.prop.PropertyHolder.Init
import java.util.concurrent.*
import java.util.logging.Logger
import kotlin.properties.ReadOnlyProperty

/**
 * The hextant platform, mainly functions as a [PropertyHolder] to manage properties of the hextant platform
 */
interface HextantPlatform : PropertyHolder {
    /**
     * Enqueues the specified [action] in the Hextant main thread
     */
    fun <T> runLater(action: () -> T): Future<T>

    fun exit()

    override fun copy(init: PropertyHolder.Init.() -> Unit): HextantPlatform

    /**
     * The default instance of the [HextantPlatform]
     */
    private class Impl(propertyHolder: HextantPlatform.() -> PropertyHolder) : HextantPlatform {
        private val executor = Executors.newSingleThreadExecutor()

        private val propertyHolder = propertyHolder(this)

        override fun <T> runLater(action: () -> T): Future<T> {
            val future = executor.submit(action)
            return CompletableFuture.supplyAsync { future.get() }.exceptionally { it.printStackTrace(); throw it }
        }

        override fun exit() {
            executor.shutdown()
        }

        override fun copy(init: Init.() -> Unit): HextantPlatform {
            return HextantPlatform.withPropertyHolder { propertyHolder.copy(init) }
        }

        override fun <T : Any, Read : Permission> get(permission: Read, property: Property<out T, Read, *>): T =
            propertyHolder[permission, property]

        override fun <T : Any, Write : Permission> set(
            permission: Write,
            property: Property<in T, *, Write>,
            value: T
        ) {
            propertyHolder[permission, property] = value
        }

        override fun <T : Any, Write : Permission> setFactory(
            permission: Write,
            property: Property<in T, *, Write>,
            factory: () -> T
        ) {
            propertyHolder.setFactory(permission, property, factory)
        }

        override fun <T : Any, Write : Permission> setBy(
            permission: Write,
            property: Property<in T, *, Write>,
            delegate: ReadOnlyProperty<Nothing?, T>
        ) {
            propertyHolder.setBy(permission, property, delegate)
        }
    }

    companion object {
        fun HextantPlatform.initDefaultProperties(): PropertyHolder = PropertyHolder.newInstance {
            val platform = this@initDefaultProperties
            set(Version, Version(1, 0, isSnapshot = true))
            set(SelectionDistributor, SelectionDistributor.newInstance(platform))
            val cl = platform.javaClass.classLoader
            set(EditorViewFactory, EditorViewFactory.newInstance(platform, cl))
            set(EditableFactory, EditableFactory.newInstance(cl))
            set(Commands, Commands.newInstance(platform))
            set(Inspections, Inspections.newInstance(platform))
            val expanderFactory = ExpanderFactory.newInstance(cl, platform)
            set(ExpanderFactory, expanderFactory)
            set(EditorFactory, EditorFactory.newInstance(cl, platform))
            set(CoreProperties.logger, Logger.getLogger("org.nikok.hextant"))
        }

        fun withPropertyHolder(propertyHolder: HextantPlatform.() -> PropertyHolder): HextantPlatform =
            Impl(propertyHolder)

        val INSTANCE = newInstance()

        fun newInstance(init: Init.() -> Unit = {}) =
            HextantPlatform.withPropertyHolder { initDefaultProperties().copy(init) }
    }
}

