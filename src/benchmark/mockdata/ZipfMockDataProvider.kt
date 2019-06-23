package de.aaronoe.benchmark.mockdata

import benchmark.PowerLaw
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlin.random.Random

object ZipfMockDataProvider: MockDataProvider("MediumZipf") {

    private val powerLaw = PowerLaw()

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until 40).map {
            Seminar("$it", Random.nextInt(30, 55))
        }

        val totalCapacity = seminars.sumBy { it.capacity }

        val students = (0 until totalCapacity).map {
            val preferenceListLength = Random.nextInt(3, 8)
            val takenIndices = hashSetOf<Int>()

            val prefList = (0 until preferenceListLength).map inner@ {
                while (true) {
                    val index = powerLaw.zipf(seminars.size)
                    if (index !in takenIndices) {
                        takenIndices += index
                        return@inner seminars[index]
                    }
                }
            }.filterIsInstance(Seminar::class.java).also {
                assert(it.size == preferenceListLength)
            }

            Student("$it", "$it", prefList)
        }

        return students to seminars
    }
}