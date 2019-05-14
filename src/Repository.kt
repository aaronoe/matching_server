package de.aaronoe

import com.google.gson.GsonBuilder
import de.aaronoe.models.PostSeminar
import de.aaronoe.models.PostStudent
import de.aaronoe.models.Seminar
import de.aaronoe.models.Student
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import java.io.File
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

    fun getDataFile(): File? {
        return File("${::studentData.name}.json").let {
            if (it.exists()) it else null
        }
    }

    fun getCopiedStudentData(): AppData {
        return studentData.let {
            val seminarMap = it.seminars.map { it.copy() }.associateBy { it.id }

            val students = it.students.map { it.copy(preferences = it.preferences.map { seminarMap.get(it.id)!! }) }
            val seminars = seminarMap.values
            it.copy(students.toMutableList(), seminars.toMutableList())
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

            val updatedStudents = it.students.map { it.copy(preferences = it.preferences.toMutableList().apply {
                removeAll { it.id == seminarId }
            })}

            it.copy(seminars = newSeminars, students = updatedStudents.toMutableList()).also {
                studentData = it
                channel.send(it)
            }
            result
        }
    }

}