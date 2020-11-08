/**
 *@author Nikolaus Knop
 */

package hextant.command

import hextant.command.Command.Category
import hextant.command.Command.Type
import hextant.command.Command.Type.MultipleReceivers
import hextant.config.AbstractEnabled
import kotlin.reflect.KClass

/**
 * Skeletal implementation of [Command]
 * @constructor
 */
abstract class AbstractCommand<R : Any, T : Any>(
    override val receiverCls: KClass<R>,
    initiallyEnabled: Boolean = true
) : Command<R, T>, AbstractEnabled(initiallyEnabled) {
    override val shortName: String?
        get() = null
    override val category: Category?
        get() = null

    override val id: String
        get() = shortName ?: name

    override val commandType: Type
        get() = MultipleReceivers

    /**
     * Execute this [Command] on the specified [receiver] and the specified [args]
     */
    protected abstract fun doExecute(receiver: R, args: CommandArguments): T

    /**
     * Check that the specified [arguments] match [parameters] and then call [doExecute]
     */
    override fun execute(receiver: R, arguments: List<Any>): T {
        val args = CommandArguments(parameters, arguments)
        return doExecute(receiver, args)
    }

    /**
     * By default return `true` if the specified [receiver] is an instance of [R]
     */
    override fun isApplicableOn(receiver: Any) = receiverCls.isInstance(receiver)
}