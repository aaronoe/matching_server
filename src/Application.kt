package de.aaronoe

import com.google.gson.Gson
import de.aaronoe.DatasetType.*
import de.aaronoe.algorithms.StudentMatchingAlgorithm
import de.aaronoe.algorithms.cpp.*
import de.aaronoe.benchmark.getStatistics
import de.aaronoe.benchmark.mockdata.LargeMockDataProvider
import de.aaronoe.benchmark.mockdata.MediumMockDataProvider
import de.aaronoe.benchmark.mockdata.PrefLibDataProvider
import de.aaronoe.benchmark.mockdata.ZipfMockDataProvider
import de.aaronoe.models.MatchResponse
import de.aaronoe.models.Matching
import de.aaronoe.models.PostSeminar
import de.aaronoe.models.PostStudent
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.mapNotNull

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {
            serializeSpecialFloatingPointValues()
        }
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            cause.printStackTrace()
            call.respond(listOf("There was an error processing your request!", cause.localizedMessage))
        }
    }

    val gson = Gson()

    install(DefaultHeaders)
    install(WebSockets)
    install(CORS) {
        anyHost()
        header(HttpHeaders.Allow)
        header(HttpHeaders.AccessControlAllowOrigin)
        method(HttpMethod.Delete)
    }

    routing {
        get("/hello") {
            call.respondText(text = "Hello World")
        }

        post("/students") {
            val newStudent = call.receive<PostStudent>()

            call.respond(Repository.addStudent(newStudent))
        }

        post("/students/{student_id}") {
            val studentId = call.parameters["student_id"] ?: throw IllegalStateException("Student ID should be supplied")

            val newStudent = call.receive<PostStudent>()
            val result = Repository.updateStudent(studentId, newStudent)
            if (result == null) {
                call.respondText(text = "Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(result)
            }
        }

        post("/seminars") {
            val newSeminar = call.receive<PostSeminar>()

            call.respond(Repository.addSeminar(newSeminar))
        }

        post("/seminars/{seminar_id}") {
            val seminarId = call.parameters["seminar_id"] ?: throw IllegalStateException("Seminar ID should be supplied")

            val newSeminar = call.receive<PostSeminar>()
            val result = Repository.updateSeminar(seminarId, newSeminar)
            if (result == null) {
                call.respondText(text = "Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(result)
            }
        }

        get("/match/{mechanism}") {
            val algorithm: StudentMatchingAlgorithm = when (call.parameters["mechanism"]) {
                "rsd" -> CppRsd
                "popular" -> CppPopular
                "popular-mod" -> CppPopularModified
                "hungarian" -> CppHungarian
                "max-pareto" -> CppMaxPareto
                else -> throw IllegalArgumentException("Unknown algorithm")
            }

            val (students, seminars) = Repository.getCopiedStudentData()
            val (result, runtime) = algorithm.executeWithTimestamp(students, seminars)
            val stats = getStatistics(result, students, seminars).cleanNans()

            result.map(Matching.Companion::fromMapEntry).sortedBy { it.seminar.name }.let {
                call.respond(MatchResponse(it, stats, runtime))
            }
        }

        get("/download") {
            Repository.getDataFile()?.let {
                call.response.header("Content-Disposition", "attachment; filename=\"${it.name}\"")
                call.respondFile(it)
            } ?: call.respond(status = HttpStatusCode.NotFound, message = "Not found")
        }

        post("/file") {
            val payload = call.receiveText()
            val json = payload.split("\n")[4]
            val parsed = gson.fromJson<Repository.AppData>(json, Repository.AppData::class.java)
            Repository.setData(parsed)
        }

        post("/dataset") {
            val type = call.receive<DatasetPayload>()

            when (type.name) {
                PrefLib1 -> Repository.changeDataset(PrefLibDataProvider.prefLib1)
                PrefLib2 -> Repository.changeDataset(PrefLibDataProvider.prefLib2)
                Zipfian -> Repository.changeDataset(ZipfMockDataProvider)
                Uniform -> Repository.changeDataset(MediumMockDataProvider)
                Custom -> throw IllegalArgumentException("Use /file endpoint for uploading custom data")
            }

            call.respondText(status = HttpStatusCode.OK, text = "OK")
        }

        delete("/students/{student_id}") {
            val id = requireNotNull(call.parameters["student_id"])

            val result = Repository.deleteStudent(id)
            val status = if (result) HttpStatusCode.OK else HttpStatusCode.NotFound
            call.respondText(status = status, text = "Done")
        }

        delete("/seminars/{seminar_id}") {
            val id = requireNotNull(call.parameters["seminar_id"])

            val result = Repository.deleteSeminar(id)
            val status = if (result) HttpStatusCode.OK else HttpStatusCode.NotFound
            call.respondText(status = status, text = "Done")
        }

        webSocket("/") {
            println("New connection")
            outgoing.send(Frame.Text(gson.toJson(Repository.studentData)))
            Repository.channel.consumeEach {
                println("new event size: ${it.students.size}")
                outgoing.send(Frame.Text(gson.toJson(it)))
            }
            incoming.mapNotNull { it as? Frame.Text }.consumeEach { frame ->
                val text = frame.readText()
                outgoing.send(Frame.Text("YOU SAID $text"))
                println("Incoming: $text")
                if (text.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }
}

data class DatasetPayload(val name: DatasetType)

enum class DatasetType {
    PrefLib1,
    PrefLib2,
    Zipfian,
    Uniform,
    Custom
}