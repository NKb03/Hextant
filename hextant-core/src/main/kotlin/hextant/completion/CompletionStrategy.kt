package hextant.completion

import hextant.completion.CompletionResult.Match
import hextant.completion.CompletionResult.NoMatch

/**
 * A completion strategy
 */
interface CompletionStrategy {
    /**
     * Match the given input string and the [completion] and return a [CompletionResult].
     */
    fun match(input: String, completion: String): CompletionResult

    private object Simple : CompletionStrategy {
        override fun match(input: String, completion: String): CompletionResult {
            //if (now == completion) return NoMatch
            var completionRegionStart = 0
            var completionIdx = -1
            val matchedRegions = mutableListOf<IntRange>()
            outer@ for (n in input) {
                //search associated character in completion
                inner@ while (true) {
                    completionIdx++
                    if (completionIdx >= completion.length) return NoMatch //No associated character found in completion
                    val c = completion[completionIdx]
                    //Next character in input
                    if (c == n) break@inner
                    if (completionRegionStart < completionIdx) { //Have there been characters associated
                        matchedRegions.add(completionRegionStart until completionIdx)
                    }
                    completionRegionStart = completionIdx + 1

                }
            }
            matchedRegions.add(completionRegionStart..completionIdx)
            return Match(matchedRegions)
        }
    }

    private class Words(
        private val isSeparator: Char.() -> Boolean,
        private val charEquality: (Char, Char) -> Boolean
    ) : CompletionStrategy {
        override fun match(input: String, completion: String): CompletionResult {
            if (input == completion) return NoMatch
            var completionRegionStart = 0
            var completionIdx = -1
            val matchedRegions = mutableListOf<IntRange>()
            for (n: Char in input) {
                completionIdx++
                if (completionIdx >= completion.length) return NoMatch
                var c = completion[completionIdx]
                when {
                    charEquality(c, n) -> {
                    }
                    n.isSeparator()    -> {
                        if (completionRegionStart < completionIdx) { //Have there been characters associated
                            matchedRegions.add(completionRegionStart until completionIdx)
                        }
                        inner@ while (true) {
                            completionIdx++
                            if (completionIdx >= completion.length) return NoMatch
                            c = completion[completionIdx]
                            if (charEquality(c, n)) {
                                completionRegionStart = completionIdx
                                break@inner
                            }
                        }
                    }
                    else               -> return NoMatch
                }
            }
            matchedRegions.add(completionRegionStart..completionIdx)
            return Match(matchedRegions)
        }
    }

    companion object {
        /**
         * An equality predicate on chars that ignores the case of letters
         */
        val equalityIgnoreCase = { c1: Char, c2: Char -> c1.equals(c2, ignoreCase = true) }

        /**
         * A simple completion strategy the doesn't respect whitespace or capital letters
         */
        val simple: CompletionStrategy = Simple

        /**
         * The camelcase completion strategy
         */
        val camelCase: CompletionStrategy = Words(Char::isUpperCase, equalityIgnoreCase)

        /**
         * A completion strategy that separated words by the given [separators] and then matches them
         */
        fun separators(
            separators: Set<Char>,
            charEquality: (Char, Char) -> Boolean = Char::equals
        ): CompletionStrategy = Words(separators::contains, charEquality)

        /**
         * Vararg function for [CompletionStrategy.separators]
         */
        fun separators(
            vararg separators: Char,
            charEquality: (Char, Char) -> Boolean = Char::equals
        ): CompletionStrategy =
            separators(separators.toSet(), charEquality)

        /**
         * A separation completion strategy separating with a underscore ('_')
         */
        val underscore: CompletionStrategy = separators('_', charEquality = Char::equals)

        /**
         * A separation completion strategy separating by a hyphen ('-')
         */
        val hyphen: CompletionStrategy = separators('-', charEquality = Char::equals)
    }
}
