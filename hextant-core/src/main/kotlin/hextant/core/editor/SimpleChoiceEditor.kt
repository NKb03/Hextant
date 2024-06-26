package hextant.core.editor

import hextant.context.Context
import hextant.core.view.SimpleChoiceEditorView
import hextant.serial.Snapshot
import hextant.serial.json
import hextant.serial.loadClass
import hextant.serial.string
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import reaktive.value.ReactiveValue
import reaktive.value.now
import reaktive.value.reactiveVariable

abstract class SimpleChoiceEditor<C : Any>(context: Context, default: C) :
    AbstractEditor<C, SimpleChoiceEditorView<C>>(context), ComboBoxSource<C> {

    private val _selected = reactiveVariable(default)

    override val result: ReactiveValue<C> = _selected

    /**
     * Select the given [choice]
     */
    override fun select(choice: C) {
        if (choice == result.now) return
        _selected.set(choice)
        notifyViews { selected(choice) }
    }

    override fun toString(choice: C): String = choice.toString()

    override fun fromString(str: String): C? = null

    abstract override fun choices(): List<C>

    override fun createSnapshot(): Snapshot<*> = Snap<C>()

    override fun viewAdded(view: SimpleChoiceEditorView<C>) {
        view.selected(_selected.now)
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    private class Snap<C : Any> : Snapshot<SimpleChoiceEditor<C>>() {
        private lateinit var selected: C

        override fun doRecord(original: SimpleChoiceEditor<C>) {
            selected = original.result.now
        }

        override fun reconstructObject(original: SimpleChoiceEditor<C>) {
            original._selected.now = selected
        }

        override fun encode(builder: JsonObjectBuilder) {
            builder.put("classOfSelected", this.selected.javaClass.name)
            val serializer = selected::class.serializer() as KSerializer<C>
            builder.put("selected", json.encodeToJsonElement(serializer, this.selected))
        }

        override fun decode(element: JsonObject) {
            val classOfSelected = element.getValue("classOfSelected").string
            val clazz = classOfSelected.loadClass().kotlin
            val serializer = clazz.serializer() as KSerializer<C>
            selected = json.decodeFromJsonElement(serializer, element.getValue("selected"))
        }
    }
}