package hotkitchen

import hotkitchen.config.config
import hotkitchen.route.userRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 28888, host = "localhost") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    config()
    userRouting()
}