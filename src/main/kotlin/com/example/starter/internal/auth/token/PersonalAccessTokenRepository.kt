package com.example.starter.internal.auth.token

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

@ApplicationScoped
class PersonalAccessTokenRepository : PanacheRepositoryBase<PersonalAccessTokenEntity, Long> {
    fun listForUser(userId: Long): List<PersonalAccessTokenEntity> =
        list("userId = ?1 order by id desc", userId)

    fun findForUser(userId: Long, tokenId: Long): PersonalAccessTokenEntity? =
        find("userId = ?1 and id = ?2", userId, tokenId).firstResult()

    fun deleteExpiredOrOldRevoked(now: Instant, revokedBefore: Instant): Long =
        delete("expiresAt < ?1 or (revokedAt is not null and revokedAt < ?2)", now, revokedBefore)
}
