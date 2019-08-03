package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import org.jgrapht.alg.matching.HopcroftKarpMaximumCardinalityBipartiteMatching
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph

object PopularChaAlgorithm : StudentMatchingAlgorithm {

    override suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        data class MapResult(
            val students: List<Student>,
            val seminar: Seminar,
            val hasCapacityLeft: Boolean
        ) {

            override fun toString(): String {
                return "${seminar.name} - $hasCapacityLeft"
            }
        }

        val houseCapacityOverview = students.groupBy { it.preferences.first() }

        val matchedStudents = houseCapacityOverview
            .mapValues { MapResult(it.value, it.key, it.value.count() <= it.key.capacity) }
            .filter { it.value.hasCapacityLeft }
            .flatMap { it.value.students }
            .also {
                it.forEach { student ->
                    student.match = student.preferences.first().also {
                        it.assignments.add(student)
                    }
                }
            }

        val unmatchedStudents = students - matchedStudents
        val availableSeminars = seminars.filter(Seminar::canAssignMore).toHashSet()

        val graph = SimpleGraph<String, DefaultEdge>(DefaultEdge::class.java)

        unmatchedStudents.forEach {
            graph.addVertex(it.id)
        }

        // map of new id's with suffix to original seminar
        val idSeminarMap = mutableMapOf<String, Seminar>()

        val seminarMap = availableSeminars.map { seminar ->
            seminar to (0 until (seminar.capacity - seminar.assignments.size)).map {
                val newId = "${seminar.id}$$it"
                idSeminarMap[newId] = seminar
                seminar.copy(id = newId)
            }
        }.toMap().toMutableMap()

        seminarMap.forEach { (_, list) ->
            list.forEach {
                graph.addVertex(it.id)
            }
        }

        // create f and s houses for students
        val studentPrefs = unmatchedStudents.map {
            val fHouse = it.preferences.first()
            val sHouse = it.preferences.firstOrNull {
                it != fHouse && houseCapacityOverview[it]!!.size < it.capacity
            } ?: Seminar("l-house_${it.id}", 1).also {
                seminarMap[it] = listOf(it) // last resort house
                idSeminarMap[it.id] = it
                graph.addVertex(it.id)
            }

            it to (fHouse to sHouse)
        }

        studentPrefs.forEach { (student, prefs) ->
            prefs.toList().forEach {
                seminarMap.getValue(it).forEach {
                    graph.addEdge(student.id, it.id)
                }
            }
        }

        val algorithm = HopcroftKarpMaximumCardinalityBipartiteMatching<String, DefaultEdge>(
            graph,
            unmatchedStudents.map { it.id }.toSet(),
            seminarMap.flatMap { it.value.map { it.id } }.toSet()
        )

        val studentMap = unmatchedStudents.associateBy { it.id }

        val matching = algorithm.matching
        println("IsPerfect: ${matching.edges.size == unmatchedStudents.size}")
        matching.edges.forEach { edge ->
            val student = studentMap[graph.getEdgeSource(edge)]!!
            val seminar = idSeminarMap[graph.getEdgeTarget(edge)]!!

            seminar.assignments.add(student)
        }

        students.forEach { student ->
            val match = seminars.firstOrNull { it.assignments.contains(student) }
            val index = student.preferences.indexOf(match)
            (0 until index).forEach {
                if (student.preferences[it].canAssignMore) {
                    println("Opt possible")
                }
            }
        }
        println("Done Opt")

        return seminars.map { it to it.assignments.toList() }.toMap()
    }

}