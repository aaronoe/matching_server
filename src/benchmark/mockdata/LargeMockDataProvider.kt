package de.aaronoe.benchmark.mockdata

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlin.random.Random

object LargeMockDataProvider: MockDataProvider("LargeUniform") {

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until 100).map {
            Seminar("$it", Random.nextInt(20, 40))
        }
        val totalCapacity = seminars.sumBy { it.capacity }

        val students = (0 until totalCapacity).map {
            Student("$it", "$it", seminars.shuffled().dropLast(90))
        }

        return students to seminars
    }
}