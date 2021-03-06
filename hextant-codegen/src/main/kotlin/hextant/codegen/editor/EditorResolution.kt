/**
 *@author Nikolaus Knop
 */

package hextant.codegen.editor

import javax.lang.model.element.TypeElement

data class EditorResolution(
    val className: String,
    private val resultNullable: () -> Boolean
) {
    constructor(className: String, nullable: Boolean) : this(className, { nullable })

    val isResultNullable by lazy(resultNullable)

    companion object {
        private val map = mutableMapOf<TypeElement, EditorResolution>()

        fun register(resultType: TypeElement, className: String, nullable: () -> Boolean) {
            map[resultType] = EditorResolution(className, nullable)
        }

        fun resolve(resultType: TypeElement): EditorResolution? = map[resultType]
    }
}