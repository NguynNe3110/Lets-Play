package com.uzuu.letplay.domain.model

enum class RoomLifecycle {
    ACTIVE,
    ENDED
}

enum class LegacyRoomMode {
    LEGACY_SINGLE,
    GROUP_ITINERARY
}

enum class MemberRole {
    HOST,
    MEMBER
}

data class Member(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val legacyIndex: Int? = null
)

data class OnlineMember(
    val id: MembershipId,
    val displayName: String,
    val role: MemberRole = MemberRole.MEMBER,
    val isOnline: Boolean = false,
    val accountId: AccountId? = null
) {
    val isHost: Boolean get() = role == MemberRole.HOST
}
