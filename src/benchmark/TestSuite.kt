package de.aaronoe.benchmark

import de.aaronoe.algorithms.*
import de.aaronoe.algorithms.cpp.*
import de.aaronoe.benchmark.mockdata.LargeMockDataProvider
import de.aaronoe.benchmark.mockdata.MockDataProvider
import de.aaronoe.benchmark.mockdata.PrefLibDataProvider
import de.aaronoe.benchmark.mockdata.ZipfMockDataProvider
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlinx.coroutines.*
import kravis.*
import java.awt.Dimension
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("dd-MM-yy_HH:mm:ss")

data class Result(
    val stats: Statistics,
    val algorithm: StudentMatchingAlgorithm,
    val matching: Map<Seminar, List<Student>>,
    val timestamp: Long = 0
)

data class Statistics(
    val profile: List<Double>,
    val unassignedCount: Double,
    val averageRank: Double,
    val standardDeviationRank: Double,
    val averageRankWithUnassigned: Double,
    val standardDeviationWithUnassigned: Double
) {

    override fun toString(): String {
        return with(StringBuilder()) {
            append("Profile: $profile")
            append(" - ")
            append("Unassigned Count: ${unassignedCount.toInt()}")
            append(" - ")
            append("Average Rank: ${averageRank.toString().take(5)}")
            append(" - ")
            append("Standard Deviation Rank: ${standardDeviationRank.toString().take(5)}")
            append(" - ")
            append("Average Rank (/w unassigned): ${averageRankWithUnassigned.toString().take(5)}")
            append(" - ")
            append("Standard Deviation Rank (/w unassigned): ${standardDeviationWithUnassigned.toString().take(5)}")
            toString()
        }
    }
}

val StudentMatchingAlgorithm.name: String
    get() {
        return when (this) {
            is PopularChaAlgorithm -> "PopularCHA"
            is RandomSerialDictatorshipAlgorithm -> "RSD"
            is HungarianAlgorithm -> "Hungarian"
            is CppHungarian -> "Cpp Hungarian"
            is CppRsd -> "Cpp RSD"
            is CppPopular -> "Cpp Popular"
            is CppMaxPareto -> "Cpp Max Pareto"
            is CppPopularModified -> "Cpp Popular Modified"
            else -> throw IllegalArgumentException()
        }
    }

fun getStatistics(
    result: Map<Seminar, List<Student>>,
    students: List<Student>,
    seminars: List<Seminar>
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

    val max = seminars.size + 1
    val ranksWithUnassigned = ranks + (0 until unassignedCount).map { max }.also { println("Size extra: ${it.size}") }

    return Statistics(
        profile = profile.map { it.toDouble() },
        unassignedCount = unassignedCount.toDouble(),
        averageRank = ranks.average() + 1,
        standardDeviationRank = ranks.standartDeviation(),
        averageRankWithUnassigned = ranksWithUnassigned.average() + 1,
        standardDeviationWithUnassigned = ranksWithUnassigned.standartDeviation()
    )
}

fun doTestRun(
    runs: Int = 8,
    dataSupplier: MockDataProvider = LargeMockDataProvider
) = runBlocking(Dispatchers.Default) {
    val data = (0 until runs).map { dataSupplier.generateData() }

    data.first().first.map { it.preferences.first().name }
        .groupBy { it }.mapValues { it.value.size }
        .toList()
        .sortedBy { it.second }
        .let(::println)

    val tasks = listOf(CppRsd, CppMaxPareto, CppPopular, CppPopularModified, CppHungarian).map { algorithm ->
        (0 until runs).map {
            async {
                val (students, seminars) = data[it].deepCopy()

                val (matching, timestamp) = algorithm.executeWithTimestamp(students, seminars)
                Result(
                    stats = getStatistics(matching, students, seminars),
                    algorithm = algorithm,
                    matching = matching,
                    timestamp = timestamp
                )
            }
        }
    }

    val results = tasks.map { it.awaitAll() }.flatten().groupBy { it.algorithm }
    saveResults(data, dataSupplier, results)
}

