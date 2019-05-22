package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import org.jgrapht.alg.matching.HopcroftKarpMaximumCardinalityBipartiteMatching
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph

object PopularChaAlgorithm : StudentMatchingAlgorithm {

    override fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        data class MapResult(
            val students: List<Student>,
            val seminar: Seminar,
            val hasCapacityLeft: Boolean
        ) {

            override fun toString(): String {
                return "${seminar.name} - $hasCapacityLeft"
            }
        }

        val matchedStudents = students
            .groupBy { it.preferences.first() }
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
        val availableSeminars = unmatchedStudents.flatMap { it.preferences.filter { it.canAssignMore } }.toHashSet()

        val graph = SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

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
        }.toMap()

        seminarMap.forEach { (_, list) ->
            list.forEach {
                graph.addVertex(it.id)
            }
        }

        unmatchedStudents.forEach { student ->
            student.preferences.forEachIndexed { index, seminar ->
                seminarMap[seminar]?.forEach {
                    graph.addEdge(student.id, it.id).also {
                        graph.setEdgeWeight(it, index.toDouble())
                    }
                }
            }
        }

        val algorithm = HopcroftKarpMaximumCardinalityBipartiteMatching<String, DefaultWeightedEdge>(
            graph,
            unmatchedStudents.map { it.id }.toSet(),
            seminarMap.flatMap { it.value.map { it.id } }.toSet()
        )

        val studentMap = unmatchedStudents.associateBy { it.id }

        algorithm.matching.edges.forEach { edge ->
            val student = studentMap[graph.getEdgeSource(edge)]!!
            val seminar = idSeminarMap[graph.getEdgeTarget(edge)]!!

            seminar.assignments.add(student)
        }

        return seminars.map { it to it.assignments.toList() }.toMap()
    }

}