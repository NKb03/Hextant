/**
 *@author Nikolaus Knop
 */

package hextant.core.completion

import javafx.scene.control.Control

/**
 * A completion
 */
interface Completion<out T> {
    /**
     * The object that was completed
     */
    val completed: T

    /**
     * @return the completed text
     * for "hel" it could be "helloworld"
     */
    val text: String

    /**
     * @return a [Control] showing this completion
     */
    fun createNode(): Control
}