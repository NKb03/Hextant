/**
 *@author Nikolaus Knop
 */

package hextant.blocky.editor

import hextant.blocky.*
import hextant.core.editor.ExpanderConfigurator

object ExprExpanderDelegator : ExpanderConfigurator<ExprEditor<Expr>>({
    registerInterceptor { text, context -> IntLiteral.wrap(text).orNull().let { IntLiteralEditor(context, text) } }
    registerInterceptor { text, context ->
        Id.wrap(text).orNull().let { RefEditor(context).apply { id.setText(text) } }
    }
    registerInterceptor { text, context ->
        BinaryOperator.wrap(text).orNull().let { operator ->
            BinaryExpressionEditor(context).apply { op.setText(operator.toString()) }
        }
    }
    registerInterceptor { text, context ->
        UnaryOperator.wrap(text).orNull().let { operator ->
            UnaryExpressionEditor(context).apply { op.setText(operator.toString()) }
        }
    }
})