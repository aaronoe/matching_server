package de.aaronoe.algorithms

import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import java.util.*
import kotlin.random.Random

object MockData {

    fun getMediumSizedRandomData(): Pair<List<Student>, List<Seminar>> {
        val ids = Seminar("Electronic Identities", 40)
        val home = Seminar("Heimautomatisierung", 40)
        val debugging = Seminar("Debugging", 40)
        val one = Seminar("one", 40)
        val two = Seminar( "two", 40)
        val three = Seminar("three", 40)
        val four = Seminar( "four", 40)

        val seminars = listOf(ids, home, debugging, one, two, three, four)

        val students = (0 until seminars.sumBy { it.capacity }).map {
            Student(id = it.toString(), name = UUID.randomUUID().toString(), preferences = seminars.shuffled())
        }

        println("Student Preference Distribution:")
        students.groupBy { it.preferences.first() }.mapValues { it.value.size }.forEach { seminar, i ->
            println("${seminar.name} - $i")
        }

        return students to seminars
    }

    fun getVeryLargeRandomData(): Pair<List<Student>, List<Seminar>> {
        val seminars = (0 until 100).map {
            Seminar("$it", Random.nextInt(20, 40))
        }
        val totalCapacity = seminars.sumBy { it.capacity }

        val students = (0 until totalCapacity).map {
            Student("$it", "$it", seminars.shuffled().dropLast(90))
        }

        return students to seminars
    }

}