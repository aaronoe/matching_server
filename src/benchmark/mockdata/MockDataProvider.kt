package de.aaronoe.benchmark.mockdata

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

abstract class MockDataProvider(val name: String) {

    abstract fun generateData(): Pair<List<Student>, List<Seminar>>

}