package de.aaronoe.algorithms.cpp

import de.aaronoe.Repository.awaitCompletion
import de.aaronoe.algorithms.StudentMatchingAlgorithm
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import java.io.StringWriter
import java.util.*
import kotlin.system.measureTimeMillis

class BaseCppAlgorithm(private val type: Algorithm): StudentMatchingAlgorithm {

    enum class Algorithm(val argName: String) {
        Hungarian("hungarian"),
        Popular("popular"),
        RSD("rsd"),
        MaxPareto("max-pareto")
    }

    override suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        var id = 0
        val seminarIdMap = seminars.associateBy({ it.id }, { id++ })

        val result = with(StringWriter()) {
            appendln("${seminars.size} ${students.size}")

            seminars.forEachIndexed { index, seminar ->
                appendln("$index ${seminar.capacity}")
            }

            students.forEachIndexed { index, student ->
                append("$index ${student.preferences.size}")
                student.preferences.forEach { seminar ->
                    val mappedId = seminarIdMap.getValue(seminar.id)

                    append(" $mappedId")
                }
                appendln()
            }

            toString()
        }

        val process = Runtime.getRuntime().exec("./seminar_assignment ${type.argName}")

        measureTimeMillis {
            process.outputStream.bufferedWriter().use {
                it.write(result)
            }

            process.awaitCompletion()
        }.let {
            println("Execution took ${it}ms")
        }

        val scanner = Scanner(process.inputStream)
        val resultLength = scanner.nextInt()
        val matching = (0 until resultLength).map {
            val student = scanner.nextInt()
            val seminar = scanner.nextInt()

            students[student] to seminars[seminar]
        }

        return matching.groupBy(Pair<Student, Seminar>::second, Pair<Student, Seminar>::first)
    }

}