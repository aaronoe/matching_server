@file:Suppress("NAME_SHADOWING")

package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object AaronPopularityAlgorithm : StudentMatchingAlgorithm {

    override fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        data class MapResult(
            val students: List<Student>,
            val seminar: Seminar,
            val hasCapacityLeft: Boolean
        ) {

            override fun toString(): String {
                return "${seminar.name} - $hasCapacityLeft"
            }
        }

        val map = students
            .groupBy { it.preferences.first() }
            .mapValues { MapResult(it.value, it.key, it.value.count() <= it.key.capacity) }

        val matchedStudents = map
            .filter { it.value.hasCapacityLeft }
            .flatMap { it.value.students }
            .also {
                it.forEach { student ->
                    student.match = student.preferences.first().also {
                        it.assignments.add(student)
                    }
                }
            }
        val unmatchedStudents = students - matchedStudents

        (0 until seminars.size).forEach { index ->
            for (student in unmatchedStudents.shuffled()) {
                if (student.match == null) {
                    val currentPreference = student.preferences.getOrNull(index) ?: continue
                    if (currentPreference.canAssignMore) {
                        currentPreference.assignments.add(student)
                        student.match = currentPreference
                    }
                }
            }
        }

        println(map)

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