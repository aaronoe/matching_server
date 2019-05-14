package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

interface StudentMatchingAlgorithm {

    fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>>

}