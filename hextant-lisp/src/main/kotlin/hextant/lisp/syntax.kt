/**
 * @author Nikolaus Knop
 */

package hextant.lisp

import hextant.codegen.*
import hextant.core.editor.TokenType
import hextant.core.editor.TokenTypeConfig
import hextant.lisp.editor.SExprEditor
import hextant.lisp.editor.SExprExpander
import hextant.lisp.rt.RuntimeScope
import hextant.lisp.rt.evaluate

@EditorInterface(SExprEditor::class)
@ListEditor
@UseEditor(SExprExpander::class)
sealed class SExpr

interface SelfEvaluating

sealed class Scalar : SExpr() {
    companion object : TokenTypeConfig<SExpr>({
        "#f" compilesTo BooleanLiteral(false)
        "#t" compilesTo BooleanLiteral(true)
        registerInterceptor { token -> IllegalScalar(token) }
        registerInterceptor { token ->
            token.takeIf { s ->
                s.isNotEmpty() && !s.first().isDigit() && s.none { c -> c.isWhitespace() }
            }?.let { s -> Symbol(s) }
        }
        registerInterceptor { token ->
            token.toIntOrNull()?.let { v -> IntLiteral(v) }
        }
        registerInterceptor { token ->
            if (token.startsWith("'"))
                Scalar.compile(token.drop(1))
                    .takeIf { it !is IllegalScalar }
                    ?.let(::Quotation)
            else null
        }
        registerInterceptor { token ->
            if (token.startsWith(","))
                Scalar.compile(token.drop(1))
                    .takeIf { it !is IllegalScalar }
                    ?.let(::Unquote)
            else null
        }
    })
}

data class IllegalScalar(val token: String) : Scalar() {
    override fun toString(): String = "<illegal: $token>"
}

sealed class Literal<T : Any> : SelfEvaluating, Scalar() {
    abstract val value: T
}

@Token(nodeType = SExpr::class)
@ListEditor
data class Symbol(val name: String) : Scalar() {
    override fun toString(): String = name

    companion object : TokenType<Symbol> {
        fun validate(name: String): String = when {
            name.isEmpty() -> "empty symbols are invalid"
            name.isBlank() -> "blank symbols are invalid"
            name.any { it.isWhitespace() } -> "symbol contains whitespace"
            name.any { it in "()[]'`," } -> "symbol contains illegal characters"
            else -> "ok"
        }

        override fun compile(token: String): Symbol = Symbol(token)
    }
}

data class IntLiteral(override val value: Int) : Literal<Int>() {
    override fun toString(): String = "$value"

    companion object : TokenType<IntLiteral?> {
        override fun compile(token: String): IntLiteral? = token.toIntOrNull()?.let(::IntLiteral)
    }
}

data class BooleanLiteral(override val value: Boolean) : Literal<Boolean>() {
    override fun toString(): String = "<boolean: $value>"

    companion object : TokenType<BooleanLiteral?> {
        override fun compile(token: String): BooleanLiteral? = when (token) {
            "#t" -> BooleanLiteral(true)
            "#f" -> BooleanLiteral(false)
            else -> null
        }
    }
}

data class Pair(var car: SExpr, var cdr: SExpr) : SExpr()

object Nil : Scalar() {
    override fun toString(): String = "Nil"
}

@Compound(nodeType = SExpr::class)
data class Quotation(val quoted: SExpr) : SExpr(), SelfEvaluating

@Compound(nodeType = SExpr::class)
data class QuasiQuotation(val quoted: SExpr) : SExpr()

@Compound(nodeType = SExpr::class)
data class Unquote(val expr: SExpr) : SExpr()

@Compound(nodeType = SExpr::class, register = false)
fun lambda(parameters: List<Symbol>, body: SExpr) = macroInvocation("lambda".s, listOf(list(parameters), body))

@Compound(nodeType = SExpr::class, register = false)
fun let(name: Symbol, value: SExpr, body: SExpr) = macroInvocation("let".s, listOf(name, value, body))

@Compound(nodeType = SExpr::class, register = false)
fun macroInvocation(macro: SExpr, arguments: List<SExpr>) =
    list("eval".s, list(macro, *arguments.map(::quote).toTypedArray()))

abstract class Procedure : SExpr(), SelfEvaluating {
    abstract val name: String?

    abstract val arity: Int

    abstract fun call(arguments: List<SExpr>, callerScope: RuntimeScope): SExpr
}

data class Builtin(
    override val name: String,
    override val arity: Int,
    private val def: (arguments: List<SExpr>, callerScope: RuntimeScope) -> SExpr
) : Procedure() {
    override fun call(arguments: List<SExpr>, callerScope: RuntimeScope): SExpr = def(arguments, callerScope)
}

data class Closure(
    override val name: String?,
    val parameters: List<String>,
    val body: SExpr,
    val closureScope: RuntimeScope
) : Procedure() {
    override val arity: Int
        get() = parameters.size

    override fun call(arguments: List<SExpr>, callerScope: RuntimeScope): SExpr {
        val callEnv = closureScope.child()
        for ((name, value) in parameters.zip(arguments)) callEnv.define(name, value)
        return body.evaluate(callEnv)
    }
}