@file:Suppress("NAME_SHADOWING")

package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object GreedyMatchingAlgorithm : StudentMatchingAlgorithm {

    override fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        val students = students.map { it.copy() }

        val assignments = mutableMapOf<Student, Seminar>()
        val unassignedStudents = mutableSetOf<Student>()

        unassignedStudents.addAll(students)

        var changeMade = true

        while (changeMade && unassignedStudents.toSet().isNotEmpty()) {
            changeMade = unassignedStudents.toSet().map { tryAssignment(it, assignments, unassignedStudents) }.all { it }
        }

        println("Results:")
        assignments.mapValues { (student, seminar) ->
            val selectedPriority = student.preferences.indexOf(seminar) + 1
            //println("Student #${student.id} - ${seminar.name} - Priority $selectedPriority")
            selectedPriority
        }.let {
            println("Weight: ${it.values.sum()}")
            println("Average Priority: ${it.values.average()} - Standart Deviation: ${it.values.standartDeviation()}")
            println("Matching is complete: ${assignments.size == students.size}")
        }

        return assignments.map { it }.groupBy { it.value }.mapValues { it.value.map { it.key } }
    }

    private fun tryAssignment(
        student: Student,
        assignments: MutableMap<Student, Seminar>,
        unassignedStudents: MutableSet<Student>
    ): Boolean {
        student.preferences.forEachIndexed { index, seminar ->
            if (seminar.tryAssign(student, index, assignments, unassignedStudents)) {
                return true
            }
        }

        return false
    }

}