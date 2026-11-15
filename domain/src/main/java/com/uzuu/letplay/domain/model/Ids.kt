package com.uzuu.letplay.domain.model

@JvmInline
value class LocalRoomId(val value: String) {
    init {
        require(value.isNotBlank()) { "LocalRoomId must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class LocalRoomCode(val value: String) {
    init {
        require(value.isNotBlank()) { "LocalRoomCode must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class OnlineRoomId(val value: String) {
    init {
        require(value.isNotBlank()) { "OnlineRoomId must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class MembershipId(val value: String) {
    init {
        require(value.isNotBlank()) { "MembershipId must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class AccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountId must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ProposalId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProposalId must not be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class LocalProposalId(val value: Long) {
    init {
        require(value >= 0) { "LocalProposalId must be non-negative: $value" }
    }
    override fun toString(): String = value.toString()
}

fun LocalProposalId.toProposalId(): ProposalId = ProposalId(value.toString())
fun ProposalId.toLocalProposalId(): LocalProposalId? = value.toLongOrNull()?.let { LocalProposalId(it) }
