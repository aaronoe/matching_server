package de.aaronoe

import com.google.gson.GsonBuilder
import de.aaronoe.models.PostSeminar
import de.aaronoe.models.PostStudent
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import java.io.FileReader
import java.io.FileWriter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@ExperimentalCoroutinesApi
object Repository {

    data class AppData(
        val students: MutableList<Student> = mutableListOf(),
        val seminars: MutableList<Seminar> = mutableListOf()
    )

    var studentData by object : ReadWriteProperty<Any?, AppData> {

        private val gson = GsonBuilder().create()

        override fun getValue(thisRef: Any?, property: KProperty<*>): AppData {
            FileReader("${property.name}.json").use { reader ->
                return gson.fromJson(reader, AppData::class.java)
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: AppData) {
            FileWriter("${property.name}.json").use { writer ->
                gson.toJson(value, writer)
            }
        }
    }

    val channel = BroadcastChannel<AppData>(Channel.CONFLATED)

    suspend fun addStudent(student: PostStudent): AppData {
        return studentData.let {
            it.copy(students = it.students.apply { add(student.toStudent()) }).also {
                studentData = it
                channel.send(it)
            }
        }
    }

    suspend fun addSeminar(seminar: PostSeminar): AppData {
        return studentData.let {
            it.copy(seminars = it.seminars.apply { add(seminar.toSeminar()) }).also {
                studentData = it
                channel.send(it)
            }
        }
    }

    suspend fun deleteStudent(studentId: String): Boolean {
        return studentData.let {
            val newStudents = it.students
            val result = newStudents.removeIf { it.id == studentId }

            it.copy(students = newStudents).also {
                studentData = it
                channel.send(it)
            }
            result
        }
    }

    suspend fun deleteSeminar(seminarId: String): Boolean {
        return studentData.let {
            val newSeminars = it.seminars
            val result = newSeminars.removeIf { it.id == seminarId }

            it.copy(seminars = newSeminars).also {
                studentData = it
                channel.send(it)
            }
            result
        }
    }

}