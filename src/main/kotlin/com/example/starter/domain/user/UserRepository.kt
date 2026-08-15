package com.example.starter.domain.user

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity? =
        find("lower(email) = ?1", email.lowercase()).firstResult()

    fun findActiveById(id: Long): UserEntity? =
        find("id = ?1 and active = true", id).firstResult()

    fun listByName(): List<UserEntity> = list("order by name asc, id asc")
}
