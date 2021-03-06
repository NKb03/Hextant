/**
 * @author Nikolaus Knop
 */

package hextant.lisp.editor

import hextant.command.command
import hextant.context.Context
import hextant.context.withoutUndo
import hextant.core.editor.replaceWith
import hextant.lisp.*
import hextant.lisp.rt.display
import hextant.lisp.rt.extractList
import hextant.lisp.rt.fail
import hextant.lisp.rt.isList
import hextant.plugins.PluginBuilder
import hextant.plugins.PluginBuilder.Phase.Disable
import hextant.plugins.PluginBuilder.Phase.Initialize
import hextant.plugins.registerInspection
import reaktive.list.binding.first
import reaktive.value.binding.binding
import reaktive.value.binding.equalTo
import reaktive.value.binding.flatMap
import reaktive.value.now
import reaktive.value.reactiveValue

fun SExpr.reconstructEditor(context: Context): SExprExpander = SExprExpander(context).also { e -> e.reconstruct(this) }

fun SExprExpander.reconstruct(expr: SExpr) {
    when (expr) {
        is Scalar -> {
            if (isExpanded.now) reset()
            setText(display(expr))
        }
        else -> expand(context.withoutUndo { reconstruct(expr, context) })
    }
}

private fun reconstruct(expr: SExpr, context: Context): SExprEditor<*> = when (expr) {
    is Pair -> run {
        assert(expr.isList()) { "Can't reconstruct expression ${(display(expr))}" }
        val exprs = expr.extractList()
        if (exprs.isNotEmpty() && exprs[0] is Symbol) {
            val name = (exprs[0] as Symbol).name
            val syntax = SpecialSyntax.get(name)
            if (syntax != null && syntax.represents(exprs)) {
                return syntax.represent(context, exprs)
            }
        }
        CallExprEditor(context).apply {
            for (ex in exprs) {
                expressions.addLast(ex.reconstructEditor(context))
            }
        }
    }
    is Nil -> CallExprEditor(context)
    is Quotation -> QuotationEditor(context).apply {
        quoted.reconstruct(expr.quoted)
    }
    is QuasiQuotation -> QuasiQuotationEditor(context).apply {
        quoted.reconstruct(expr.quoted)
    }
    is Unquote -> UnquoteEditor(context).also { e ->
        e.expr.reconstruct(expr.expr)
    }
    is Closure -> LambdaEditor(context).apply {
        for (param in expr.parameters) {
            parameters.addLast(SymbolEditor(context, param))
        }
        body.reconstruct(expr.body)
    }
    else -> fail("Can't reconstruct expression ${display(expr)}")
}

val beautify = command<CallExprEditor, Unit> {
    name = "Beautify Expression"
    shortName = "beautify"
    description = "Replaces a common S-Expr with a special syntax"
    defaultShortcut("Ctrl?+B")
    applicableIf { e ->
        val sym = e.expressions.results.now.firstOrNull() as? Symbol ?: return@applicableIf false
        val syntax = SpecialSyntax.get(sym.name) ?: return@applicableIf false
        val editors = e.expressions.editors.now.map { it.editor.now }
        syntax.arity + 1 == editors.size && syntax.representsEditors(editors)
    }
    executing { e, _ ->
        val sym = e.expressions.results.now[0] as Symbol
        val syntax = SpecialSyntax.get(sym.name)!!
        val editors = e.expressions.editors.now.map { it.editor.now }
        val special = syntax.representEditors(e.context, editors)
        e.replaceWith(special)
    }
}

fun PluginBuilder.addSpecialSyntax(syntax: SpecialSyntax<*>) {
    on(Initialize) {
        SpecialSyntax.register(syntax)
        SExprExpander.config.registerKey(syntax.name) { context -> syntax.createTemplate(context) }
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
            val rightName = inspected.expressions.editors.first()
                .flatMap { it?.result ?: reactiveValue(null) }
                .equalTo(Symbol(syntax.name))
            rightName.flatMap { right ->
                if (!right) reactiveValue(false)
                else binding { syntax.representsEditors(inspected.expressions.editors()) }
            }
        }
        addFix("Replace with ${syntax.name}-form", beautify)
    }
}