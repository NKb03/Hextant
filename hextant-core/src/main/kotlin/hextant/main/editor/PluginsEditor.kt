/**
 *@author Nikolaus Knop
 */

package hextant.main.editor

import hextant.context.Context
import hextant.core.editor.AbstractEditor
import hextant.main.HextantPlatform.marketplace
import hextant.main.plugins.PluginException
import hextant.main.plugins.PluginManager
import hextant.main.view.PluginsEditorView
import hextant.plugins.Plugin
import hextant.plugins.PluginInfo.Type
import kotlinx.coroutines.*
import reaktive.value.reactiveValue
import validated.valid

internal class PluginsEditor(
    context: Context,
    private val manager: PluginManager,
    private val types: Set<Type>
) : AbstractEditor<Collection<Plugin>, PluginsEditorView>(context) {
    override val result get() = reactiveValue(valid(manager.enabledPlugins()))

    fun enable(plugin: Plugin, view: PluginsEditorView) {
        val activated = try {
            manager.enable(plugin, view::confirmEnable) ?: return
        } catch (e: PluginException) {
            return view.alertError(e.message!!)
        }
        views {
            GlobalScope.launch {
                available.removeAll(activated)
                enabled.addAll(activated.filter { it.matches(enabledSearchText) })
            }
        }
    }

    fun disable(plugin: Plugin, view: PluginsEditorView) {
        val disabled = try {
            manager.disable(plugin, view::confirmDisable, view::askDisable) ?: return
        } catch (e: PluginException) {
            return view.alertError(e.message!!)
        }

        views {
            GlobalScope.launch {
                available.addAll(disabled.filter { it.matches(availableSearchText) })
                enabled.removeAll(disabled)
            }
        }
    }

    fun searchInAvailable(view: PluginsEditorView) {
        GlobalScope.launch {
            val marketplace = context[marketplace]
            withContext(Dispatchers.Default) { }
            val available = marketplace.getPlugins(view.availableSearchText, LIMIT, types, manager.enabledIds())
            view.available.clear()
            view.available.addAll(available.map { id -> manager.getPlugin(id) })
        }
    }

    fun searchInEnabled(view: PluginsEditorView) {
        GlobalScope.launch {
            val enabled = manager.enabledPlugins().filter { it.matches(view.enabledSearchText) }
            view.enabled.clear()
            view.enabled.addAll(enabled)
        }
    }

    override fun viewAdded(view: PluginsEditorView) {
        searchInEnabled(view)
        searchInAvailable(view)
    }

    private suspend fun Plugin.matches(searchText: String): Boolean {
        if (info.await().type !in types) return false
        return info.await().name.matches(searchText) || info.await().author.matches(searchText) || id.matches(searchText)
    }

    private fun String.matches(searchText: String) = startsWith(searchText)

    companion object {
        private const val LIMIT = 20
    }
}