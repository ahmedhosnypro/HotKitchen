package hotkitchen

import hotkitchen.route.userRouting
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
fun main(){
    embeddedServer(Netty, port = 28888, host = "localhost") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    userRouting()
}