/**
 * @author Nikolaus Knop
 */

package org.nikok.hextant.core.completion

/**
 * Used to get [Completion]s
*/
interface Completer<T> {
    /**
     * @return the possible completions for [element] given a pool of possible completions [completionPool]
    */
    fun completions(element: String, completionPool: Set<T>): Set<Completion>
}