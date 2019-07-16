package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlin.system.measureTimeMillis

interface StudentMatchingAlgorithm {

    suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>>

    suspend fun executeWithTimestamp(students: List<Student>, seminars: List<Seminar>): MatchingResult {
        var result = emptyMap<Seminar, List<Student>>()

        val time = measureTimeMillis {
            result = execute(students, seminars)
        }

        return MatchingResult(result, time)
    }

    data class MatchingResult(
        val result: Map<Seminar, List<Student>>,
        val timeInMillis: Long
    )

}