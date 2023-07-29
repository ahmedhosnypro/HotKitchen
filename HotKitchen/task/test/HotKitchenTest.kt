import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult

class HotKitchenTest : StageTest<Any>() {

    private val email = System.currentTimeMillis().toString() + "jmail.com"
    private val password = (System.currentTimeMillis() % 100_000_000).toString()

    private val signedIn = """{"status":"Signed In"}"""
    private val signedUp = """{"status":"Signed Up"}"""
    private val registrationFailed = """{"status":"Registration failed"}"""
    private val authorizationFailed = """{"status":"Authorization failed"}"""
    private val currentCredentials = """{"email":"$email","userType":"testUser","password":"correct$password"}"""
    private val currentWrongSignIn = """{"email":"$email","password":"wrong$password"}"""
    private val currentCorrectSignIn = """{"email":"$email","password":"correct$password"}"""


    @DynamicTest(order = 1)
    fun registerNewUser(): CheckResult {
        try {
            withApplication(
                createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
            {
                with(handleRequest(HttpMethod.Post, "/signup") {
                    setBody(currentCredentials)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }) {
                    if (response.content != signedUp || response.status() != HttpStatusCode.OK)
                        return@withApplication CheckResult.wrong(
                            "Cannot register a new user. Wrong response message or status code."
                        )
                }
            }
        } catch (e: Exception) {
            return CheckResult.wrong(e.message)
        }
        return CheckResult.correct()
    }


    @DynamicTest(order = 2)
    fun registerExistingUser(): CheckResult {
        try {
            withApplication(
                createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
            {
                with(handleRequest(HttpMethod.Post, "/signup") {
                    setBody(currentCredentials)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }) {
                    if (response.content != registrationFailed || response.status() != HttpStatusCode.Forbidden)
                        return@withApplication CheckResult.wrong(
                            "An existing user is registered. Wrong response message or status code."
                        )
                }
            }
        } catch (e: Exception) {
            return CheckResult.wrong(e.message)
        }
        return CheckResult.correct()
    }

    @DynamicTest(order = 3)
    fun wrongAuthorization(): CheckResult {
        try {
            withApplication(
                createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
            {
                with(handleRequest(HttpMethod.Post, "/signin") {
                    setBody(currentWrongSignIn)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }) {
                    if (response.content != authorizationFailed || response.status() != HttpStatusCode.Forbidden)
                        return@withApplication CheckResult.wrong(
                            "Error when authorizing a user using a wrong password. Wrong response message or status code."
                        )
                }
            }
        } catch (e: Exception) {
            return CheckResult.wrong(e.message)
        }
        return CheckResult.correct()
    }

    @DynamicTest(order = 4)
    fun correctAuthorization(): CheckResult {
        try {
            withApplication(
                createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
            {
                with(handleRequest(HttpMethod.Post, "/signin") {
                    setBody(currentCorrectSignIn)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }) {
                    if (response.content != signedIn || response.status() != HttpStatusCode.OK)
                        return@withApplication CheckResult.wrong(
                        "Error when authorizing a user using a correct password. Wrong response message or status code."
                    )
                }
            }
        } catch (e: Exception) {
            return CheckResult.wrong(e.message)
        }
        return CheckResult.correct()
    }
}