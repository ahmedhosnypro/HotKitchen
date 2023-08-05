package hotkitchen.config

import entity.InvalidCredentialsException
import entity.UserAlreadyExistsException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val UNKNOWN_ERROR = "Unknown error"
fun StatusPagesConfig.exceptionHandler() {
    exception<UserAlreadyExistsException> { call, cause ->
        call.respondText(ContentType.Application.Json, HttpStatusCode.Forbidden) {
            Json.encodeToString(mapOf("status" to (cause.message ?: UNKNOWN_ERROR)))
        }
    }

    exception<InvalidCredentialsException> { call, cause ->
        call.respondText(ContentType.Application.Json, HttpStatusCode.Forbidden) {
            Json.encodeToString(mapOf("status" to (cause.message ?: UNKNOWN_ERROR)))
        }
    }
}