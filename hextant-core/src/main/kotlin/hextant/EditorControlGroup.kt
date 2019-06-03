/**
 *@author Nikolaus Knop
 */

package hextant

import hextant.base.EditorControl
import hextant.bundle.Bundle
import hextant.bundle.CorePermissions.Public
import hextant.bundle.Property
import hextant.util.DoubleWeakHashMap

class EditorControlGroup : ViewGroup<EditorControl<*>> {
    private val views = DoubleWeakHashMap<Editor<*>, EditorControl<*>>()

    override fun getViewOf(editor: Editor<*>): EditorControl<*> =
        views[editor] ?: throw NoSuchElementException("No view for $editor in this view group")

    override fun createViewFor(editor: Editor<*>, context: Context, arguments: Bundle): EditorControl<*> {
        val contexts = generateSequence(context) { it.parent }
        for (c in contexts) {
            val control = context[EditorControlFactory].createControl(editor, arguments)
            if (control != null) {
                views[editor] = control
                return control
            }
        }
        throw NoSuchElementException("No view factory registered for $editor")
    }

    companion object : Property<EditorControlGroup, Public, Public>("editor control group")
}