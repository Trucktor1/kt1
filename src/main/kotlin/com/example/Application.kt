package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaskItem(
    val id: Int,
    val title: String,
    val description: String,
    val completed: Boolean
)

@Serializable
data class NewTaskPayload(
    val title: String,
    val description: String,
    val completed: Boolean = false
)

@Serializable
data class FailureInfo(
    val error: String
)

@Serializable
data class SuccessInfo(
    val message: String
)

private val taskStorage = mutableListOf(
    TaskItem(
        id = 10,
        title = "Освоить Kotlin",
        description = "Повторить ключевые возможности языка",
        completed = false
    ),
    TaskItem(
        id = 20,
        title = "Разобраться с Ktor",
        description = "Написать REST API средствами Ktor",
        completed = true
    )
)

private var currentId = 30

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = false
            }
        )
    }

    routing {

        get("/tasks") {

            val statusFilter =
                call.request.queryParameters["completed"]

            if (
                statusFilter != null &&
                statusFilter != "true" &&
                statusFilter != "false"
            ) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FailureInfo(
                        error = "Параметр 'completed' должен быть true или false"
                    )
                )
                return@get
            }

            val selectedTasks = if (statusFilter == null) {
                taskStorage
            } else {
                val isDone = statusFilter == "true"

                taskStorage.filter { element ->
                    element.completed == isDone
                }
            }

            call.respond(
                HttpStatusCode.OK,
                selectedTasks
            )
        }

        get("/tasks/{id}") {

            val taskId = call.parameters["id"]?.toIntOrNull()

            if (taskId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FailureInfo(
                        error = "Идентификатор задачи должен быть целым числом"
                    )
                )
                return@get
            }

            val foundTask = taskStorage.find { element ->
                element.id == taskId
            }

            if (foundTask == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    FailureInfo(
                        error = "Задача с идентификатором $taskId не найдена"
                    )
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                foundTask
            )
        }

        post("/tasks") {

            val payload = try {
                call.receive<NewTaskPayload>()
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FailureInfo(
                        error = "Некорректное JSON-тело запроса"
                    )
                )
                return@post
            }

            if (payload.title.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FailureInfo(
                        error = "Название не должно быть пустым"
                    )
                )
                return@post
            }

            val newTask = TaskItem(
                id = currentId++,
                title = payload.title,
                description = payload.description,
                completed = payload.completed
            )

            taskStorage.add(newTask)

            call.respond(
                HttpStatusCode.Created,
                newTask
            )
        }

        delete("/tasks/{id}") {

            val taskId = call.parameters["id"]?.toIntOrNull()

            if (taskId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FailureInfo(
                        error = "Идентификатор задачи должен быть целым числом"
                    )
                )
                return@delete
            }

            val foundTask = taskStorage.find { element ->
                element.id == taskId
            }

            if (foundTask == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    FailureInfo(
                        error = "Задача с идентификатором $taskId не найдена"
                    )
                )
                return@delete
            }

            taskStorage.remove(foundTask)

            call.respond(
                HttpStatusCode.OK,
                SuccessInfo(
                    message = "Задача с идентификатором $taskId успешно удалена"
                )
            )
        }
    }
}