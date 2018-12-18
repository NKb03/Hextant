package org.nikok.hextant.core.undo

import matchers.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*

internal object AbstractEditSpec : Spek({
    given("an edit") {
        val e = AnEdit()
        it("should be able to undo") {
            e.canUndo shouldBe `true`
        }
        it("shouldn't be able to redo") {
            e.canRedo shouldBe `false`
        }
        on("redoing it") {
            it("should throw an IllegalStateException") {
                { e.redo() }.shouldThrow<IllegalStateException>()
            }
            it("should not actually redo") {
                e.undone shouldBe `false`
            }
            it("should still not be able to redo") {
                e.canRedo shouldBe `false`
            }
            it("should still be able to undo") {
                e.canUndo shouldBe `true`
            }
        }
        on("undoing it") {
            e.undo()
            it("should actually undo") {
                e.undone shouldBe `true`
            }
            it("should be able to redo") {
                e.canRedo shouldBe `true`
            }
            it("should not be able to undo") {
                e.canUndo shouldBe `false`
            }
        }
        on("undoing it") {
            it("should throw an IllegalStateException") {
                { e.undo() }.shouldThrow<IllegalStateException>()
            }
            it("should not actually undo") {
                e.undone shouldBe `true`
            }
            it("should still not be able to undo") {
                e.canUndo shouldBe `false`
            }
            it("should still be able to redo") {
                e.canRedo shouldBe `true`
            }
        }
        on("redoing it") {
            e.redo()
            it("should actually redo") {
                e.undone shouldBe `false`
            }
            it("should be able to undo") {
                e.canUndo shouldBe `true`
            }
            it("should not be able to redo") {
                e.canRedo shouldBe `false`
            }
        }
    }
})
