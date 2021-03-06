/**
 * @author Nikolaus Knop
 */

package hextant.core.editor

import hextant.context.Context
import hextant.core.Editor

/**
 * A factory for [ExpanderDelegate]'s
 */
interface ExpanderDelegator<out E : Editor<*>> {
    fun getDelegate(): ExpanderDelegate<E>
}

abstract class ExpanderConfigurator<E : Editor<*>>(configure: ExpanderConfig<E>.() -> Unit) : ExpanderDelegator<E> {
    val config = ExpanderConfig<E>().apply(configure)

    final override fun getDelegate(): ExpanderConfig<E> = config
}

abstract class SimpleExpanderDelegator<E : Editor<*>>(private val function: (text: String, ctx: Context) -> E?) :
    ExpanderDelegator<E> {
    private val delegate = object : ExpanderDelegate<E> {
        override fun expand(text: String, context: Context): E? = function(text, context)

        override fun expand(item: Any, context: Context): E? = null
    }

    override fun getDelegate(): ExpanderDelegate<E> = delegate
}