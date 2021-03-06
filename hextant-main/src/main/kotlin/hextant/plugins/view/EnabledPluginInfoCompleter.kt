/**
 * @author Nikolaus Knop
 */

package hextant.plugins.view

import hextant.completion.Completion
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor
import hextant.plugins.PluginInfo
import hextant.plugins.PluginManager
import kotlinx.coroutines.runBlocking

internal object EnabledPluginInfoCompleter : ConfiguredCompleter<Editor<*>, PluginInfo>(CompletionStrategy.simple) {
    override fun extractText(context: Editor<*>, item: PluginInfo): String = item.id

    override fun Completion.Builder<PluginInfo>.configure(context: Editor<*>) {
        infoText = "${completion.name} by ${completion.author}"
        tooltipText = completion.description
    }

    override fun completionPool(context: Editor<*>): Collection<PluginInfo> =
        runBlocking { context.context[PluginManager].enabledPlugins().map { it.info.await() } }
}