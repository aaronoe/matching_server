@file:Suppress("NAME_SHADOWING")

package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object RoundAlgorithm: StudentMatchingAlgorithm {

    override fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        var round = 0

        while (round < seminars.size) {
            for (student in students) {
                if (student.match == null) {
                    val currentPreference = student.preferences.getOrNull(round) ?: continue
                    if (currentPreference.canAssignMore) {
                        currentPreference.assignments.add(student)
                        student.match = currentPreference
                    }
                }
            }

            round++
        }

        students.map { student ->
            student to student.preferences.indexOf(student.match) + 1
        }.toMap().let {
            println("Weight: ${it.values.sum()}")
            println("Average Priority: ${it.values.average()} - Standart Deviation: ${it.values.standartDeviation()}")
            println("Matching is complete: ${students.filter { it.match != null }.size == students.size}")
        }

        return students.filter { it.match != null }.groupBy { it.match!! }
    }

}