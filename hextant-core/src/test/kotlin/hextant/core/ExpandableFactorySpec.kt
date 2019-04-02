package hextant.core

import hextant.core.editable.Expandable
import hextant.core.mocks.EditableMock
import matchers.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*

object ExpandableFactorySpec : Spek({
    given("an ExpandableFactory") {
        val f = ExpandableFactory.newInstance()

        class ExpandableMock : Expandable<Unit, EditableMock>()
        on("registering a factory") {
            f.register(EditableMock::class) { ExpandableMock() }
            it("should throw no error") {}
        }
        on("creating an expandable for the registered class") {
            val ex = f.createExpandable(EditableMock::class)
            it("should return an instance of the registered expandable class") {
                ex shouldBe instanceOf<ExpandableMock>()
            }
        }
    }
})