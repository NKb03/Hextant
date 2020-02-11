/**
 *@author Nikolaus Knop
 */

package hextant.fx

import hextant.Context
import hextant.completion.Completion
import hextant.gui.SearchableListController
import hextant.gui.SearchableListView
import javafx.scene.control.*
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.layout.*
import javafx.scene.paint.Color

class SearchableListPopup<T : Any>(
    private val controller: SearchableListController<T>,
    noCompletionsText: String = DEFAULT_NO_COMPLETIONS_TEXT,
    context: Context
) : PopupControl(),
    SearchableListView<T> {
    private val searchBar = SearchBar()

    private val noCompletionsLabel = Label(noCompletionsText).apply { blackBackground() }

    private fun Region.blackBackground() {
        background = Background(BackgroundFill(Color.BLACK, CornerRadii(5.0), insets))
        if (this is Label) {
            textFill = Color.WHITE
        }
    }

    private val root = VBox(5.0, searchBar, noCompletionsLabel)

    private val completionsBox = VBox().apply {
        styleClass.add("searchable-list-completions")
        blackBackground()
    }

    init {
        scene.root = root
        scene.initHextantScene(context.platform)
        listenForSearchTextChange()
    }

    private fun listenForSearchTextChange() {
        controller.addView(this)
        searchBar.textProperty().addListener { _, _, new ->
            controller.text = new
        }
    }

    override fun displaySearchText(new: String) {
        searchBar.text = new
    }

    override fun displayCompletions(completions: Collection<Completion<T>>) {
        completionsBox.children.clear()
        root.children[1] = completionsBox
        for (completion in completions) {
            val node = createItem(completion)
            completionsBox.isFillWidth = true
            node.styleClass.add("completion")
            addSelectionHandlers(node, completion)
            completionsBox.children.add(node)
        }
    }

    private fun createItem(completion: Completion<T>): Control {
        TODO("not implemented")
    }

    private fun addSelectionHandlers(node: Control, completion: Completion<T>) {
        node.setOnMouseClicked { evt ->
            controller.selectCompletion(completion)
            evt.consume()
        }
        node.setOnKeyPressed { evt ->
            if (evt.code == ENTER) {
                controller.selectCompletion(completion)
                evt.consume()
            }
        }
    }

    override fun displayNoCompletions() {
        root.children[1] = noCompletionsLabel
    }

    override fun close() {
        hide()
    }

    companion object {
        private const val DEFAULT_NO_COMPLETIONS_TEXT = "No completions available"
    }
}