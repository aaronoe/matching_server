package de.aaronoe.benchmark.mockdata

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object LargeMockDataProvider: MockDataProvider("LargeUniform") {
    private const val SEMINAR_COUNT = 50

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until SEMINAR_COUNT).map {
            Seminar("$it", SEMINAR_COUNT * 2)
        }

        val totalCapacity = seminars.sumBy { it.capacity }

        val students = (0 until totalCapacity).map {
            Student("$it", "$it", seminars.shuffled().take(10))
        }

        return students to seminars
    }
}