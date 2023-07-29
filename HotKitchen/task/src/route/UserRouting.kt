package hotkitchen.route

import entity.User
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class UserDTO(
    val email: String, val password: String
)

@Serializable
data class NewUserDTO(
    val email: String,
    val userType: String,
    val password: String,
)

fun Application.userRouting() {
    Database.connect("jdbc:postgresql://localhost:5332/HotKitchen", "org.postgresql.Driver", "postgres", "postgres")
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(User)
    }
    routing {
        post("/signup") {
            val user = Json.decodeFromString(NewUserDTO.serializer(), call.receiveText())

            try {
                transaction {
                    User.insert {
                        it[email] = user.email
                        it[userType] = user.userType
                        it[password] = user.password
                    }
                }
                call.respondText { Json.encodeToString(mapOf("status" to "Signed Up")) }
            } catch (e: Exception) {
                call.respondText(status = HttpStatusCode.Forbidden) {
                    Json.encodeToString(mapOf("status" to "Registration failed"))
                }
            }
        }

        post("/signin") {
            val userDTO = Json.decodeFromString(UserDTO.serializer(), call.receiveText())
            try {
                transaction {
                    User.select {
                        (User.email eq userDTO.email) and (User.password eq userDTO.password)
                    }.firstOrNull() ?: throw Exception("Authorization failed")
                }
                call.respondText { Json.encodeToString(mapOf("status" to "Signed In")) }
            } catch (e: Exception) {
                call.respondText(status = HttpStatusCode.Forbidden) {
                    Json.encodeToString(mapOf("status" to e.message))
                }
            }
        }
    }
}