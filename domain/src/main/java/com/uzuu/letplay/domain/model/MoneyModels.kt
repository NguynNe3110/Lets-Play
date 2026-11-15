package com.uzuu.letplay.domain.model

enum class SettlementStatus {
    PENDING,
    CONFIRMED,
    REJECTED;

    companion object {
        fun fromString(value: String): SettlementStatus = when (value.lowercase()) {
            "pending" -> PENDING
            "confirmed" -> CONFIRMED
            "rejected" -> REJECTED
            else -> PENDING
        }
    }
}

data class Allocation(
    val membershipId: MembershipId,
    val displayName: String = "",
    val amount: MoneyVnd
)

data class Expense(
    val id: String,
    val title: String,
    val totalAmount: MoneyVnd,
    val note: String = "",
    val payers: List<Allocation>,
    val shares: List<Allocation>,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    init {
        require(title.isNotBlank()) { "Expense title must not be blank" }
        require(payers.isNotEmpty()) { "Expense must have at least one payer" }
        require(shares.isNotEmpty()) { "Expense must have at least one share" }

        val payerSum = payers.fold(0L) { acc, p -> Math.addExact(acc, p.amount.amount) }
        val shareSum = shares.fold(0L) { acc, s -> Math.addExact(acc, s.amount.amount) }
        require(payerSum == totalAmount.amount) {
            "Sum of payers ($payerSum) must equal totalAmount (${totalAmount.amount})"
        }
        require(shareSum == totalAmount.amount) {
            "Sum of shares ($shareSum) must equal totalAmount (${totalAmount.amount})"
        }
    }
}

data class LocalExpense(
    val id: Long,
    val title: String,
    val amount: MoneyVnd,
    val payerIndex: Int,
    val participantIndices: List<Int>,
    val shares: List<MoneyVnd>,
    val method: SplitMethod = SplitMethod.EQUAL,
    val createdAt: String = ""
) {
    enum class SplitMethod {
        EQUAL,
        CUSTOM
    }

    init {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(amount.amount in 1L..100_000_000L) {
            "Local expense amount must be between 1 and 100,000,000 VND, got ${amount.amount}"
        }
        require(payerIndex >= 0) { "Payer index must be non-negative: $payerIndex" }
        require(participantIndices.isNotEmpty()) { "Participants must not be empty" }
        val shareSum = shares.fold(0L) { acc, s -> Math.addExact(acc, s.amount) }
        require(shareSum == amount.amount) {
            "Sum of shares ($shareSum) must equal expense amount (${amount.amount})"
        }
    }
}

data class Balance(
    val membershipId: MembershipId,
    val displayName: String = "",
    val netAmount: Long,
    val memberIndex: Int? = null
) {
    val isCreditor: Boolean get() = netAmount > 0
    val isDebtor: Boolean get() = netAmount < 0
    val isSettled: Boolean get() = netAmount == 0L

    val creditAmount: MoneyVnd get() = if (netAmount > 0) MoneyVnd(netAmount) else MoneyVnd.ZERO
    val debtAmount: MoneyVnd get() = if (netAmount < 0) MoneyVnd(-netAmount) else MoneyVnd.ZERO

    fun formatNetAmount(): String = MoneyVnd.formatVndAmount(netAmount)
}

data class SettlementSuggestion(
    val fromMembershipId: MembershipId,
    val toMembershipId: MembershipId,
    val amount: MoneyVnd,
    val fromDisplayName: String = "",
    val toDisplayName: String = "",
    val fromIndex: Int? = null,
    val toIndex: Int? = null
) {
    init {
        require(amount.amount > 0) { "Settlement amount must be positive, got: ${amount.amount}" }
    }
}

data class LocalTransfer(
    val id: Long,
    val fromIndex: Int,
    val toIndex: Int,
    val amount: MoneyVnd,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val createdAt: String = ""
)
