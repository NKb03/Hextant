/**
 *@author Nikolaus Knop
 */

package hextant.settings.model

import hextant.bundle.Internal
import hextant.bundle.Property

/**
 * This class manages properties that can be configured in a settings editor.
 */
class ConfigurableProperties {
    private val properties = mutableMapOf<String, ConfigurableProperty>()

    /**
     * Register the specified [property] as configurable.
     */
    fun register(property: ConfigurableProperty) {
        val name = property.property.name ?: error("Configurable properties must have a name")
        properties[name] = property
    }

    /**
     * Register a [ConfigurableProperty] with the specified [property] and the type [T].
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> register(property: Property<T, *, *>) {
        register(
            ConfigurableProperty(
                property as Property<Any, Any, Any>,
                T::class
            )
        )
    }

    internal fun byName(name: String) = properties[name]

    internal fun all(): Collection<ConfigurableProperty> = properties.values

    companion object : Property<ConfigurableProperties, Any, Internal>("configurable properties")
}