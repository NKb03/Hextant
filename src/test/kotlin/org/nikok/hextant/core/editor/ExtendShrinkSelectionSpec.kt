/**
 *@author Nikolaus Knop
 */

package org.nikok.hextant.core.editor

import matchers.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.on
import org.nikok.hextant.*
import org.nikok.hextant.core.base.AbstractEditor
import org.nikok.hextant.core.mocks.EditableMock
import org.nikok.reaktive.value.ReactiveValue
import org.nikok.reaktive.value.reactiveValue

internal object ExtendShrinkSelectionSpec : Spek({
    class Parent : ParentEditable<Unit, EditableMock>() {
        override fun accepts(child: Editable<*>): Boolean = child is EditableMock

        override val edited: ReactiveValue<Unit>
            get() = reactiveValue("unit", Unit)
    }

    class ChildEditor(platform: HextantPlatform) :
        AbstractEditor<EditableMock, EditorView>(EditableMock(), platform)

    class ParentEditor(platform: HextantPlatform) : AbstractEditor<Parent, EditorView>(Parent(), platform)

    given("3 children and a parent") {
        val platform = HextantPlatform.newInstance()
        val children = List(3) { ChildEditor(platform) }
        val parent = ParentEditor(platform)
        for (it in children) {
            it.editable.moveTo(parent.editable)
        }
        val second = children[1]
        val third = children[2]
        on("selecting the second and the third child and the extending selection") {
            second.select()
            third.toggleSelection()
            parent.extendSelection(second)
            test("the second child should not be selected") {
                second.isSelected shouldBe `false`
            }
            test("the third child should still be selected") {
                third.isSelected shouldBe `true`
            }
            test("the parent should be selected") {
                parent.isSelected shouldBe `true`
            }
        }
        on("shrinking selection on the parent") {
            parent.shrinkSelection()
            test("the second and the third child should be selected") {
                second.isSelected shouldBe `true`
                third.isSelected shouldBe `true`
            }
            test("the parent should not be selected") {
                parent.isSelected shouldBe `false`
            }
        }
        on("selecting the parent and then shrinking selection") {
            parent.select()
            parent.shrinkSelection()
            test("the first child should be selected") {
                children[0].isSelected shouldBe `true`
            }
            test("the parent should not be selected") {
                parent.isSelected shouldBe `false`
            }
        }
    }
})