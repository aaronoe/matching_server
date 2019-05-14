package de.aaronoe

import com.google.gson.Gson
import de.aaronoe.algorithms.AaronPopularityAlgorithm
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
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.mapNotNull

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@UseExperimental(ObsoleteCoroutinesApi::class)
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
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
        post("/students") {
            val newStudent = call.receive<PostStudent>()

            call.respond(Repository.addStudent(newStudent))
        }

        post("/seminars") {
            val newSeminar = call.receive<PostSeminar>()

            call.respond(Repository.addSeminar(newSeminar))
        }

        get("/match") {
            val (students, seminars) = Repository.getCopiedStudentData()
            val result = AaronPopularityAlgorithm.execute(students, seminars)

            result.map(Matching.Companion::fromMapEntry).let {
                call.respond(it)
            }
        }

        get("/download") {
            Repository.getDataFile()?.let {
                call.response.header("Content-Disposition", "attachment; filename=\"${it.name}\"")
                call.respondFile(it)
            } ?: call.respond(status = HttpStatusCode.NotFound, message = "Not found")
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

