/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core

import org.nikok.hextant.Context
import org.nikok.hextant.Editable
import org.nikok.hextant.bundle.Property
import org.nikok.hextant.core.CorePermissions.Internal
import org.nikok.hextant.core.CorePermissions.Public
import org.nikok.hextant.core.base.EditorControl
import org.nikok.hextant.core.editable.ConvertedEditable
import org.nikok.hextant.core.editable.Expandable
import org.nikok.hextant.core.impl.ClassMap
import org.nikok.hextant.core.view.FXExpanderView
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 * Used to manage the views of [Editable]s
 */
interface EditorViewFactory {
    /**
     * Register the specified [viewFactory] to the given [editableCls].
     * From now all calls of [getFXView] with an argument of type [E] will use the [viewFactory]
     */
    fun <E : Editable<*>> registerFX(editableCls: KClass<out E>, viewFactory: (E) -> EditorControl<*>)

    /**
     * @return the [EditorControl<*>] associated with the type of the specified [editable]
     * @throws NoSuchElementException if there is no [EditorControl<*>] registered with this [editable]
     */
    fun <E : Editable<*>> getFXView(editable: E): EditorControl<*>

    @Suppress("UNCHECKED_CAST") private class Impl(
        private val context: Context,
        private val classLoader: ClassLoader
    ) : EditorViewFactory {
        private val viewFactories = ClassMap.invariant<(Editable<*>) -> EditorControl<*>>()

        override fun <E : Editable<*>> registerFX(editableCls: KClass<out E>, viewFactory: (E) -> EditorControl<*>) {
            @Suppress("UNCHECKED_CAST") val cast = viewFactory as (Editable<*>) -> EditorControl<*>
            viewFactories[editableCls] = cast
        }

        override fun <E : Editable<*>> getFXView(editable: E): EditorControl<*> {
            val cls = editable::class
            when (editable) {
                is ConvertedEditable<*, *> -> return getFXView(editable.source)
                is Expandable<*, *>        -> return FXExpanderView(editable, context)
                else                       -> {
                    viewFactories[cls]?.let { f -> return f(editable) }
                    defaultFactory(cls)?.let { c -> return c(editable) }
                    unresolvedView(cls)
                }
            }
        }

        private fun <E : Editable<*>> unresolvedView(cls: KClass<out E>): Nothing {
            throw NoSuchElementException("Could not resolve view for $cls")
        }

        private fun defaultFactory(cls: KClass<out Editable<*>>): ((Editable<*>) -> EditorControl<*>)? {
            val viewCls = resolveDefault(cls) ?: return null
            val constructor = resolveConstructor(cls, viewCls)
            registerFX(cls, constructor)
            return constructor
        }

        private fun <E : Any> resolveConstructor(
            editableCls: KClass<out E>,
            viewCls: KClass<EditorControl<*>>
        ): (Editable<*>) -> EditorControl<*> {
            lateinit var contextParameter: KParameter
            lateinit var editableParameter: KParameter
            val constructor = viewCls.constructors.find { constructor ->
                val parameters = constructor.parameters
                contextParameter = parameters.find {
                    it.type.classifier == Context::class
                } ?: return@find false
                editableParameter = parameters.find {
                    it.type.isSupertypeOf(editableCls.starProjectedType)
                } ?: return@find false
                val otherParameters = parameters - setOf(contextParameter, editableParameter)
                otherParameters.count { !it.isOptional } == 0
            } ?: throw NoSuchElementException("Could not find constructor for $viewCls")
            return { expandable ->
                constructor.callBy(
                    mapOf(
                        editableParameter to expandable,
                        contextParameter to context
                    )
                )
            }

        }

        private fun resolveDefault(editableCls: KClass<*>): KClass<EditorControl<*>>? {
            val name = editableCls.simpleName ?: return null
            val pkg = editableCls.java.`package`?.name ?: return null
            if (!name.startsWith("Editable")) return null
            val viewClsName = "FX" + name.removePrefix("Editable") + "EditorView"
            val inSamePackage = "$pkg.$viewClsName"
            val inViewPackage = "$pkg.view.$viewClsName"
            val siblingViewPkg = pkg.replaceAfterLast('.', "view")
            val inSiblingViewPkg = "$siblingViewPkg.$viewClsName"
            return tryCreateViewCls(inSamePackage) ?: tryCreateViewCls(inViewPackage) ?: tryCreateViewCls(
                inSiblingViewPkg
            )
        }

        private fun tryCreateViewCls(name: String): KClass<EditorControl<*>>? {
            return try {
                val cls = classLoader.loadClass(name)
                val k = cls.kotlin
                k.takeIf { it.isSubclassOf(EditorControl::class) } as KClass<EditorControl<*>>?
            } catch (cnf: ClassNotFoundException) {
                null
            }
        }
    }

    companion object : Property<EditorViewFactory, Public, Internal>("editor-view-factory") {
        fun newInstance(context: Context, classLoader: ClassLoader): EditorViewFactory =
            Impl(context, classLoader)

        inline fun newInstance(
            context: Context,
            configure: EditorViewFactory.() -> Unit,
            classLoader: ClassLoader
        ): EditorViewFactory =
            newInstance(context, classLoader).apply(configure)
    }
}

inline fun <reified E : Editable<*>> EditorViewFactory.registerFX(noinline viewFactory: (E) -> EditorControl<*>) {
    registerFX(E::class, viewFactory)
}

inline fun EditorViewFactory.configure(config: EditorViewFactory.() -> Unit) {
    apply(config)
}