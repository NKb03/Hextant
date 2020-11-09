/**
 *@author Nikolaus Knop
 */

package hextant.core.view

import bundles.Bundle
import hextant.codegen.ProvideImplementation
import hextant.context.ControlFactory
import hextant.core.editor.ColorEditor
import reaktive.value.binding.map
import reaktive.value.fx.asObservableValue

internal class ColorEditorControl @ProvideImplementation(ControlFactory::class) constructor(
    editor: ColorEditor,
    arguments: Bundle
) : TokenEditorControl(editor, arguments) {
    init {
        val style = editor.result.map { c: String? -> c?.let { "-fx-text-fill: ${it.toCSSColor()}" } }
        root.styleProperty().bind(style.asObservableValue())
    }

    private fun String.toCSSColor() = replace("0x", "#")
}