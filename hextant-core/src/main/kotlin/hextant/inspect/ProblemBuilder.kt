/**
 *@author Nikolaus Knop
 */

package hextant.inspect

import hextant.command.Builder

/**
 * Builder for a [Problem]
 */
@Builder
class ProblemBuilder @PublishedApi internal constructor() {
    /**
     * The severity of the built problem, defaults to [Severity.Warning]
     */
    var severity: Severity = Severity.Warning
    /**
     * The message of the built problem, must be specified
     */
    lateinit var message: String

    /**
     * The location that caused this problem
     */
    lateinit var location: Any

    private val fixes: MutableCollection<ProblemFix> = mutableSetOf()

    /**
     * Add a possible problem-[fix]
     */
    fun addFix(fix: ProblemFix) {
        fixes.add(fix)
    }

    /**
     * Add multiple possible problem-[fixes]
     */
    fun addFixes(fixes: Collection<ProblemFix>) {
        this.fixes.addAll(fixes)
    }

    @PublishedApi internal fun build(): Problem {
        return Problem.of(severity, message, fixes)
    }
}