package hotkitchen.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import entity.InvalidCredentialsException
import entity.User
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction


const val secret = "secret"
const val issuer = "http://localhost:28888/"
const val audience = "http://localhost:28888/signin"
const val myRealm = "Access to 'signin'"

fun Application.config() {
    auth()
    install(StatusPages) { exceptionHandler() }
    install(ContentNegotiation) { json() }
    database()
}

fun Application.auth() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(
                JWT.require(Algorithm.HMAC256(secret)).withAudience(audience).withIssuer(issuer).build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
            }
        }
        basic("basic") {
            realm = myRealm
            validate { credentials ->
                transaction {
                    val user = User.findByEmail(credentials.name)
                    if (user == null || user[User.password] != credentials.password) throw InvalidCredentialsException("Invalid email or password")
                }
                UserIdPrincipal(credentials.name)
            }
        }
    }
}

fun database() {
    Database.connect("jdbc:postgresql://localhost:5332/HotKitchen", "org.postgresql.Driver", "postgres", "postgres")
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(User)
    }
}