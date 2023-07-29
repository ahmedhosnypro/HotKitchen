package entity

import org.jetbrains.exposed.dao.id.IntIdTable

object User : IntIdTable() {
    val email = varchar("email", 50).uniqueIndex()
    val userType = varchar("user_type", 50)
    val password = varchar("password", 50)
}