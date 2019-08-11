package de.aaronoe.benchmark.mockdata

import benchmark.PowerLaw
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlin.random.Random

object ZipfMockDataProvider: MockDataProvider("MediumZipf") {

    private val powerLaw = PowerLaw()
    private const val SEMINAR_COUNT = 10

    override fun generateData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until SEMINAR_COUNT).map {
            Seminar("$it", 20)
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

            Student(name = getRandomName(), preferences = prefList)
        }

        return students to seminars
    }
}