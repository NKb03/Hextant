/**
 *@author Nikolaus Knop
 */

package hextant.project.editor

import hextant.context.Context
import hextant.core.Editor
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import hextant.project.File
import hextant.serial.*
import hextant.serial.SerialProperties.projectRoot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import reaktive.Observer
import reaktive.event.event
import reaktive.value.ReactiveValue
import reaktive.value.reactiveVariable

internal class FileEditor<R> private constructor(context: Context) : CompoundEditor<File<R>?>(context),
                                                                     ProjectItemEditor<R, File<R>> {
    private lateinit var id: String
    private lateinit var path: java.io.File
    private lateinit var content: VirtualFile<Editor<R>>

    override val itemName by child(FileNameEditor(context))

    private val _result = reactiveVariable<File<R>?>(null)

    private var obs: Observer? = null
    private lateinit var observer: Observer

    private val rootEditorChange = event<Editor<R>>()
    internal val rootEditorChanged get() = rootEditorChange.stream
    internal val rootEditor get() = content.get()


    override fun deletePhysical() {
        context[FileManager].deleteFile(path)
    }

    private fun bindResult() {
        updateEditor(content.get())
        observer = content.read.observe { _, e ->
            obs?.kill()
            updateEditor(e)
            rootEditorChange.fire(e)
        }
    }

    private class Snap<R> : Snapshot<FileEditor<R>>() {
        private lateinit var id: String
        private lateinit var itemName: Snapshot<FileNameEditor>

        override fun doRecord(original: FileEditor<R>) {
            original.content.write()
            id = original.id
            itemName = original.itemName.snapshot()
        }

        override fun reconstructObject(original: FileEditor<R>) {
            itemName.reconstructObject(original.itemName)
            original.id = id
            original.path = original.context[projectRoot].resolve(id)
            original.content = original.context[FileManager].from(original.path, original.context)
            original.bindResult()
        }

        override fun encode(builder: JsonObjectBuilder) {
            builder.put("id", this.id)
            builder.put("itemName", this.itemName.encodeToJson())
        }

        override fun decode(element: JsonObject) {
            id = element.getValue("id").string
            itemName = decodeFromJson<FileNameEditor>(element.getValue("itemName"))
        }
    }

    @Suppress("DEPRECATION")
    private fun updateEditor(e: Editor<R>) {
        obs = _result.bind(composeResult<File<R>?>(itemName, e, default = { null }) { File(itemName.now, e.now) })
        e.setFile(content)
    }

    override fun createSnapshot(): Snapshot<*> = Snap<R>()

    override fun supportsCopyPaste(): Boolean = true

    override val result: ReactiveValue<File<R>?> get() = _result

    companion object {
        fun <R> newInstance(context: Context): FileEditor<R> {
            val e = FileEditor<R>(context)
            e.id = context[IdGenerator].generateID()
            e.path = context[projectRoot].resolve(e.id)
            e.content = context[FileManager].get(RootExpander(context), e.path)
            e.content.write()
            e.bindResult()
            return e
        }
    }
}