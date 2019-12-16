/**
 * @author Nikolaus Knop
 */

package hextant.bundle

/**
 * @return the value of the [property] in this [Bundle] or `null` if it isn't configured
 */
fun <T : Any, P : Permission> Bundle.getOrNull(permission: P, property: Property<T, P, *>): T? =
    if (hasProperty(property)) get(permission, property)
    else null