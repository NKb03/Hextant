/**
 *@author Nikolaus Knop
 */

package hextant.command.line

import hextant.command.Command
import hextant.command.Command.Type.MultipleReceivers
import hextant.command.Command.Type.SingleReceiver
import hextant.command.Commands
import hextant.command.line.CommandReceiverType.*
import hextant.context.Context
import hextant.context.SelectionDistributor
import hextant.core.Editor
import reaktive.collection.binding.all
import reaktive.collection.binding.anyR
import reaktive.list.reactive
import reaktive.set.ReactiveSet
import reaktive.set.binding.mapNotNull
import reaktive.set.unmodifiableReactiveSet
import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveValue
import reaktive.value.binding.map
import reaktive.value.now
import reaktive.value.reactiveValue

/**
 * A [CommandSource] the uses the [SelectionDistributor] and the [Commands] of the given context.
 */
class ContextCommandSource(
    private val context: Context,
    private val distributor: SelectionDistributor,
    private val commands: Commands,
    receiverTypes: Set<CommandReceiverType>
) : CommandSource {
    constructor(
        platform: Context,
        distributor: SelectionDistributor,
        commands: Commands,
        vararg receiverTypes: CommandReceiverType
    ) : this(platform, distributor, commands, receiverTypes.toSet())

    constructor(context: Context, vararg receiverTypes: CommandReceiverType) : this(
        context,
        context[SelectionDistributor],
        context[Commands],
        *receiverTypes
    )

    private val selectedReceivers = receiverTypes.map { t -> selectedReceivers(t) }
    private val focusedReceivers = receiverTypes.map { t -> focusedReceiver(t) }

    private fun selectedReceivers(type: CommandReceiverType): ReactiveSet<Any> = when (type) {
        Views     -> distributor.selectedViews
        Targets   -> distributor.selectedTargets
        Expanders -> distributor.selectedTargets.mapNotNull { t -> (t as? Editor<*>)?.expander }
        Global -> unmodifiableReactiveSet(setOf(context))
    }

    private fun focusedReceiver(type: CommandReceiverType): ReactiveValue<Any?> = when (type) {
        Views     -> distributor.focusedView
        Targets   -> distributor.focusedTarget
        Expanders -> distributor.focusedTarget.map { t -> (t as? Editor<*>)?.expander }
        Global -> reactiveValue(context)
    }

    override fun executeCommand(command: Command<*, *>, arguments: List<Any>): List<Any> =
        when (command.commandType) {
            SingleReceiver -> focusedReceivers.asSequence()
                .map { it.now }
                .filterNotNull()
                .filter { r -> command.isApplicableOn(r) }.take(1)
                .map { r ->
                    @Suppress("UNCHECKED_CAST") //we checked this with the filter
                    command as Command<Any, *>
                    command.execute(r, arguments)
                }.toList()
            MultipleReceivers -> selectedReceivers.asSequence()
                .filter { selected -> selected.now.all { command.isApplicableOn(it) } }
                .flatMap { selected -> selected.now.asSequence() }
                .map { r ->
                    @Suppress("UNCHECKED_CAST") //we checked this with the filter
                    command as Command<Any, *>
                    command.execute(r, arguments)
                }.toList()
        }

    override fun availableCommands(): Collection<Command<*, *>> = selectedReceivers
        .flatMap { selected ->
            intersect(selected.now.map { r ->
                commands.applicableOn(r).filter { it.commandType == MultipleReceivers }
            })
        }
        .union(focusedReceivers.flatMap { focused ->
            focused.now?.let { f ->
                commands.applicableOn(f).filter { c -> c.commandType == SingleReceiver }
            } ?: emptySet<Command<*, *>>()
        })

    override fun isApplicable(command: Command<*, *>): ReactiveBoolean = when (command.commandType) {
        MultipleReceivers -> selectedReceivers.reactive().anyR { selected ->
            selected.all { r -> command.isApplicableOn(r) }
        }
        SingleReceiver    -> focusedReceivers.reactive().anyR { focused ->
            focused.map { r -> r != null && command.isApplicableOn(r) }
        }
    }

    companion object {
        private fun <E> intersect(sets: Iterable<Collection<E>>): Set<E> {
            if (!sets.any()) return emptySet()
            val intersection = sets.first().toMutableSet()
            for (e in sets.drop(1)) intersection.retainAll(e)
            return intersection
        }
    }
}