package de.aaronoe.benchmark.mockdata

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object MediumMockDataProvider : MockDataProvider("MediumUniform") {

    private const val SEMINAR_COUNT = 10

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until SEMINAR_COUNT).map {
            Seminar("Seminar #$it", SEMINAR_COUNT * 2)
        }

        val totalCapacity = seminars.sumBy { it.capacity }

        val students = (0 until totalCapacity).map {
            Student("$it", LargeMockDataProvider.getRandomName(), seminars.shuffled())
        }

        return students to seminars
    }

}