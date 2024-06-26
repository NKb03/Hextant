import ExprPlugin.color
import bundles.publicProperty
import bundles.set
import hextant.command.Command.Type.SingleReceiver
import hextant.command.executingCompoundEdit
import hextant.context.EditorControlGroup
import hextant.core.editor.ColorEditor
import hextant.core.view.AbstractTokenEditorControl
import hextant.core.view.TokenEditorControl
import hextant.expr.IntLiteral
import hextant.expr.Operator
import hextant.expr.Operator.Plus
import hextant.expr.editor.*
import hextant.expr.view.Style
import hextant.fx.WindowSize
import hextant.fx.runFXWithTimeout
import hextant.plugins.*
import hextant.plugins.PluginBuilder.Phase.Initialize
import hextant.undo.compoundEdit
import javafx.scene.paint.Color
import reaktive.value.binding.and
import reaktive.value.binding.impl.notNull
import reaktive.value.binding.map
import reaktive.value.now

object ExprPlugin : PluginInitializer({
    registerCommand<ExprEditor<*>, Int> {
        name = "Evaluate Expression"
        shortName = "eval"
        applicableIf { exprEditor -> exprEditor.result.now != null }
        description = "Evaluates the selected expression and prints it to the console"
        executing { editor, _ ->
            val e = editor.result.now!!
            val v = e.value
            v
        }
    }
    registerCommand<OperatorEditor, Unit> {
        name = "Flip operands"
        shortName = "flip_op"
        description = "Flips the both operands in this operator application"
        type = SingleReceiver
        applicableIf { oe ->
            val oae = oe.parent as? OperatorApplicationEditor ?: return@applicableIf false
            oae.operator.result.now?.isCommutative ?: false
        }
        executingCompoundEdit { oe, _ ->
            val oae = oe.parent as OperatorApplicationEditor
            val expander1 = oae.operand1
            val editableOp1 = expander1.editor.now
            val expander2 = oae.operand2
            val editableOp2 = expander2.editor.now
            if (editableOp2 != null) expander1.expand(editableOp2)
            if (editableOp1 != null) expander2.expand(editableOp1)
        }
    }
    registerCommand<OperatorApplicationEditor, Unit> {
        name = "Collapse expression"
        shortName = "collapse"
        description = "Partially evaluate the selected expression"
        applicableIf { oae ->
            oae.result.now != null && oae.expander != null
        }
        executingCompoundEdit { oae, _ ->
            val ex = oae.expander as ExprExpander
            val res = oae.result.now!!.value
            val editable = IntLiteralEditor(oae.context, res.toString())
            ex.expand(editable)
        }
    }
    registerCommand<ExprEditor<*>, Unit> {
        name = "Unwrap expression"
        shortName = "unwrap"
        description = "Unwrap an expression by replacing its outer application with itself"
        applicableIf {
            it.parent is OperatorApplicationEditor && it.parent!!.expander is ExprExpander
        }
        executingCompoundEdit { editor, _ ->
            val parentExpander = editor.parent!!.expander as ExprExpander
            parentExpander.expand(editor)
        }
    }
    registerInspection<IntLiteralEditor> {
        id = "int-literal.syntax"
        description = "Reports invalid integer literals"
        isSevere(true)
        checkingThat { inspected.result.notNull() }
        message { "'${inspected.text.now}' is not a valid integer literal" }
    }
    registerInspection<OperatorEditor> {
        id = "operator.syntax"
        description = "Reports invalid operators"
        isSevere(true)
        checkingThat { inspected.result.notNull() }
        message { "'${inspected.text.now}' is not a valid operator" }
    }
    registerInspection<OperatorApplicationEditor> {
        id = "identical"
        description = "Prevent identical operations"
        isSevere(false)
        location { inspected.operator }
        preventingThat {
            val operandIsZero = inspected.operand2.result.map { it is IntLiteral && it.value == 0 }
            val operatorIsPlus = inspected.operator.result.map { it == Plus }
            operatorIsPlus.and(operandIsZero)
        }
        message { "Operation doesn't change the result" }
        addFix {
            description = "Shorten expression"
            applicableIf {
                inspected.expander is ExprExpander
            }
            fixingBy {
                val expander = inspected.expander as ExprExpander
                expander.expand(inspected.operand1)
            }
        }
    }
    registerCommand<ExprExpander, Unit> {
        description =
            "Wraps the current expression in an binary expression with the current expression being the left operand"
        name = "Wrap in binary expression"
        shortName = "wrap"
        defaultShortcut("Ctrl+W")
        applicableIf { it.isExpanded.now }
        val op = addParameter<Operator> {
            name = "operator"
        }
        executing { expander, args ->
            expander.context.compoundEdit("Wrap in binary expression") {
                val editor = expander.editor.now!!
                val app = OperatorApplicationEditor(editor.context)
                expander.expand(app)
                app.operator.setText(args[op].name)
                app.operand1.expand(editor)
                runFXWithTimeout {
                    expander.context[EditorControlGroup].getViewOf(app.operand2).receiveFocus()
                }
            }
        }
    }
    observeProperty<AbstractTokenEditorControl, Color>(color) { control, c ->
        control.root.style = c.let { "-fx-text-fill: $c" }
    }
    registerCommand<TokenEditorControl, Unit> {
        description = "Sets the text fill"
        name = "Set Color"
        shortName = "color"
        defaultShortcut("Ctrl+F")
        val c = addParameter<Color> {
            editWith(::ColorEditor)
            name = "color"
            description = "The text fill"
        }
        executing { v, args -> v.arguments[color] = args[c] }
    }
    configurableProperty(Style.BorderColor) { ctx -> ColorEditor(ctx) }
    resultStyleClass<IntLiteral> { "int-literal" }
    stylesheet("expr.css")
    on(Initialize) { context ->
        context[WindowSize] = WindowSize.FitContent
    }
}) {
    val color = publicProperty<Color>("color")
}