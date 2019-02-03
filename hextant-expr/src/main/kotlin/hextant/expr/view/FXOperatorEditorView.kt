/**
 *@author Nikolaus Knop
 */

package hextant.expr.view

import hextant.Context
import hextant.core.view.FXTokenEditorView
import hextant.expr.editable.EditableOperator

class FXOperatorEditorView(editable: EditableOperator, context: Context) :
    FXTokenEditorView(editable, context)