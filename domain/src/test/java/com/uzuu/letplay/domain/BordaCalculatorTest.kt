package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.ProposalId
import com.uzuu.letplay.domain.model.VoteValue
import com.uzuu.letplay.domain.rules.BordaCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BordaCalculatorTest {

    @Test
    fun calculatesBordaOrderCorrectlyWithMultipleBallots() {
        val p1 = ProposalId("1")
        val p2 = ProposalId("2")
        val p3 = ProposalId("3")
        val selected = listOf(p1, p2, p3)

        // 3 proposals -> rank 0 = 2 pts, rank 1 = 1 pt, rank 2 = 0 pts
        // Ballot 1: [p2, p3, p1] -> p2: 2, p3: 1, p1: 0
        // Ballot 2: [p2, p1, p3] -> p2: 2, p1: 1, p3: 0
        // Ballot 3: [p3, p2, p1] -> p3: 2, p2: 1, p1: 0
        // Total scores:
        // p2: 2 + 2 + 1 = 5
        // p3: 1 + 0 + 2 = 3
        // p1: 0 + 1 + 0 = 1
        val ballots = listOf(
            listOf(p2, p3, p1),
            listOf(p2, p1, p3),
            listOf(p3, p2, p1)
        )

        val result = BordaCalculator.calculateBordaOrder(selected, ballots)
        assertEquals(listOf(p2, p3, p1), result)
    }

    @Test
    fun breaksTiesUsingOriginalSelectedOrder() {
        val p1 = ProposalId("p1")
        val p2 = ProposalId("p2")
        val selected = listOf(p1, p2)

        // Equal scores:
        // Ballot 1: [p1, p2] -> p1: 1, p2: 0
        // Ballot 2: [p2, p1] -> p2: 1, p1: 0
        // Both have score 1. p1 came first in `selected`, so p1 wins tie.
        val ballots = listOf(
            listOf(p1, p2),
            listOf(p2, p1)
        )

        val result = BordaCalculator.calculateBordaOrder(selected, ballots)
        assertEquals(listOf(p1, p2), result)
    }

    @Test
    fun ignoresInvalidBallots() {
        val p1 = ProposalId("1")
        val p2 = ProposalId("2")
        val selected = listOf(p1, p2)

        val ballots = listOf(
            listOf(p1), // wrong length -> ignored
            listOf(p1, p1), // duplicate -> ignored
            listOf(p1, ProposalId("unknown")), // unknown ID -> ignored
            listOf(p2, p1) // valid -> p2: 1 pt, p1: 0 pt
        )

        val result = BordaCalculator.calculateBordaOrder(selected, ballots)
        assertEquals(listOf(p2, p1), result)
    }

    @Test
    fun br01CalculatesScoreAsTwoTimesVeryWantPlusOneTimesCanJoin() {
        // Base: 3 very want, 1 can join, 0 pass
        val baseVotes = listOf(3, 1, 0)

        // Base score = 3 * 2 + 1 * 1 = 7
        assertEquals(7, BordaCalculator.calculateProposalScore(baseVotes, emptyList()))

        // User votes VERY_WANT (2 points) -> total: 4 very want, 1 can join -> 4*2 + 1 = 9
        val userVotes = listOf(VoteValue.VERY_WANT)
        assertEquals(9, BordaCalculator.calculateProposalScore(baseVotes, userVotes))

        // Multiple users: 1 very want, 1 can join, 1 pass -> total: 4 very want, 2 can join, 1 pass -> 4*2 + 2 = 10
        val moreVotes = listOf(VoteValue.VERY_WANT, VoteValue.CAN_JOIN, VoteValue.PASS)
        assertEquals(10, BordaCalculator.calculateProposalScore(baseVotes, moreVotes))
    }

    @Test
    fun talliesVotesCorrectly() {
        val baseVotes = listOf(3, 1, 0)
        val userVotes = listOf(VoteValue.CAN_JOIN)
        val (veryWant, canJoin, pass) = BordaCalculator.tally(baseVotes, userVotes)
        assertEquals(3, veryWant)
        assertEquals(2, canJoin)
        assertEquals(0, pass)
    }

    @Test
    fun calculatesLegTimesStartingFrom1800With15mTransit() {
        val p1 = ProposalId("1")
        val p2 = ProposalId("2")
        val durations = mapOf(
            p1 to 90, // 90 min duration
            p2 to 30  // 30 min duration
        )

        // Leg 0: starts at 18:00
        assertEquals("18:00", BordaCalculator.calculateLegTime(0, listOf(p1, p2), durations))

        // Leg 1: 18:00 + 90 min + 15 min transit = 19:45
        assertEquals("19:45", BordaCalculator.calculateLegTime(1, listOf(p1, p2), durations))

        // Leg 2 (arrival/completion): 19:45 + 30 min + 15 min transit = 20:30
        assertEquals("20:30", BordaCalculator.calculateLegTime(2, listOf(p1, p2), durations))
    }
}
