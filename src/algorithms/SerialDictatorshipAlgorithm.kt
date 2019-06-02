package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student

object SerialDictatorshipAlgorithm: StudentMatchingAlgorithm {

    override suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        students.forEach { student ->
            for (seminar in student.preferences) {
                if (seminar.canAssignMore) {
                    seminar.assignments.add(student)
                    student.match = seminar
                    break
                }
            }
        }

        return students.filter { it.match != null }.groupBy { it.match!! }
    }

}