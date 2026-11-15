package com.uzuu.letplay.domain.model

enum class VoteValue(val score: Int, val label: String) {
    PASS(0, "Để dịp khác"),
    CAN_JOIN(1, "Đi cùng được"),
    VERY_WANT(2, "Rất muốn");

    companion object {
        fun fromScore(score: Int): VoteValue = when (score) {
            0 -> PASS
            1 -> CAN_JOIN
            2 -> VERY_WANT
            else -> throw IllegalArgumentException("Invalid vote score: $score. Must be 0, 1, or 2.")
        }
    }
}

enum class ProposalCategory {
    FOOD,
    BOOK,
    CAMERA,
    COFFEE,
    PIN;

    companion object {
        fun fromString(value: String): ProposalCategory = when (value.lowercase()) {
            "food" -> FOOD
            "book" -> BOOK
            "camera" -> CAMERA
            "coffee" -> COFFEE
            "pin" -> PIN
            else -> PIN
        }
    }
}

data class Proposal(
    val id: ProposalId,
    val title: String,
    val description: String = "",
    val estimatedCostPerPerson: MoneyVnd? = null,
    val durationMinutes: Int? = null,
    val category: ProposalCategory = ProposalCategory.PIN,
    val creatorMembershipId: MembershipId? = null,
    val authorName: String = "",
    val isCreatedByMe: Boolean = false,
    val isCompleted: Boolean = false,
    val scheduledTime: String = "",
    val location: String = "",
    val baseVotes: List<Int> = listOf(0, 0, 0)
) {
    init {
        require(title.isNotBlank()) { "Proposal title must not be blank" }
        estimatedCostPerPerson?.let {
            require(it.amount in 0L..100_000_000L) {
                "Estimated cost must be between 0 and 100,000,000 VND, got ${it.amount}"
            }
        }
        durationMinutes?.let {
            require(it > 0) { "Duration must be positive, got $it" }
        }
    }
}

data class PlanDraft(
    val selectedProposals: List<ProposalId> = emptyList(),
    val orderedProposals: List<ProposalId> = emptyList(),
    val draftStartTime: String = "18:00",
    val orderBallots: Map<MembershipId, List<ProposalId>> = emptyMap()
)

data class PublishedPlan(
    val version: Int,
    val orderedProposals: List<ProposalId>,
    val startTime: String = "18:00",
    val completedProposals: Set<ProposalId> = emptySet(),
    val reason: String = "",
    val publishedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(version > 0) { "Plan version must be positive: $version" }
        require(orderedProposals.isNotEmpty()) { "Published plan must contain at least one stop" }
    }
}

data class PlanSnapshot(
    val version: Int,
    val orderedProposals: List<ProposalId>,
    val startTime: String,
    val completedProposals: Set<ProposalId>,
    val reason: String,
    val savedAtMillis: Long,
    val proposalsSnapshot: List<Proposal> = emptyList()
)
