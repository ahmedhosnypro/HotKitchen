import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import org.junit.Test

class HotKitchenTest : StageTest<Any>() {

    @Serializable
    private data class Credentials(var email: String, var userType: String, var password: String)

    @Serializable
    private data class SignUpCredentials(var email: String, var password: String)

    @Serializable
    private data class Token(val token: String)

    private object Messages {
        const val invalidEmail = """{"status":"Invalid email"}"""
        const val invalidPassword = """{"status":"Invalid password"}"""
        const val userAlreadyExists = """{"status":"User already exists"}"""
        const val invalidEmailPassword = """{"status":"Invalid email or password"}"""
    }

    private val time = System.currentTimeMillis().toString()
    private val wrongEmails =
        arrayOf(
            "@example.com",
            time,
            "$time@gmail",
            "$time@mail@com",
            "$time.gmail",
            "$time.mail.ru",
            "$time@yandex.ru@why",
            "$time@yandex@ru.why",
            "@which$time@gmail.com",
            "$time@gmail",
            "$time#lala@mail.us",
            "Goose Smith <$time@example.com>",
            "$time@example.com (Duck Smith)"
        )
    private val wrongPasswords =
        arrayOf(
            "",
            "ad12",
            "ad124",
            "password",
            "0123456",
            "paaaaaaaaaaaasssssword",
            "11113123123123123"
        )
    private val jwtRegex = """^[a-zA-Z0-9]+?\.[a-zA-Z0-9]+?\..+""".toRegex()
    private val currentCredentials = Credentials("$time@mail.com", "client", "password$time")
    private lateinit var signInToken: String
    private lateinit var signUpToken: String


    @DynamicTest(order = 1)
    fun checkWrongEmail(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                for (email in wrongEmails) {
                    val response = client.post("/signup") {
                        setBody(Json.encodeToString(Credentials(email, "client", "password123")))
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    if (response.bodyAsText() != Messages.invalidEmail || response.status != HttpStatusCode.Forbidden) {
                        result = CheckResult.wrong("Invalid email is not handled correctly.\n" +
                                    "Wrong response message or status code.\n" +
                                    "$email is invalid email")
                    }
                }
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 2)
    fun checkWrongPassword(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                for (password in wrongPasswords) {
                    val response = client.post("/signup") {
                        setBody(Json.encodeToString(Credentials(currentCredentials.email, "client", password)))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    if (response.bodyAsText() != Messages.invalidPassword || response.status != HttpStatusCode.Forbidden)
                        result =
                            CheckResult.wrong("Invalid password is not handled correctly.\nWrong response message or status code.\n\"$password\" is invalid password")
                }
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 3)
    fun getSignInJWTToken(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                val response = client.post("/signup") {
                    setBody(Json.encodeToString(currentCredentials))
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                try {
                    val principal = Json.decodeFromString<Token>(response.bodyAsText() ?: "")
                    signInToken = principal.token
                    if (!signInToken.matches(jwtRegex) || signInToken.contains(currentCredentials.email))
                        result = CheckResult.wrong("Invalid JWT token")
                } catch (e: Exception) {
                    result = CheckResult.wrong("Cannot get token form /signin request")
                }
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 4)
    fun registerExistingUser(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                val response = client.post("/signup") {
                    setBody(Json.encodeToString(currentCredentials))
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                if (response.bodyAsText() != Messages.userAlreadyExists || response.status != HttpStatusCode.Forbidden)
                    result = CheckResult.wrong("An existing user is registered. Wrong response message or status code.")
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 5)
    fun wrongAuthorization(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                var response = client.post("/signin") {
                    setBody(
                        Json.encodeToString(
                            SignUpCredentials(
                                "why?does?this?email?exists",
                                currentCredentials.password
                            )
                        )
                    )
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                if (response.bodyAsText() != Messages.invalidEmailPassword || response.status != HttpStatusCode.Forbidden) {
                    result =
                        CheckResult.wrong("Error when authorizing a user using a wrong email. Wrong response message or status code.")
                    return@testApplication
                }

                response = client.post("/signin") {
                    setBody(Json.encodeToString(SignUpCredentials(currentCredentials.email, "completelyWrong123")))
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                if (response.bodyAsText() != Messages.invalidEmailPassword || response.status != HttpStatusCode.Forbidden)
                    result =
                        CheckResult.wrong("Error when authorizing a user using a wrong password. Wrong response message or status code.")
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 6)
    fun getSignUpJWTToken(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                val response = client.post("/signin") {
                    setBody(
                        Json.encodeToString(
                            SignUpCredentials(
                                currentCredentials.email,
                                currentCredentials.password
                            )
                        )
                    )
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                try {
                    val principal = Json.decodeFromString<Token>(response.bodyAsText() ?: "")
                    signUpToken = principal.token
                    if (!signUpToken.matches(jwtRegex) || signUpToken.contains(currentCredentials.email))
                        result = CheckResult.wrong("Invalid JWT token")
                } catch (e: Exception) {
                    result = CheckResult.wrong("Cannot get token form /signin request")
                }
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 7)
    fun wrongValidation(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                var response = client.get("/validate") {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer lala${(100..999).random()}.blo${(100..999).random()}blo.kek${(100..999).random()}"
                    )
                }
                if (response.status != HttpStatusCode.Unauthorized) {
                    result =
                        CheckResult.wrong("Wrong status code when authorizing with a completely wrong token using /validate")
                    return@testApplication
                }
                response = client.get("/validate") {
                    header(HttpHeaders.Authorization, signInToken)
                }
                if (response.status != HttpStatusCode.Unauthorized)
                    result =
                        CheckResult.wrong("Wrong status code when authorizing with a JWT token using /validate. " +
                                "Do you use \"Bearer\" in header?")
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }

    @DynamicTest(order = 8)
    fun correctValidation(): CheckResult {
        var result = CheckResult.correct()
        try {
            testApplication {
                var response = client.get("/validate") {
                    header(HttpHeaders.Authorization, "Bearer $signInToken")
                }
                if (response.status != HttpStatusCode.OK || response.bodyAsText()
                    != "Hello, ${currentCredentials.userType} ${currentCredentials.email}") {
                    result = CheckResult.wrong(
                        "Token validation with signin token failed.\nStatus code should be \"200 OK\"\n" +
                                "Message should be \"Hello, " +
                                "${currentCredentials.userType} ${currentCredentials.email}\""
                    )
                    return@testApplication
                }

                response = client.get("/validate") {
                    header(HttpHeaders.Authorization, "Bearer $signUpToken")
                }
                if (response.status != HttpStatusCode.OK
                    || response.bodyAsText() != "Hello, ${currentCredentials.userType} ${currentCredentials.email}"
                )
                    result = CheckResult.wrong(
                        "Token validation with signup token failed.\n" +
                                "Status code should be \"200 OK\"\nMessage should be \"Hello, " +
                                "${currentCredentials.userType} ${currentCredentials.email}\""
                    )
            }
        } catch (e: Exception) {
            result = CheckResult.wrong(e.message)
        }
        return result
    }
}