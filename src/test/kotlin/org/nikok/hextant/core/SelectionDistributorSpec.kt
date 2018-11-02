package org.nikok.hextant.core

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import org.nikok.hextant.Editor
import org.nikok.hextant.HextantPlatform
import org.nikok.hextant.core.CorePermissions.Internal
import org.nikok.hextant.core.impl.SelectionDistributor
import org.nikok.hextant.core.mocks.MockEditor

internal object SelectionDistributorSpec: Spek({
    val platform = HextantPlatform.newInstance()
    val sut = platform[Internal, SelectionDistributor]
    test(sut, platform)
}) {
    operator fun invoke(
        sut: SelectionDistributor,
        platform: HextantPlatform
    ): Spek {
        return Spek.wrap { test(sut, platform) }
    }
}

private fun SpecBody.test(sut: SelectionDistributor, platform: HextantPlatform) {
    given("a new selection distributor") {
        fun testSelectedEditorsEqual(vararg expected: Editor<*>) {
            sut.selectedEditors.now shouldMatch equalTo(expected.toSet())
        }
        it("should have no selected editors") {
            sut.selectedEditors.now shouldMatch equalTo(emptySet())
        }
        val editor = MockEditor(platform = platform)
        on("selecting an editor") {
            editor.select()
            test("the selected editor should be in the selected editors") {
                testSelectedEditorsEqual(editor)
            }
        }
        on("toggling selection") {
            editor.toggleSelection()
            test("nothing happens") {
                testSelectedEditorsEqual(editor)
            }
        }
        val editor2 = MockEditor(platform = platform)
        on("selecting another editor") {
            editor2.select()
            test("only the newly selected editor should be selected") {
                testSelectedEditorsEqual(editor2)
            }
        }
        on("toggling selection of a previously unselected editor") {
            editor.toggleSelection()
            test("both the old and the new editor should be selected") {
                testSelectedEditorsEqual(editor, editor2)
            }
        }
        on("toggling selection of a previously selected editor") {
            editor2.toggleSelection()
            test("only the other selected editor should remain selected") {
                testSelectedEditorsEqual(editor)
            }
        }
    }
}



