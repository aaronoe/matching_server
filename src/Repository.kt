package de.aaronoe

import com.google.gson.GsonBuilder
import de.aaronoe.benchmark.mockdata.MockDataProvider
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
        private val file = File("studentData.json").also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): AppData {
            FileReader("${property.name}.json").use { reader ->
                return gson.fromJson(reader, AppData::class.java) ?: AppData()
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

    suspend fun updateStudent(studentId: String, updated: PostStudent): Student? {
        return studentData.let {
            val index = it.students.indexOfFirst { it.id == studentId }
            if (index == -1) return null

            val newStudent = Student(id = studentId, name = updated.name, preferences = updated.preferences)
            val mutableStudents = it.students.toMutableList()
            mutableStudents[index] = newStudent

            it.copy(students = mutableStudents).also {
                studentData = it
                channel.send(it)
            }

            newStudent
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

    suspend fun updateSeminar(seminarId: String, updated: PostSeminar): Seminar? {
        return studentData.let {
            val index = it.seminars.indexOfFirst { it.id == seminarId }
            if (index == -1) return null

            val newSeminar = Seminar(id = seminarId, name = updated.name, capacity = updated.capacity)
            val mutableSeminars = it.seminars.toMutableList()
            mutableSeminars[index] = newSeminar

            it.copy(seminars = mutableSeminars).also {
                studentData = it
                channel.send(it)
            }

            newSeminar
        }
    }

    suspend fun changeDataset(provider: MockDataProvider) {
        val (students, seminars) = provider.generateData()
        val data = AppData(students.toMutableList(), seminars.toMutableList())

        studentData = data
        channel.send(data)
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

    suspend fun setData(newData: AppData) {
        studentData = newData
        channel.send(newData)
    }

}