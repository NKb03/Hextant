/**
 *@author Nikolaus Knop
 */

package hextant.command.line

import bundles.Bundle
import bundles.createBundle
import hextant.context.Context
import hextant.fx.HextantPopup

/**
 * A [HextantPopup] that shows a [CommandLine]
 */
class CommandLinePopup(
    context: Context,
    commandLine: CommandLine,
    arguments: Bundle = createBundle(),
    minWidth: Double = 300.0
) : HextantPopup(context) {
    private val autoHide = commandLine.executedCommand.observe { _, _ -> hide() }
    private val view = CommandLineControl(commandLine, arguments)

    init {
        view.minWidth = minWidth
        scene.root = view
    }

    override fun show() {
        super.show()
        view.receiveFocus()
    }
}