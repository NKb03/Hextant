/**
 *@author Nikolaus Knop
 */

package hextant.base

import hextant.Context
import hextant.HextantPlatform
import hextant.bundle.*

/**
 * Skeletal implementation of [Context] using the specified [parent] and [bundle]
 */
open class AbstractContext(final override val parent: Context?, private val bundle: Bundle = Bundle.newInstance()) :
    Context, Bundle by bundle {
    override val platform: HextantPlatform
        get() = TODO("not implemented")

    override fun <T, Read : Permission> get(permission: Read, property: Property<out T, Read, *>): T =
        try {
            bundle[permission, property]
        } catch (e: NoSuchPropertyException) {
            if (parent == null) throw e
            parent[permission, property]
        }
}