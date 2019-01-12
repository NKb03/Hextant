/**
 *@author Nikolaus Knop
 */

package hextant.fx

import javafx.scene.control.Label

class OperatorLabel(operatorText: String) : Label(" $operatorText ") {
    init {
        styleClass.add("hextant-text")
        styleClass.add("operator")
    }
}