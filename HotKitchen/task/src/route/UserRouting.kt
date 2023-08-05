package hotkitchen.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import entity.User
import hotkitchen.config.audience
import hotkitchen.config.issuer
import hotkitchen.config.secret
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

const val INVALID_PASSWORD = "Invalid password"
const val INVALID_EMAIL = "Invalid email"
const val INVALID_EMAIL_PASSWORD = "Invalid email or password"
const val EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"

const val PASSWORD_REGEX = "^(?=.+[A-Za-z])(?=.+\\d)[A-Za-z\\d]{6,}\$"

@Serializable
data class UserDTO(
    val email: String, val password: String
)

@Serializable
data class NewUserDTO(
    val email: String, val userType: String, val password: String
)


fun Application.userRouting() {
    routing {
        post("/signup") {
            val newUser = call.receive<NewUserDTO>()
            if (newUser.email.matches(Regex(EMAIL_REGEX)).not()) {
                call.respondText(status = HttpStatusCode.Forbidden) {
                    Json.encodeToString(mapOf("status" to INVALID_EMAIL))
                }
                return@post
            }
            if (newUser.password.matches(Regex(PASSWORD_REGEX)).not()) {
                call.respondText(status = HttpStatusCode.Forbidden) {
                    Json.encodeToString(mapOf("status" to INVALID_PASSWORD))
                }
                return@post
            }

            val token = createToken(newUser.email)
            User.insert(newUser, token)
            call.respondText { Json.encodeToString(mapOf("token" to token)) }
        }

        post("/signin") {
            val user = call.receive<UserDTO>()
            if (user.email.matches(Regex(EMAIL_REGEX)).not() || user.password.matches(Regex(PASSWORD_REGEX)).not()) {
                call.respondText(status = HttpStatusCode.Forbidden) {
                    Json.encodeToString(mapOf("status" to INVALID_EMAIL_PASSWORD))
                }
                return@post
            }

            val token = createToken(user.email)
            User.signin(user, token)
            call.respondText { Json.encodeToString(mapOf("token" to token)) }
        }

        authenticate("auth-jwt") {
            get("/validate") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal!!.payload.getClaim("username").asString()
                val userType = User.findByEmail(email)!![User.userType]
                call.respondText { "Hello, $userType $email" }
            }
        }
    }
}

fun createToken(username: String): String =
    JWT.create().withAudience(audience).withIssuer(issuer).withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000000)).sign(Algorithm.HMAC256(secret))
