package com.example.starter.application.auth

import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.NewAuthSession
import java.util.UUID

interface SessionManager {
    fun create(userId: Long, ipAddress: String?, userAgent: String?): NewAuthSession
    fun sessions(userId: Long): List<AuthSession>
    fun revoke(userId: Long, sessionId: UUID)
    fun revokeOthers(userId: Long, exceptSessionId: UUID)
}
