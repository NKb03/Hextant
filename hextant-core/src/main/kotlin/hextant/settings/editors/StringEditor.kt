/**
 *@author Nikolaus Knop
 */

package hextant.settings.editors

import hextant.context.Context
import hextant.core.editor.BidirectionalTokenEditor
import validated.Validated
import validated.valid

/**
 * A simple editor for string values.
 */
class StringEditor(context: Context, text: String) : BidirectionalTokenEditor<String>(context, text) {
    constructor(context: Context) : this(context, "")

    override fun compile(token: String): Validated<String> =
        valid(token)
}