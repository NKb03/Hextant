/**
 *@author Nikolaus Knop
 */

package hextant.codegen.editor

import hextant.codegen.*
import hextant.codegen.aspects.FeatureCollector
import krobot.api.*
import javax.lang.model.element.TypeElement

internal object ListEditorCodegen : EditorClassGen<EditableList, TypeElement>() {
    override fun process(element: TypeElement, annotation: EditableList) {
        val editorCls = getTypeMirror(annotation::editorCls).takeIf { it.toString() != None::class.qualifiedName }
        val editorClsName = editorCls?.toString() ?: getEditorClassName(element.asType())
        val simpleName = element.simpleName.toString()
        val nullable = isResultNullable(element.asType())
        val qn = extractQualifiedEditorClassName(annotation, element, classNameSuffix = "ListEditor")
        val (pkg, name) = splitPackageAndSimpleName(qn)
        val file = kotlinClass(
            pkg, {
                import(element.toString())
                import("hextant.core.editor.ListEditor")
                import(editorClsName)
                import("hextant.context.*")
            },
            name,
            primaryConstructor = { "context" of "Context" },
            inheritance = {
                extend(type("ListEditor").parameterizedBy {
                    if (nullable) invariant(type(simpleName).nullable())
                    else invariant(simpleName)
                    invariant(editorClsName)
                }, "context".e)
            }
        ) {
            addConstructor(
                {
                    "context" of "Context"
                    "vararg editors" of editorClsName
                },
                "context".e
            ) {
                addFor("i", "editors".e select "indices") {
                    addVal("e") initializedWith "editors".e["i".e]
                    callFunction("addAt", {}, "i".e, "e".e)
                }
            }
            addSingleExprFunction("createEditor", { override() }) { call(editorClsName, annotation.childContext.e) }
        }
        writeKotlinFile(file)
        FeatureCollector.generatedEditor("$pkg.$name")
    }
}