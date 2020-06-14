/**
 *@author Nikolaus Knop
 */

@file:Suppress("UNCHECKED_CAST")

package hextant.settings.editors

import bundles.*
import hextant.*
import hextant.base.CompoundEditor
import hextant.core.Internal
import hextant.settings.model.ConfigurableProperties
import reaktive.Observer
import reaktive.list.ListChange.Added
import reaktive.list.ListChange.Removed
import reaktive.value.*
import validated.Validated.Valid
import validated.reaktive.ReactiveValidated
import validated.valid

/**
 * An editor that can be used to configure properties. It is implemented as a list of property-value entries.
 */
class SettingsEditor(context: Context) : CompoundEditor<Bundle>(context) {
    private val bundle = createBundle()

    private val valueEditor = mutableMapOf<Property<*, *, *>, BidirectionalEditor<*>>()

    /**
     * The [Bundle] configured by this editor.
     * Settings the value of a property of this [Bundle] causes this [Editor] to add or modify an entry accordingly.
     */
    val settings = object : Bundle by bundle {
        override fun <Write : Any, T> set(write: Write, property: Property<in T, *, Write>, value: T) {
            if (value == null) throw AssertionError()
            bundle[write, property] = value
            val editor = valueEditor[property] as BidirectionalEditor<Any>?
            if (editor != null) editor.setResult(value)
            else {
                val ex = SettingsEntryExpander(context)
                val properties = context[Internal, ConfigurableProperties]
                val name = property.name ?: throw RuntimeException("Property configured in settings must have a name!")
                val p = properties.byName(name)
                    ?: throw RuntimeException("Property configured in settings must be configurable!")
                val entry = SettingsEntryEditor(context, p)
                val ed = entry.value as BidirectionalEditor<Any?>
                ed.setResult(value)
                ex.setEditor(entry)
                entries.addLast(ex)
            }
        }
    }

    internal val entries by child(SettingsEntryListEditor(context))

    private val expanderObservers = mutableMapOf<SettingsEntryExpander, Observer>()
    private val valueObservers = mutableMapOf<SettingsEntryEditor, Observer>()
    private val entryObserver: Observer

    override val result: ReactiveValidated<Bundle> = reactiveValue(
        valid(
            settings
        )
    )

    init {
        entryObserver = entries.editors.observeList { ch ->
            if (ch is Added) {
                expanderObservers[ch.added] = ch.added.editor.observe { _, old, new ->
                    if (old != null) {
                        valueObservers.remove(old)!!.kill()
                        val prop = old.property.property
                        if (bundle.hasProperty(prop)) bundle.delete(prop)
                    }
                    if (new != null) {
                        val prop = new.property.property
                        valueObservers[new] = new.value.result.forEach { r ->
                            if (r is Valid) bundle[prop] = r.value
                            else if (bundle.hasProperty(prop)) bundle.delete(prop)
                        }
                    }
                }
            }
            if (ch is Removed) {
                val e = ch.removed.editor.now
                if (e != null) {
                    valueObservers.remove(e)!!.kill()
                    val prop = e.property.property
                    if (bundle.hasProperty(prop)) bundle.delete(prop)
                }
                expanderObservers.remove(ch.removed)!!.kill()
            }
        }
    }
}