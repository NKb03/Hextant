import hextant.Context
import hextant.completion.*
import hextant.core.view.FXExpanderView
import hextant.core.view.ListEditorControl
import hextant.core.view.ListEditorControl.Orientation.Horizontal
import hextant.expr.Expr
import hextant.expr.editor.*
import hextant.plugin.dsl.PluginInitializer
import org.controlsfx.glyphfont.FontAwesome
import validated.valid

object ExprPlugin : PluginInitializer({
    author = "Nikolaus Knop"
    name = "Hextant Expressions"
    defaultEditor(::IntLiteralEditor)
    defaultEditor(::OperatorEditor)
    defaultEditor(::OperatorApplicationEditor)
    defaultEditor(::SumEditor)
    defaultEditor(::ExprExpander)
    defaultEditor(::ExprListEditor)
    compoundView { e: OperatorApplicationEditor ->
        line {
            operator("(")
            view(e.operand1)
            view(e.operator)
            view(e.operand2)
            operator(")")
        }
    }
    view { editor: ExprExpander, args ->
        val c = CompoundCompleter<Context, Any>()
        c.addCompleter(ExprExpander.config.completer(CompletionStrategy.simple))
        c.addCompleter(object : ConfiguredCompleter<Context, Int>(CompletionStrategy.simple) {
            override fun completionPool(context: Context): Collection<Int> = listOf(666, 42)
        })
        FXExpanderView(editor, args, c)
    }
    tokenEditorView<OperatorEditor>("operator")
    tokenEditorView<IntLiteralEditor>("decimal-editor")
    compoundView { e: SumEditor ->
        line {
            keyword("sum")
            space()
            view(e.expressions)
        }
    }
    view<ExprListEditor> { editor, args ->
        ListEditorControl.withAltGlyph(editor, FontAwesome.Glyph.PLUS, args, Horizontal).apply {
            cellFactory = { ListEditorControl.SeparatorCell(", ") }
        }
    }
    registerConversion<Expr, Int> { expr -> valid(expr.value) }
    stylesheet("expr.css")
})