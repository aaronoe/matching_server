package de.aaronoe.models

data class Matching(val seminar: MatchedSeminar, val students: List<MatchedStudent>) {

    data class MatchedSeminar(val id: String, val name: String, val capacity: Int)

    data class MatchedStudent(val id: String, val name: String)

    companion object {
        fun fromMapEntry(entry: Map.Entry<Seminar, List<Student>>): Matching {
            val (seminar, students) = entry

            return Matching(
                seminar = MatchedSeminar(seminar.id, seminar.name, seminar.capacity),
                students = students.map { MatchedStudent(it.id, it.name) }
            )
        }
    }

}