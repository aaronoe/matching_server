package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import kotlin.system.measureTimeMillis

object HungarianAlgorithm: StudentMatchingAlgorithm {

    override suspend fun execute(students: List<Student>, seminars: List<Seminar>): Map<Seminar, List<Student>> {
        val studentCount = students.count()
        val seminarSeatCount = seminars.sumBy { it.capacity }

        val studentList = students.toMutableList()
        val seminarList = seminars.toMutableList()

        when {
            studentCount > seminarSeatCount -> {
                val difference = Math.abs(studentCount - seminarSeatCount)
                seminarList.add(Seminar("mock", difference))
            }
            studentCount < seminarSeatCount -> {
                val difference = Math.abs(studentCount - seminarSeatCount)
                (0 until difference)
                    .map { Student(name = "mock$it", preferences = emptyList()) }
                    .let(studentList::addAll)
            }
        }

        val seminarMap = seminarList.associateWith { seminar ->
            (0 until seminar.capacity).map {
                Seminar("${seminar.name}_$it", id = "${seminar.id}$$it", capacity = 1)
            }
        }

        val graph = SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

        seminarMap
            .flatMap { it.value }
            .forEach {
                graph.addVertex(it.id)
            }

        val allSeminarsSet = seminarMap.flatMap { it.value }.toHashSet()

        measureTimeMillis {
            studentList.forEach { student ->
                graph.addVertex(student.id)
                val preferenceMap = HashMap<String, Int>(allSeminarsSet.size)

                student.preferences.forEachIndexed { index, seminar ->
                    val list = seminarMap[seminar] ?: error("Seminar List should exist")

                    list.forEach {
                        preferenceMap[it.id] = index
                    }
                }

                allSeminarsSet.forEach {
                    val weight = preferenceMap[it.id]?.toDouble() ?: Double.MAX_VALUE

                    graph.addEdge(student.id, it.id).let {
                        graph.setEdgeWeight(it, weight)
                    }
                }
            }
        }.let {
            println("Creating Graph: $it ms")
        }

        val algorithm = KuhnMunkresMinimalWeightBipartitePerfectMatching(
            graph,
            studentList.map { it.id }.toSet(),
            seminarMap.flatMap { it.value }.map { it.id }.toSet()
        )

        val studentMap = studentList.associateBy { it.id }
        val seminarIdMap = seminarList.associateBy { it.id }

        val matching = algorithm.matching
        println("IsPerfect: ${matching.edges.size == studentList.size}")
        matching.edges.forEach { edge ->
            val student = studentMap[graph.getEdgeSource(edge)]!!
            val seminarId = graph.getEdgeTarget(edge)!!.split("$").first()
            val seminar = seminarIdMap[seminarId]!!

            seminar.assignments.add(student)
        }

        return seminars.map { it to it.assignments.toList() }.toMap()
    }

}