/**
 * @author Nikolaus Knop
 */

package hextant.lisp.rt

import hextant.lisp.*

fun SExpr.isPair(): Boolean = this is Pair || this is Quotation && quoted.isPair()

val SExpr.car: SExpr
    get() = when (this) {
        is Pair -> car
        is Quotation -> quote(quoted.car)
        else               -> fail("${display(this)} is not a pair")
    }

val SExpr.cdr: SExpr
    get() = when (this) {
        is Pair -> cdr
        is Quotation -> quote(quoted.cdr)
        else               -> fail("${display(this)} is not a pair")
    }

fun SExpr.isNil(): Boolean = this is Nil || this is Quotation && quoted.isNil()

fun SExpr.isList(): Boolean = isNil() || (isPair() && cdr.isList())

fun SExpr.extractList(): List<SExpr> {
    val args = mutableListOf<SExpr>()
    var lst = this
    while (!lst.isNil()) {
        args.add(lst.car)
        lst = lst.cdr
    }
    return args
}

fun SExpr.unquote() = if (this is Quotation) quoted else fail("$this cannot be unquoted")

fun SExpr.symbolList() = extractList()
    .map { it as? Symbol ?: fail("expected symbol but got $it") }
    .map { it.name }

fun truthy(condition: SExpr) = condition != f && condition != nil