private fun CoroutineScope.saveResults(
    data: List<Pair<List<Student>, List<Seminar>>>,
    dataSupplier: MockDataProvider,
    results: Map<StudentMatchingAlgorithm, List<Result>>
) {
    val directory = File("benchmark/${dataSupplier.name}_${dateFormat.format(Date())}").apply { mkdirs() }
    val graphDirectory = File("${directory.path}/graphs").apply { mkdirs() }
    val dataDirectory = File("${directory.path}/data").apply { mkdirs() }
    val statsDirectory = File("${directory.path}/stats").apply { mkdirs() }

    data.forEachIndexed { index, pair ->
        FileWriter("${dataDirectory.path}/$index.in").apply {
            write(formatDataToInput(pair.first, pair.second))
            close()
        }
    }

    results.forEach { (algorithm, results) ->
        val summary = with(StringBuilder()) {
            appendln("Algorithm: ${algorithm.name} - Dataset ${dataSupplier.name}")

            results.forEachIndexed { index, result ->
                appendln("[$index] ${result.stats} - Runtime: ${result.timestamp}ms")
            }

            val avgRuntime = results.map { it.timestamp }.average()
            appendln("Average Stats: ${results.map { it.stats }.average()}  - Runtime: ${avgRuntime}ms")

            toString()
        }

        FileWriter("${statsDirectory.path}/${algorithm.name}.stats").apply {
            write(summary)
            close()
        }

        println(summary)

        launch {
            results.map { it.stats }.forEachIndexed { index, statistics ->
                if (statistics.profile.isNotEmpty()) {
                    statistics.profile.mapIndexed { i, d -> i to d }
                        .plot(x = { first + 1 }, y = { second })
                        .geomCol()
                        .scaleYLog10()
                        .xLabel("Seminar")
                        .yLabel("Average Rank")
                        .title("Rank Distribution")
                        .apply { save(File("${graphDirectory.path}/${algorithm.name}_$index.png"), Dimension(2000, 1300)) }
                }
            }
        }
    }

    val popularityMatrix = getPopularityMatrix(results)
    FileWriter("${statsDirectory.path}/popularity.matrix").apply {
        write(popularityMatrix)
        close()
    }
    println(popularityMatrix)
}

private fun getPopularityMatrix(results: Map<StudentMatchingAlgorithm, List<Result>>) = with(StringWriter()) {
    val count = results.entries.first().value.size

    (0 until count).forEach { index ->
        appendln("Dataset $index:")
        appendln()
        appendf("")
        results.entries.forEachIndexed { i, entry -> appendf("[$i]") }
        appendln()

        results.entries.forEachIndexed { i, entry ->
            val result = entry.value[index]
            appendf("[$i]")
            results.entries.map { it.value[index] }.mapIndexed { index, res ->
                if (i == index) "-" else (result isMorePopularThan res).toString()
            }.forEach {
                appendf(it)
            }
            appendln()
        }

        appendln()
        appendln()
    }

    results.entries.forEachIndexed { index, entry ->
        appendln("[$index] = ${entry.key.name}")
    }

    toString()
}

private fun StringWriter.appendf(value: Any) {
    append(value.toString().take(8).padStart(8))
}

fun Collection<Statistics>.average(): Statistics {
    val filtered = filter { it.profile.isNotEmpty() }
    val avg = filtered.map { it.averageRank }.average()
    val std = filtered.map { it.standardDeviationRank }.average()
    val unassignedCount = filtered.map { it.unassignedCount }.average()

    val maxProfileLength = filtered.map { it.profile }.maxBy { it.size }?.size ?: 0
    val profileAvg = (0 until maxProfileLength).map { index ->
        filtered.map { it.profile.getOrElse(index) { 0.toDouble() } }.average()
    }

    return Statistics(profileAvg, unassignedCount, avg, std,
        filtered.map { it.averageRankWithUnassigned }.average(),
        filtered.map { it.standardDeviationWithUnassigned }.average()
    )
}

fun main() {
    //doTestRun(runs = 10, dataSupplier = LargeMockDataProvider)
    printProfile()
}

private fun printProfile() {
    val (students, seminars) = ZipfMockDataProvider.generateData()
    val avgRank = seminars.map { seminar ->
        seminar.name to students.count { it.preferences.first() == seminar }
    }.sortedByDescending { it.second }.filter { it.second != 0 }.also(::println).take(10)

    File("distributions").mkdirs()

    avgRank.plot(x = { "#$first" }, y = { second }, fill = { first })
        .geomCol(showLegend = false)
        .xLabel("Seminar ID")
        .yLabel("Number of students")
        .title("Rank Distribution for the first choice - Zipfian")
        .show()
        //.save(File("distributions/zipfian2-distribution.png"), Dimension(1500, 1300))
}