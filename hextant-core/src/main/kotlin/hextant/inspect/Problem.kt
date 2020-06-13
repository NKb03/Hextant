/**
 *@author Nikolaus Knop
 */

package hextant.inspect

/**
 * A Problem that reports a possible error
 */
sealed class Problem<in T : Any> {
    /**
     * @return the message of this [Problem]
     */
    abstract val message: String

    /**
     * @return the severity of this [Problem]
     */
    abstract val severity: Severity

    /**
     * @return the possible [ProblemFix]es to fix this [Problem]
     */
    abstract val fixes: Collection<ProblemFix<T>>

    /**
     * A Warning which indicates a possible error
     * * Does not prevent programs from being runnable
     */
    data class Warning<T : Any>(
        override val message: String,
        override val fixes: Collection<ProblemFix<T>>
    ) : Problem<T>() {
        override val severity: Severity
            get() = Severity.Warning
    }

    /**
     * An Error which prevents programs from being runnable
     */
    data class Error<T : Any>(
        override val message: String,
        override val fixes: Collection<ProblemFix<T>>
    ) : Problem<T>() {
        override val severity: Severity
            get() = Severity.Error
    }

    companion object {
        /**
         * @return [Problem] with the specified [severity], [message] and [fixes] defaulting to an [emptyList]
         */
        fun <T : Any> of(
            severity: Severity,
            message: String,
            fixes: Collection<ProblemFix<T>> = emptyList()
        ): Problem<T> {
            return if (severity.isSevere) Error(message, fixes)
            else Warning(message, fixes)
        }
    }
}