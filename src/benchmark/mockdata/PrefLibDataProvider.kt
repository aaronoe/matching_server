package de.aaronoe.benchmark.mockdata

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import java.net.URL
import java.util.*
import kotlin.math.roundToInt

class PrefLibDataProvider(
    private val url: String
): MockDataProvider("PrefLibDataProvider:${url.split("/").last()}") {

    companion object {
        val prefLib1 = PrefLibDataProvider("http://www.preflib.org/data/election/agh/ED-00009-00000001.soc")
        val prefLib2 = PrefLibDataProvider("http://www.preflib.org/data/election/agh/ED-00009-00000002.soc")
    }

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val scanner = Scanner(URL(url).openStream())

        val courseCount = scanner.nextInt()
        scanner.nextLine()
        val courses = (1..courseCount).map {
            // skip those, as they don't contain any meaningful information
            scanner.nextLine()
            Seminar("$it", 1)
        }

        val (studentCount, _, preferenceCount) = scanner.nextLine().split(",").map(String::toInt)

        val capacity = (studentCount.toDouble() / courseCount).roundToInt() + 5
        val seminars = courses.map { it.copy(capacity = capacity) }
        val courseMap = seminars.associateBy { it.name }

        val students = (0 until preferenceCount).map {
            val line = scanner.nextLine().split(",").map(String::toInt)
            val (count) = line

            val prefList = line.drop(2).map { courseMap.getValue("$it") }

            (0 until count).map {
                Student(name = getRandomName(), preferences = prefList)
            }
        }.flatten()

        return students to seminars
        /*
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
        */
    }
}