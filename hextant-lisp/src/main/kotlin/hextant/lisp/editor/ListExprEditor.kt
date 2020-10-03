/**
 *@author Nikolaus Knop
 */

package hextant.lisp.editor

import hextant.codegen.ProvideFeature
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.lisp.SExpr
import hextant.lisp.list
import validated.reaktive.ReactiveValidated
import validated.reaktive.mapValidated

@ProvideFeature
class ListExprEditor(context: Context) : CompoundEditor<SExpr>(context), SExprEditor<SExpr> {
    val expressions by child(SExprListEditor(context))

    override val result: ReactiveValidated<SExpr> = expressions.result.mapValidated { exprs -> list(exprs) }
}