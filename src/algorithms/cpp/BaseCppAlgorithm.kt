package de.aaronoe.algorithms.cpp

import de.aaronoe.algorithms.StudentMatchingAlgorithm
import de.aaronoe.algorithms.StudentMatchingAlgorithm.MatchingResult
import de.aaronoe.benchmark.formatDataToInput
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import java.util.*
import kotlin.system.measureTimeMillis

class BaseCppAlgorithm(private val type: Algorithm) : StudentMatchingAlgorithm {

    enum class Algorithm(val argName: String) {
        Hungarian("hungarian"),
        Popular("popular"),
        PopularModified("popular-modified"),
        RSD("rsd"),
        MaxPareto("max-pareto")
    }

    override suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        return executeWithTimestamp(students, seminars).result
    }

    override suspend fun executeWithTimestamp(students: List<Student>, seminars: List<Seminar>): MatchingResult {
        val input = formatDataToInput(students, seminars)
        val process = Runtime.getRuntime().exec("./seminar_assignment ${type.argName}")

        measureTimeMillis {
            process.outputStream.bufferedWriter().use {
                it.write(input)
            }

            process.errorStream.bufferedReader().use {
                it.lines().forEach {
                    //System.err.println(it)
                }
            }

            process.waitFor()
            if (process.exitValue() == 139 || process.exitValue() == 134) return MatchingResult(
                emptyMap(),
                0
            )
        }.let {
            println("Execution for ${type.name} took ${it}ms")
        }

        val scanner = Scanner(process.inputStream)
        val time = scanner.nextLong()
        val resultLength = scanner.nextInt()
        val matching = (0 until resultLength).map {
            val student = scanner.nextInt()
            val seminar = scanner.nextInt()

            students[student] to seminars[seminar]
        }

        return MatchingResult(matching.groupBy(Pair<Student, Seminar>::second, Pair<Student, Seminar>::first), time)
    }

}