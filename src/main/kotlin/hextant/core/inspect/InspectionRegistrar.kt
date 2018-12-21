/**
 *@author Nikolaus Knop
 */

package hextant.core.inspect

import org.nikok.reaktive.value.ReactiveBoolean

/**
 * Used to register [Inspection]s for objects of type [T]
 */
class InspectionRegistrar<T : Any> internal constructor() {
    private val managers: MutableMap<T, InspectionManager<T>> = HashMap()

    private val inspectionFactories: MutableList<(T) -> Inspection<T>> = mutableListOf()

    /**
     * Register the specified [inspection]
     */
    fun register(inspection: (T) -> Inspection<T>) {
        inspectionFactories.add(inspection)
        for (m in managers.values) {
            m.addInspection(inspection)
        }
    }

    private fun getInspectionManager(inspected: T): InspectionManager<T> {
        return managers.getOrPut(inspected) { createInspectionManager(inspected) }
    }

    private fun createInspectionManager(inspected: T): InspectionManager<T> {
        val manager = InspectionManager(inspected)
        for (iFact in inspectionFactories) {
            manager.addInspection(iFact)
        }
        return manager
    }

    internal fun getProblems(inspected: T): Set<Problem> {
        val m = getInspectionManager(inspected)
        return m.problems()
    }


    internal fun hasError(inspected: T): ReactiveBoolean {
        val m = getInspectionManager(inspected)
        return m.hasError
    }

    internal fun hasWarning(inspected: T): ReactiveBoolean {
        val m = getInspectionManager(inspected)
        return m.hasWarning
    }
}