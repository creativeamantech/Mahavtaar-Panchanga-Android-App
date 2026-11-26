package com.mahavtaar.panchanga.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.EmailInviteRule
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.domain.model.UserSessionLock
import java.security.MessageDigest

internal fun DocumentSnapshot.estimatedMillis(field: String): Long =
    getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()
        ?.time
        ?: 0L

fun sha256EmailHash(email: String): String {
    val clean = email.trim().lowercase()
    val digest = MessageDigest.getInstance("SHA-256").digest(clean.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

@Keep
@IgnoreExtraProperties
data class FirestoreUserAccessDoc(
    val userId: String = "",
    val email: String = "",
    val displayName: String? = null,
    val role: String = "USER",
    val status: String = "PENDING",
    val allowedPages: List<String> = emptyList(),
    val allowedFeatures: List<String> = emptyList(),
    val authorizedByUid: String? = null,
    val inviteHash: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email.trim().lowercase(),
        "displayName" to displayName?.trim()?.takeIf { it.isNotEmpty() },
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "authorizedByUid" to authorizedByUid?.trim()?.takeIf { it.isNotEmpty() },
        "inviteHash" to inviteHash?.trim()?.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "email" to email.trim().lowercase(),
        "displayName" to displayName?.trim()?.takeIf { it.isNotEmpty() },
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "authorizedByUid" to authorizedByUid?.trim()?.takeIf { it.isNotEmpty() },
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun toDomain(snapshot: DocumentSnapshot): AccessPolicy {
        val parsedRole = UserRole.entries.firstOrNull { it.name.equals(role, ignoreCase = true) }
            ?: UserRole.USER
        val parsedStatus = AccessStatus.entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
            ?: AccessStatus.PENDING
        val pages = allowedPages.mapNotNull { AppPageKey.fromNameOrNull(it) }.toSet()
        val features = allowedFeatures.mapNotNull { AppFeatureKey.fromNameOrNull(it) }.toSet()
        return AccessPolicy(
            userId = userId.ifBlank { snapshot.id },
            email = email,
            displayName = displayName,
            role = parsedRole,
            status = parsedStatus,
            allowedPages = pages,
            allowedFeatures = features,
            authorizedByUid = authorizedByUid,
            updatedAtMs = snapshot.estimatedMillis("updatedAt")
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreEmailInviteDoc(
    val emailHash: String = "",
    val email: String = "",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val allowedPages: List<String> = emptyList(),
    val allowedFeatures: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "emailHash" to emailHash,
        "email" to email.trim().lowercase(),
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "createdByUid" to createdByUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "email" to email.trim().lowercase(),
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "createdByUid" to createdByUid,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toDomain(snapshot: DocumentSnapshot): EmailInviteRule {
        val parsedRole = UserRole.entries.firstOrNull { it.name.equals(role, ignoreCase = true) }
            ?: UserRole.USER
        val parsedStatus = AccessStatus.entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
            ?: AccessStatus.ACTIVE
        return EmailInviteRule(
            emailHash = emailHash.ifBlank { snapshot.id },
            email = email,
            role = parsedRole,
            status = parsedStatus,
            allowedPages = allowedPages.mapNotNull { AppPageKey.fromNameOrNull(it) }.toSet(),
            allowedFeatures = allowedFeatures.mapNotNull { AppFeatureKey.fromNameOrNull(it) }.toSet(),
            createdByUid = createdByUid,
            updatedAtMs = snapshot.estimatedMillis("updatedAt")
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreUserSessionDoc(
    val userId: String = "",
    val activeSessionToken: String = "",
    val deviceName: String = "",
    val forcedLogout: Boolean = false,
    @ServerTimestamp val claimedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toClaimCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "activeSessionToken" to activeSessionToken,
        "deviceName" to deviceName,
        "forcedLogout" to false,
        "claimedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toClaimUpdateMap(): Map<String, Any> = mapOf(
        "activeSessionToken" to activeSessionToken,
        "deviceName" to deviceName,
        "forcedLogout" to false,
        "claimedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toDomain(snapshot: DocumentSnapshot): UserSessionLock = UserSessionLock(
        userId = userId.ifBlank { snapshot.id },
        activeSessionToken = activeSessionToken,
        deviceName = deviceName,
        forcedLogout = forcedLogout,
        claimedAtMs = snapshot.estimatedMillis("claimedAt"),
        updatedAtMs = snapshot.estimatedMillis("updatedAt")
    )
}
