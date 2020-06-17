/**
 *@author Nikolaus Knop
 */

package hextant.command

import com.nhaarman.mockitokotlin2.*
import hextant.*
import hextant.command.line.*
import hextant.command.line.CommandLine.HistoryItem
import hextant.command.line.CommandReceiverType.Targets
import hextant.expr.IntLiteral
import hextant.expr.editor.IntLiteralEditor
import hextant.test.testingContext
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*

internal object CommandLineSpec : Spek({
    given("a command line") {
        val context = testingContext {
            get(EditorFactory).register { context -> IntLiteralEditor(context) }
        }
        val command = command<Target, Unit> {
            description = "command"
            shortName = "command"
            name = "command"
            executing { t, _ -> t.execute() }
        }
        val command2 = command<Target, Unit> {
            description = "command with parameter"
            shortName = "command2"
            name = "command2"
            addParameter {
                description = "the parameter"
                this.name = "x"
                ofType<IntLiteral>()
            }
            executing { target, (x) ->
                x as IntLiteral
                target.execute(x.value)
            }
        }
        val commands = Commands.newInstance()
        commands.register(command)
        commands.register(command2)
        val t = mock<Target> { on { it.isApplicable }.then { true } }
        val v = mock<EditorView> { on { target }.then { t } }
        lateinit var intEditor: IntLiteralEditor
        val distributor = SelectionDistributor.newInstance()
        distributor.select(v)
        val cl = CommandLine(context, ContextCommandSource(distributor, commands, Targets))
        val view = mock<CommandLineView> {
            on { expanded(any(), any()) }.then {
                if (it.getArgument<Command<*, *>>(0).shortName == "command2") {
                    val editors = it.getArgument<List<Editor<*>>>(1)
                    intEditor = editors[0] as IntLiteralEditor
                }
            }
        }
        inOrder(view, t) {
            on("adding a view") {
                cl.addView(view)
                it("should display the text") {
                    verify(view).displayCommandName("")
                }
            }
            on("editing the name") {
                cl.setCommandName("new")
                it("should display the text") {
                    verify(view).displayCommandName("new")
                }
            }
            on("trying to execute") {
                cl.execute()
                it("should do nothing") {
                    verifyNoMoreInteractions()
                }
            }
            on("setting the text to a command without arguments") {
                cl.setCommandName("command")
                it("should display the text") {
                    verify(view).displayCommandName("command")
                }
            }
            on("executing the no-arg command") {
                cl.execute()
                it("should execute the command") {
                    verify(t).execute()
                }
                it("should notify the views about the executed command") {
                    verify(view).addToHistory(HistoryItem(command, emptyList(), Unit))
                }
                it("should reset") {
                    verify(view).reset()
                }
            }
            on("setting the text to a command with arguments") {
                cl.setCommandName("command2")
                it("should display the name") {
                    verify(view).displayCommandName("command2")
                }
            }
            on("expanding") {
                cl.expand()
                it("should expand") {
                    verify(view).expanded(eq(command2), any())
                }
            }
            on("executing when int editor is not ok") {
                cl.execute()
                it("should do nothing") {
                    verifyNoMoreInteractions()
                }
            }
            on("executing after making int editor valid") {
                intEditor.setText("123")
                cl.execute()
                it("should execute the command") {
                    verify(t).execute(123)
                }
                it("should notify the views about the executed command") {
                    verify(view).addToHistory(any())
                }
            }
            on("expanding and then resetting") {
                cl.setCommandName("command2")
                cl.expand()
                cl.reset()
                it("should edit the name, expand and reset") {
                    verify(view).displayCommandName("command2")
                    verify(view).expanded(eq(command2), any())
                    verify(view).reset()
                }
            }
        }
    }
}) {
    interface Target {
        val isApplicable: Boolean

        fun execute()

        fun execute(x: Int)
    }
}