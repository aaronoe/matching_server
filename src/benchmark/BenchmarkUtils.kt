package de.aaronoe.benchmark

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import java.io.StringWriter

private const val PREFIX_META_DATA = 'd'
private const val PREFIX_SEMINAR = 't'
private const val PREFIX_STUDENT = 's'

enum class PopularityResult {
    MORE, EQUAL, LESS
}

fun Pair<List<Student>, List<Seminar>>.deepCopy(): Pair<List<Student>, List<Seminar>> {
    val seminarMap = this.second.map { it.copy() }.associateBy { it.id }

    val students = this.first.map { it.copy(preferences = it.preferences.map { seminarMap.get(it.id)!! }) }
    val seminars = seminarMap.values.toList()

    return students to seminars
}

fun formatDataToInput(students: List<Student>, seminars: List<Seminar>) = with(StringWriter()) {
    var id = 0
    val seminarIdMap = seminars.associateBy({ it.id }, { id++ })

    appendln("$PREFIX_META_DATA ${seminars.size} ${students.size}")

    seminars.forEachIndexed { index, seminar ->
        appendln("$PREFIX_SEMINAR $index ${seminar.capacity}")
    }

    students.forEachIndexed { index, student ->
        append("$PREFIX_STUDENT $index ${student.preferences.size}")
        student.preferences.forEach { seminar ->
            val mappedId = seminarIdMap.getValue(seminar.id)

            append(" $mappedId")
        }
        appendln()
    }

    toString()
}


infix fun Result.isMorePopularThan(other: Result): PopularityResult {
    val first = matching.getPreferenceIndexMap()
    val second = other.matching.getPreferenceIndexMap()

    val allStudents = first.entries.map { it.key }.union(second.entries.map { it.key })

    val comparisons = allStudents.map {
        val firstIndex = first[it] ?: Int.MAX_VALUE
        val secondIndex = second[it] ?: Int.MAX_VALUE

        firstIndex.compareTo(secondIndex)
    }.groupBy { it }

    val greater = comparisons[1]?.size ?: 0
    val smaller = comparisons[-1]?.size ?: 0

    return when {
        greater < smaller -> PopularityResult.MORE
        greater > smaller -> PopularityResult.LESS
        else -> PopularityResult.EQUAL
    }
}

private fun Map<Seminar, List<Student>>.getPreferenceIndexMap(): Map<Student, Int> {
    return flatMap { (seminar, students) ->
        students.map { student -> student to student.preferences.indexOf(seminar) }
    }.toMap()
}