/**
 * @author Nikolaus Knop
 */

package hextant.serial

import hextant.serial.Snapshot.Companion.decode
import kotlinx.serialization.json.*
import java.lang.reflect.Constructor

/**
 * Used to save the state of an object at given point of time.
 * Snapshots can be used to reduce the memory impact of saving an object for later.
 * @param Original the type of the original object.
 */
abstract class Snapshot<Original : Any> {
    private var clazz: String? = null

    /**
     * Save a minimal representation of the given [original] to this snapshot.
     * @param recordClass whether the runtime class of the original object should be stored for later reconstruction.
     */
    fun record(original: Original, recordClass: Boolean) {
        if (recordClass) {
            clazz = original.javaClass.name
        }
        doRecord(original)
    }

    /**
     * Save a minimal representation of the given [original] to this snapshot.
     */
    protected abstract fun doRecord(original: Original)

    /**
     * Reconstruct the last original object that was [record]ed to this snapshot.
     */
    abstract fun reconstruct(original: Original)

    /**
     * Reconstruct the original object constructing it by supplying the given [constructorArguments] to the constructor.
     *
     * Note that the runtime classes of the [constructorArguments] must exactly match the types of the constructor
     * that should be used.
     */
    fun reconstruct(
        vararg constructorArguments: Any,
        getConstructor: (Class<Original>) -> Constructor<Original>
    ): Original {
        check(clazz != null) { "clazz is null: cannot create instance" }
        @Suppress("UNCHECKED_CAST")
        val cls = clazz!!.loadClass() as Class<Original>
        val cstr = getConstructor(cls)
        val instance = cstr.newInstance(*constructorArguments) as Original
        reconstruct(instance)
        return instance
    }

    /**
     * Serialize this snapshot as a JSON element.
     */
    protected abstract fun JsonObjectBuilder.encode()

    /**
     * Serialize this snapshot as a JSON element.
     */
    fun encode(): JsonElement = buildJsonObject {
        put("_type", this@Snapshot.javaClass.name)
        if (clazz != null) put("_class", clazz)
        encode()
    }

    /**
     * Read in a serialized snapshot represented as a JSON element.
     */
    protected abstract fun decode(element: JsonObject)

    companion object {
        /**
         * Decodes the given [element], which has been encoded with the [Snapshot.encode] function, as a [Snapshot].
         */
        fun decode(element: JsonElement): Snapshot<*> {
            require(element is JsonObject) { "Expected json object but got $element" }
            val snapshotType = element.getValue("_type").string
            val cls = snapshotType.loadClass()
            val cstr = cls.getConstructor()
            cstr.isAccessible = true
            val snapshot = cstr.newInstance()
            require(snapshot is Snapshot<*>) { "$snapshotType is not a subtype of hextant.serial.Snapshot" }
            val clazz = element["_class"]?.string
            if (clazz != null) snapshot.clazz = clazz
            snapshot.decode(element)
            return snapshot
        }

        /**
         * Syntactic sugar for [decode].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmName("decodeTypesafe")
        fun <O : Any> decode(element: JsonElement): Snapshot<O> = decode(element) as Snapshot<O>
    }
}