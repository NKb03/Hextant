/**
 *@author Nikolaus Knop
 */

package hextant.blocky.view

import bundles.Bundle
import hextant.codegen.ProvideImplementation
import hextant.context.ControlFactory
import hextant.core.view.CompoundEditorControl

class UnaryExpressionEditorControl @ProvideImplementation(ControlFactory::class) constructor(
    editor: hextant.blocky.editor.UnaryExpressionEditor, arguments: Bundle
) : CompoundEditorControl(editor, arguments, {
    line {
        spacing = 2.0
        view(editor.op)
        view(editor.operand)
    }
})