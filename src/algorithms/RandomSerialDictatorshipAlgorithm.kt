package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object RandomSerialDictatorshipAlgorithm: StudentMatchingAlgorithm {

    override fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        return SerialDictatorshipAlgorithm.execute(students.shuffled(), seminars)
    }

}