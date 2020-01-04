/**
 *@author Nikolaus Knop
 */

package hextant.project.view

import hextant.base.EditorControl
import hextant.bundle.Bundle
import hextant.createView
import hextant.fx.Glyphs
import hextant.fx.fontSize
import hextant.project.editor.DirectoryEditor
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import org.controlsfx.glyphfont.FontAwesome.Glyph.FOLDER

class DirectoryEditorControl(
    editor: DirectoryEditor<*>,
    arguments: Bundle
) : EditorControl<HBox>(editor, arguments) {
    val directoryName = context.createView(editor.directoryName)

    init {
        setChildren(directoryName)
    }

    override fun receiveFocus() {
        directoryName.receiveFocus()
    }

    override fun createDefaultRoot(): HBox =
        HBox(5.0, Glyphs.create(FOLDER).fontSize(16.0).color(Color.SLATEGRAY), directoryName)
}