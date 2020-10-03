/**
 * @author Nikolaus Knop
 */

package hextant.lispy.rt

import hextant.lispy.*

fun display(value: SExpr) = buildString { display(value) }

fun StringBuilder.display(value: SExpr) {
    when (value) {
        is Symbol -> append(value.name)
        is IntLiteral -> append(value.value)
        is BooleanLiteral -> if (value.value) append("#t") else append("#f")
        is Pair -> if (value.isList()) displayList(value) else displayPair(value)
        Nil -> append("()")
        is Quoted -> displayQuoted(value)
        is Procedure -> displayProcedure(value)
    }
}

private fun StringBuilder.displayProcedure(value: Procedure) {
    append('<')
    if (value.isMacro) append("macro") else append("procedure")
    append(' ')
    if (value.name != null) {
        append(value.name)
        append(' ')
    }
    append("#${value.arity}")
    append('>')
}

private fun StringBuilder.displayQuoted(value: Quoted) {
    append("'")
    display(value.expr)
}

private fun StringBuilder.displayPair(value: Pair) {
    append('(')
    display(value.car)
    append(" . ")
    display(value.cdr)
    append(')')
}

private fun StringBuilder.displayList(lst: Pair) {
    append('(')
    var l: Pair = lst
    while (true) {
        display(l.car)
        val nxt = l.cdr
        if (nxt is Pair) {
            l = nxt
            append(' ')
        } else {
            append(')')
            break
        }
    }
}