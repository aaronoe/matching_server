package de.aaronoe

import de.aaronoe.algorithms.*
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kravis.geomCol
import kravis.plot
import kravis.scaleYLog10
import java.awt.Dimension
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import kotlin.math.roundToInt


data class Statistics(
    val profile: List<Double>,
    val unassignedCount: Double,
    val averageRank: Double,
    val standardDeviationRank: Double
)

fun printProfile(
    result: Map<Seminar, List<Student>>,
    students: List<Student>
): Statistics {
    val ranks = result
        .flatMap { (seminar, students) ->
            students.map { student -> student.preferences.indexOf(seminar) }
        }

    val profile = ranks
        .filter { it >= 0 }
        .groupBy { it }
        .mapValues { it.value.count() }
        .toList()
        .sortedBy(Pair<Int, Int>::first)
        .map(Pair<Int, Int>::second)

    val unassignedCount = students.count() - profile.sum()

    println("Profile: $profile")
    println("Unassigned: $unassignedCount")
    return Statistics(
        profile = profile.map { it.toDouble() },
        unassignedCount = unassignedCount.toDouble(),
        averageRank = ranks.average() + 1,
        standardDeviationRank = ranks.standartDeviation()
    )
}

fun doTestRun(
    runs: Int = 10,
    dataSupplier: () -> Pair<List<Student>, List<Seminar>> = MockData::getMediumSizedRandomData
) = runBlocking {
    val data = (0 until runs).map { dataSupplier() }
    val tasks = listOf(CppHungarian, CppRsd, CppMaxPareto, CppPopular).map { algorithm ->
        (0 until runs).map {
            async {
                val (students, seminars) = data[it].deepCopy()

                val matching = algorithm.execute(students, seminars)
                algorithm to printProfile(matching, students)
            }
        }
    }

    val results = tasks.map { it.awaitAll() }


    results.forEach {
        it.forEach { (algorithm, stats) ->
            val name = when (algorithm) {
                is PopularChaAlgorithm -> "PopularCHA"
                is RandomSerialDictatorshipAlgorithm -> "RSD"
                is HungarianAlgorithm -> "Hungarian"
                is CppHungarian -> "Cpp Hungarian"
                is CppRsd -> "Cpp RSD"
                is CppPopular -> "Cpp Popular"
                is CppMaxPareto -> "Cpp Max Pareto"
                else -> throw IllegalStateException()
            }
            println("Algorithm: $name - Stats: $stats")
        }
        val algo = it.groupBy { it.first }.entries.first().key
        it.map { it.second }.average().let {
            println("Algorithm: $algo - Average Stats: $it")
        }

        it.first().second.profile.mapIndexed { index, d -> index to d }
            .plot(x = { first + 1 }, y = { second })
            .geomCol()
            .scaleYLog10()
            .xLabel("Seminar")
            .yLabel("Average Rank")
            .title("Rank Distribution")
            .apply { save(File("benchmark/${it.first().first}test.png"), Dimension(2000, 1300)) }
            .show()
    }
}

fun Collection<Statistics>.average(): Statistics {
    val avg = map { it.averageRank }.average()
    val std = map { it.standardDeviationRank }.average()
    val unassignedCount = map { it.unassignedCount }.average()

    val maxProfileLength = map { it.profile }.maxBy { it.size }?.size ?: 0
    val profileAvg = (0 until maxProfileLength).map { index ->
        map { it.profile.getOrElse(index) { 0.toDouble() } }.average()
    }

    return Statistics(profileAvg, unassignedCount, avg, std)
}

fun main() {
    doTestRun()
}

fun parseSampelData() {
    val scanner = Scanner(System.`in`)

    val courseCount = scanner.nextInt()
    scanner.nextLine()
    val courses = (1..courseCount).map {
        // skip those, as they don't contain any meaningful information
        scanner.nextLine()
        Seminar("$it", 1)
    }

    val (studentCount, _, preferenceCount) = scanner.nextLine().split(",").map(String::toInt)

    val capacity = (studentCount.toDouble() / courseCount).roundToInt() + 1
    val seminars = courses.map { it.copy(capacity = capacity) }
    val courseMap = seminars.associateBy { it.name }

    val students = (0 until preferenceCount).map {
        val line = scanner.nextLine().split(",").map(String::toInt)
        val (count) = line

        val prefList = line.drop(2).map { courseMap.getValue("$it") }

        (0 until count).map {
            Student(name = "$it", preferences = prefList)
        }
    }.flatten()

    val test = students
        .map { it.preferences.first() }
        .groupBy { it }
        .mapValues { it.value.size }
        .let { println("${it.map { it.key.name to it.value }}") }

    val avgRank = seminars.map { seminar ->
        seminar.name to students.map { it.preferences.indexOf(seminar) }.average()
    }

    avgRank.filter { it.second >= 0 }.plot(x = { "Seminar $first" }, y = { second })
        .geomCol()
        .xLabel("Seminar")
        .yLabel("Average Rank")
        .title("Rank Distribution")
        .show()

    println("Average Rank: $avgRank")
}