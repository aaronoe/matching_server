package de.aaronoe

import de.aaronoe.algorithms.*
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking


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

fun doTestRun(runs: Int = 10) = runBlocking {
    val tasks = listOf(PopularChaAlgorithm, RandomSerialDictatorshipAlgorithm, HungarianAlgorithm).map { algorithm ->
        (0 until runs).map {
            async {
                val (students, seminars) = MockData.getMediumSizedRandomData()

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
                else -> throw IllegalStateException()
            }
            println("Algorithm: $name - Stats: $stats")
        }
        val algo = it.groupBy { it.first }.entries.first().key
        it.map { it.second }.average().let {
            println("Algorithm: $algo - Average Stats: $it")
        }
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
