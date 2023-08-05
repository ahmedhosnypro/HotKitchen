package entity

import hotkitchen.route.NewUserDTO
import hotkitchen.route.UserDTO
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object User : IntIdTable() {
    val email = varchar("email", 50).uniqueIndex()
    val userType = varchar("user_type", 50)
    val password = varchar("password", 50)
    val token = text("token")

    fun insert(newUserDTO: NewUserDTO, token: String) {
        val user = transaction {
            User.select { User.email eq newUserDTO.email }.firstOrNull()
        }
        if (user != null) throw UserAlreadyExistsException("User already exists")

        transaction {
            User.insert {
                it[User.email] = newUserDTO.email
                it[User.userType] = newUserDTO.userType
                it[User.password] = newUserDTO.password
                it[User.token] = token
            }
        }
    }

    fun signin(userDTO: UserDTO, token: String) {
        val user = transaction {
            User.select { User.email eq userDTO.email }.firstOrNull()
        }

        if ((user == null) || (user[password] != userDTO.password)) throw InvalidCredentialsException("Invalid email or password")

        transaction {
            User.update({ User.email eq userDTO.email }) {
                it[User.token] = token
            }
        }
    }

    fun validate(email: String, password: String): Boolean {
        findByEmail(email) ?: return false
        return transaction {
            User.select { User.email eq email }.firstOrNull()?.get(User.password) == password
        }
    }

    fun findByEmail(email: String): ResultRow? {
        return transaction {
            User.select { User.email eq email }.firstOrNull()
        }

    }
}

class UserAlreadyExistsException(message: String) : Exception(message)
class InvalidCredentialsException(message: String) : Exception(message)