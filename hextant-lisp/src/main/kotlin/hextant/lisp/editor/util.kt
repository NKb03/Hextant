/**
 * @author Nikolaus Knop
 */

package hextant.lisp.editor

import hextant.command.command
import hextant.context.Context
import hextant.context.withoutUndo
import hextant.core.editor.replaceWith
import hextant.lisp.*
import hextant.lisp.rt.*
import hextant.plugin.PluginBuilder
import hextant.plugin.PluginBuilder.Phase.Disable
import hextant.plugin.PluginBuilder.Phase.Initialize
import hextant.plugin.registerInspection
import reaktive.list.ReactiveList
import reaktive.map
import reaktive.value.now
import validated.force
import validated.orNull

fun SExpr.reconstructEditor(context: Context): SExprExpander =
    SExprExpander(context, RuntimeScopeEditor(context, scope)).also { e -> e.reconstruct(this) }

fun SExprExpander.reconstruct(expr: SExpr) {
    withoutUndo { setEditor(reconstruct(expr, context, scope)) }
}

private fun reconstruct(expr: SExpr, context: Context, scope: RuntimeScopeEditor): SExprEditor {
    return when (expr) {
        is Symbol -> SymbolEditor(context, scope, expr.name)
        is IntLiteral -> LiteralEditor(context, scope, expr.toString())
        is BooleanLiteral -> LiteralEditor(context, scope, expr.toString())
        is Pair -> run {
            assert(expr.isList()) { "Can't reconstruct expression ${(display(expr))}" }
            val exprs = expr.extractList()
            if (exprs.isNotEmpty() && exprs[0] is Symbol) {
                val name = (exprs[0] as Symbol).name
                val syntax = SpecialSyntax.get(name)
                if (syntax != null && syntax.represents(exprs)) {
                    return syntax.represent(context, scope, exprs)
                }
            }
            CallExprEditor(context, scope).apply {
                for (ex in exprs) {
                    expressions.addLast(ex.reconstructEditor(context))
                }
            }
        }
        is Nil -> CallExprEditor(context, scope)
        is Quotation -> QuotationEditor(context, scope).apply {
            quoted.reconstruct(expr.quoted)
        }
        is QuasiQuotation -> QuasiQuotationEditor(context, scope).apply {
            quoted.reconstruct(expr.quoted)
        }
        is Unquote -> UnquoteEditor(context, scope).also { e ->
            e.expr.reconstruct(expr.expr)
        }
        is Closure -> LambdaEditor(context, scope).apply {
            for (param in expr.parameters) {
                parameters.addLast(SymbolEditor(context, scope, param))
            }
            body.reconstruct(expr.body)
        }
        is NormalizedSExpr -> NormalizedSExprEditor(context, scope).apply {
            this.expr.reconstruct(expr.expr)
        }
        else               -> fail("Can't reconstruct expression ${display(expr)}")
    }
}

val beautify = command<CallExprEditor, Unit> {
    name = "Beautify Expression"
    shortName = "beautify"
    description = "Replaces a common S-Expr with a special syntax"
    defaultShortcut("Ctrl?+B")
    applicableIf { e ->
        val sym = e.expressions.results.now.firstOrNull()?.orNull() as? Symbol ?: return@applicableIf false
        val syntax = SpecialSyntax.get(sym.name) ?: return@applicableIf false
        val editors = e.expressions.editors.now.map { it.editor.now }
        syntax.arity + 1 == editors.size && syntax.representsEditors(editors)
    }
    executing { e, _ ->
        val sym = e.expressions.results.now[0].force() as Symbol
        val syntax = SpecialSyntax.get(sym.name)!!
        val editors = e.expressions.editors.now.map { it.editor.now }
        val special = syntax.representEditors(e.context, e.scope, editors)
        e.replaceWith(special)
    }
}

fun PluginBuilder.addSpecialSyntax(syntax: SpecialSyntax<*>) {
    on(Initialize) {
        SpecialSyntax.register(syntax)
        SExprExpander.config.registerKey(syntax.name) { ex -> syntax.createTemplate(ex.context, ex.scope) }
    }
    on(Disable) {
        SpecialSyntax.unregister(syntax)
        SExprExpander.config.unregisterKey(syntax.name)
    }
    registerInspection<CallExprEditor> {
        id = "syntactic-sugar.${syntax.name}"
        isSevere(false)
        description = "Highlights expressions that can be beautified with a special syntax"
        message { "This expression can be replaced with syntactic sugar" }
        preventingThat {
            inspected.expressions.editors.map { expanders: ReactiveList<SExprExpander> ->
                val editors = expanders.now.map { it.editor.now }
                editors.firstOrNull()?.result?.now?.orNull() == Symbol(syntax.name) && syntax.representsEditors(editors)
            }
        }
        addFix("Replace with ${syntax.name}-form", beautify)
    }
}