package de.aaronoe.benchmark.mockdata

import com.github.javafaker.Faker
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

abstract class MockDataProvider(val name: String) {

    private val nameGenerator = Faker()

    abstract fun generateData(): Pair<List<Student>, List<Seminar>>

    fun getRandomName() = nameGenerator.name().fullName()

}