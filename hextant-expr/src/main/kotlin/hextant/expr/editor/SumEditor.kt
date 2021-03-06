/**
 *@author Nikolaus Knop
 */

package hextant.expr.editor

import hextant.codegen.ProvideFeature
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.expr.Sum
import reaktive.value.ReactiveValue

@ProvideFeature
class SumEditor(
    context: Context
) : CompoundEditor<Sum?>(context), ExprEditor<Sum> {
    val expressions by child(ExprListEditor(context))

    init {
        expressions.ensureNotEmpty()
    }

    override val result: ReactiveValue<Sum?> = composeResult { Sum(expressions.now) }
}