/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core.expr.editable

import org.nikok.hextant.Editable
import org.nikok.hextant.core.editable.Expandable
import org.nikok.hextant.core.expr.edited.Expr

class ExpandableExpr(override val parent: Editable<*>? = null): Expandable<Expr, Editable<Expr>>()