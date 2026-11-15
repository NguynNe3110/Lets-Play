package com.uzuu.letplay.domain.rules

import com.uzuu.letplay.domain.model.ProposalId
import com.uzuu.letplay.domain.model.VoteValue

object BordaCalculator {

    /**
     * Calculates Borda ordering of selected proposals based on submitted ballots.
     *
     * Valid ballots must:
     * 1. Have length equal to [selectedProposals.size]
     * 2. Contain no duplicate items
     * 3. Contain only items that exist in [selectedProposals]
     *
     * Points for a proposal in a ballot of size N at index `rank` (0-based):
     * points = N - 1 - rank
     *
     * Proposals are sorted by total score descending.
     * Ties are broken by original position in [selectedProposals] (ascending index).
     */
    fun <T> calculateBordaOrder(
        selectedProposals: List<T>,
        ballots: Collection<List<T>>
    ): List<T> {
        if (selectedProposals.isEmpty()) return emptyList()
        val selectedSet = selectedProposals.toSet()

        // Filter valid ballots
        val validBallots = ballots.filter { ballot ->
            ballot.size == selectedProposals.size &&
                    ballot.toSet().size == ballot.size &&
                    ballot.all { it in selectedSet }
        }

        data class ScoredItem(val item: T, val originalIndex: Int, val score: Long)

        val scored = selectedProposals.mapIndexed { index, item ->
            val score = validBallots.fold(0L) { sum, ballot ->
                val rank = ballot.indexOf(item)
                val points = (ballot.size - 1 - rank).toLong()
                Math.addExact(sum, points)
            }
            ScoredItem(item, index, score)
        }

        // Sort descending by score, tie-break ascending by originalIndex
        return scored
            .sortedWith(compareByDescending<ScoredItem> { it.score }.thenBy { it.originalIndex })
            .map { it.item }
    }

    /**
     * Tally votes for a proposal across base votes and user votes.
     * Base votes: [veryWant, canJoin, pass]
     * User votes: Iterable of VoteValue
     *
     * Returns Triple(veryWantCount, canJoinCount, passCount).
     */
    fun tally(
        baseVotes: List<Int>,
        userVotes: Iterable<VoteValue>
    ): Triple<Int, Int, Int> {
        var veryWant = baseVotes.getOrElse(0) { 0 }
        var canJoin = baseVotes.getOrElse(1) { 0 }
        var pass = baseVotes.getOrElse(2) { 0 }

        for (vote in userVotes) {
            when (vote) {
                VoteValue.VERY_WANT -> veryWant++
                VoteValue.CAN_JOIN -> canJoin++
                VoteValue.PASS -> pass++
            }
        }

        return Triple(veryWant, canJoin, pass)
    }

    /**
     * Calculates score for a proposal according to BR01:
     * Score = 2 * (Rất muốn) + 1 * (Đi cùng được)
     */
    fun calculateProposalScore(
        baseVotes: List<Int>,
        userVotes: Iterable<VoteValue>
    ): Int {
        val (veryWant, canJoin, _) = tally(baseVotes, userVotes)
        return veryWant * 2 + canJoin
    }

    /**
     * Calculates estimated start time for each leg of a trip:
     * Starts at [startHour]:[startMinute], adds duration of each preceding leg + [transitMinutes].
     * Formats output as "HH:mm".
     */
    fun <T> calculateLegTime(
        legIndex: Int,
        order: List<T>,
        durationsMinutes: Map<T, Int>,
        startHour: Int = 18,
        startMinute: Int = 0,
        transitMinutes: Int = 15,
        defaultDurationMinutes: Int = 30
    ): String {
        require(legIndex in 0..order.size) { "legIndex $legIndex out of range [0, ${order.size}]" }
        var totalMinutes = startHour * 60 + startMinute
        for (i in 0 until legIndex) {
            val proposalId = order[i]
            val duration = durationsMinutes[proposalId] ?: defaultDurationMinutes
            totalMinutes += duration + transitMinutes
        }

        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val hStr = hours.toString().padStart(2, '0')
        val mStr = minutes.toString().padStart(2, '0')
        return "$hStr:$mStr"
    }
}
