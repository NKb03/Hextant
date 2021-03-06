/**
 *@author Nikolaus Knop
 */

package hextant.command

import hextant.config.Feature
import hextant.config.FeatureType
import hextant.context.Context
import hextant.context.EditorFactory
import hextant.core.Editor
import hextant.fx.Shortcut
import hextant.serial.getConstructor
import kotlin.reflect.KClass

/**
 * A Command that is executable on a receiver of type [R]
 */
interface Command<in R : Any, out T : Any> : Feature {
    /**
     * Execute this command on [receiver] with the specified [arguments]
     */
    fun execute(receiver: R, arguments: List<Any>): T

    /**
     * @return the short name of this [Command]
     * * This should be a short "typeable" name because it is used in the command shell
     * * For example: refactor
     * * If [shortName] returns `null` this [Command] cannot be used from the command line
     */
    val shortName: String?

    /**
     * @return the name of this [Command]
     * * It should be a imperative description of the action this command executes
     * * For example: 'Refactor this Method'
     */
    val name: String

    /**
     * @return he category of this [Command]
     * * This is used to place the menu item in the right menu of the menu bar
     * * If [category] returns `null` this [Command] can't be used from the Menu bar
     */
    val category: Category?

    /**
     * The shortcut that triggers this command or `null` if the command has no shortcut.
     */
    val shortcut: Shortcut?

    /**
     * The parameters of this [Command]
     */
    val parameters: List<Parameter<*>>

    /**
     * @return the description of this [Command]
     * * It should explain what this command does
     */
    override val description: String

    /**
     * The class of the receiver used for checking the type when executing
     */
    val receiverClass: KClass<in R>

    /**
     * The [Type] of this command
     */
    val commandType: Type

    /**
     * @return whether this [Command] can be executed on the specified [receiver]
     */
    fun isApplicableOn(receiver: Any): Boolean

    /**
     * A command category corresponds to a menu of shortcuts.
     * @property name the name of the command category
     */
    class Category private constructor(val name: String) {
        companion object {
            private val cache = mutableMapOf<String, Category>()

            /**
             * @return a possibly cached [Category] with the specified [name]
             */
            fun withName(name: String) = cache.getOrPut(name) { Category(name) }

            /**
             * The file menu
             */
            val FILE = withName("File")

            /**
             * The edit menu
             */
            val EDIT = withName("Edit")

            /**
             * The view menu
             */
            val VIEW = withName("View")
        }
    }

    /**
     * Specifies whether a command is applicable to multiple targets at once or only to one at a time
     */
    enum class Type {
        /**
         * Indicates that the command is applicable only on one receiver at a time.
         */
        SingleReceiver,

        /**
         * Indicates that the command is applicable on multiple receivers at a time.
         */
        MultipleReceivers
    }

    /**
     * A Parameter of a [Command]
     * @property name the name of this parameter
     * @property type the expected type for this parameter
     * @property description explains what this parameter is used for
     * @property editWith if not `null` this class of editor is used for editing the value of this parameter
     */
    data class Parameter<T : Any>(
        val name: String,
        val type: KClass<T>,
        val description: String,
        val editWith: EditorFactory<*>?
    ) {
        override fun toString() = buildString {
            append(name)
            append(": ")
            append(type.toString())
            appendLine()
            append(description)
        }
    }

    /**
     * A builder for [Parameter]s.
     * @param type the type of the parameter
     */
    @Builder
    class ParameterBuilder<T : Any> @PublishedApi internal constructor(val type: KClass<T>) {
        /**
         * The name of the parameter
         */
        lateinit var name: String

        /**
         * The description of this parameter, an explanation of what the parameter influences.
         * The default description is "No description provided"
         */
        var description: String = "No description provided"

        private var editorFactory: EditorFactory<T>? = null

        /**
         * This will cause command lines to use the given [factory] to create an editor for the value of this parameter.
         */
        fun editWith(factory: EditorFactory<T>) {
            editorFactory = factory
        }

        /**
         * Sets the [Parameter.editWith] to the given [clazz].
         */
        fun editWith(clazz: KClass<out Editor<T>>) {
            editWith(clazz.getConstructor(Context::class))
        }

        /**
         * Syntactic sugar for `editWith(T::class)`
         */
        inline fun <reified E : Editor<T>> editWith() {
            editWith(E::class)
        }

        @PublishedApi internal fun build(): Parameter<T> = Parameter(name, type, description, editorFactory)
    }

    companion object: FeatureType<Command<*, *>>("Command") {
        override fun onEnable(feature: Command<*, *>, context: Context) {
            super.onEnable(feature, context)
            context[Commands].register(feature)
        }

        override fun onDisable(feature: Command<*, *>, context: Context) {
            super.onDisable(feature, context)
            context[Commands].unregister(feature)
        }
    }
}