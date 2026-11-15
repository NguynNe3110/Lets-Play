package com.example.base.shared

import com.example.base.shared.flow.DebounceCancellationPolicy
import com.example.base.shared.flow.debounceWithLatest
import com.example.base.shared.flow.debounceWithLatestFlow
import com.example.base.shared.flow.droppable
import com.example.base.shared.flow.droppableTransform
import com.example.base.shared.flow.sequential
import com.example.base.shared.flow.sequentialAction
import com.example.base.shared.flow.sequentialQueue
import com.example.base.shared.flow.throttleFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class FlowPoliciesTest {

    // region throttleFirst

    @Test
    fun throttleFirstEmitsFirstItemAndDropsSubsequentItemsWithinWindow() = runTest {
        val upstream = flow {
            emit(1) // t = 0
            delay(30)
            emit(2) // t = 30 -> dropped
            delay(30)
            emit(3) // t = 60 -> dropped
            delay(50)
            emit(4) // t = 110 -> window expired at 100, emitted!
            delay(20)
            emit(5) // t = 130 -> dropped
            delay(90)
            emit(6) // t = 220 -> window expired at 210, emitted!
        }

        val emissions = mutableListOf<Pair<Long, Int>>()
        upstream.throttleFirst(100)
            .collect { value ->
                emissions += (currentTime to value)
            }

        assertEquals(
            listOf(
                0L to 1,
                110L to 4,
                220L to 6
            ),
            emissions
        )
    }

    @Test
    fun throttleFirstWithZeroWindowEmitsAllItems() = runTest {
        val result = flowOf(1, 2, 3, 4)
            .throttleFirst(0)
            .toList()

        assertEquals(listOf(1, 2, 3, 4), result)
    }

    @Test
    fun throttleFirstCompletesWithoutHangingForRemainingWindow() = runTest {
        val start = currentTime
        val result = flow {
            emit("hello")
        }.throttleFirst(5_000)
            .toList()

        assertEquals(listOf("hello"), result)
        assertEquals(start, currentTime) // Flow completed immediately without waiting 5s!
    }

    // endregion

    // region droppable

    @Test
    fun droppableCallbackDropsInvocationsWhileActive() = runTest {
        val executed = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()

        val onAction = droppable(this) { param: String ->
            executed += "start:$param"
            gate.await()
            executed += "end:$param"
        }

        assertFalse(onAction.isRunning)

        // First call starts execution
        onAction("first")
        runCurrent()
        assertTrue(onAction.isRunning)
        assertEquals(listOf("start:first"), executed)

        // Second and third calls arrive while first is still running -> dropped!
        onAction("second")
        onAction("third")
        runCurrent()
        assertEquals(listOf("start:first"), executed)

        // Complete the first execution
        gate.complete(Unit)
        runCurrent()
        assertFalse(onAction.isRunning)
        assertEquals(listOf("start:first", "end:first"), executed)

        // Now that it's idle, next call is accepted
        val secondGate = CompletableDeferred<Unit>()
        val onAction2 = droppable(this) { param: String ->
            executed += "start:$param"
            secondGate.await()
            executed += "end:$param"
        }
        onAction2("fourth")
        runCurrent()
        secondGate.complete(Unit)
        runCurrent()

        assertEquals(listOf("start:first", "end:first", "start:fourth", "end:fourth"), executed)
    }

    @Test
    fun droppableResetsStateWhenActionFails() = runTest {
        val attempts = AtomicInteger(0)
        val caughtErrors = mutableListOf<Throwable>()
        val onAction = droppable(this, onError = { caughtErrors += it }) {
            attempts.incrementAndGet()
            throw IllegalStateException("Failed")
        }

        onAction()
        runCurrent()
        assertFalse(onAction.isRunning)
        assertEquals(1, attempts.get())
        assertEquals(1, caughtErrors.size)

        // Next invocation runs because state was safely reset
        onAction()
        runCurrent()
        assertFalse(onAction.isRunning)
        assertEquals(2, attempts.get())
        assertEquals(2, caughtErrors.size)
    }

    @Test
    fun flowDroppableTransformDropsUpstreamEmissionsDuringActiveTransform() = runTest {
        val upstream = flow {
            emit("A") // t = 0
            delay(20)
            emit("B") // t = 20 -> dropped because transform("A") is delaying 50ms
            delay(40)
            emit("C") // t = 60 -> transform("A") finished at 50ms, so "C" is accepted!
        }

        val results = mutableListOf<Pair<Long, String>>()
        upstream.droppableTransform { item ->
            delay(50)
            "$item-processed"
        }.collect { processed ->
            results += (currentTime to processed)
        }

        assertEquals(
            listOf(
                50L to "A-processed",
                110L to "C-processed"
            ),
            results
        )
    }

    // endregion

    // region sequential

    @Test
    fun sequentialProcessesInvocationsStrictlyInFifoOrderWithoutOverlap() = runTest {
        val log = mutableListOf<String>()
        var activeCount = 0
        var maxConcurrent = 0

        val onEvent = sequential<Int>(this) { item ->
            activeCount++
            if (activeCount > maxConcurrent) maxConcurrent = activeCount
            log += "start:$item"
            delay(50)
            log += "end:$item"
            activeCount--
        }

        // Fire 3 items concurrently
        onEvent(1)
        onEvent(2)
        onEvent(3)

        advanceTimeBy(200)

        assertEquals(1, maxConcurrent) // strictly non-overlapping
        assertEquals(
            listOf(
                "start:1", "end:1",
                "start:2", "end:2",
                "start:3", "end:3"
            ),
            log
        )
        onEvent.close()
    }

    @Test
    fun sequentialWithBufferOverflowDropsOldestWhenCapacityExceeded() = runTest {
        val processed = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()

        val onEvent = sequential<Int>(
            scope = this,
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ) { item ->
            if (item == 1) gate.await()
            processed += item
        }

        onEvent(1) // actively processing (waiting on gate)
        runCurrent()

        onEvent(2) // in buffer
        onEvent(3) // replaces 2 in buffer (DROP_OLDEST)
        runCurrent()

        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf(1, 3), processed)
        onEvent.close()
    }

    @Test
    fun sequentialTryEnqueueReturnsFalseWhenBoundedBufferIsFull() = runTest {
        val processed = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()

        val onEvent = sequential<Int>(
            scope = this,
            capacity = 1,
            onBufferOverflow = BufferOverflow.SUSPEND
        ) { item ->
            if (item == 1) gate.await()
            processed += item
        }

        // First item is picked up by consumer loop and waits at gate
        assertTrue(onEvent.tryEnqueue(1))
        runCurrent()

        // Buffer has capacity = 1, so second item is accepted into buffer
        assertTrue(onEvent.tryEnqueue(2))
        runCurrent()

        // Buffer is now full (item 1 processing, item 2 in buffer).
        // Under BufferOverflow.SUSPEND, non-blocking tryEnqueue must return false!
        assertFalse(onEvent.tryEnqueue(3))
        runCurrent()

        // Release gate to let item 1 complete
        gate.complete(Unit)
        runCurrent()

        // Item 1 and item 2 have finished. Item 3 was rejected and thus not processed.
        assertEquals(listOf(1, 2), processed)

        // Capacity is now free, next tryEnqueue is accepted
        assertTrue(onEvent.tryEnqueue(4))
        runCurrent()

        assertEquals(listOf(1, 2, 4), processed)
        onEvent.close()
    }

    @Test
    fun sequentialSuspendEnqueueWaitsUntilCapacityIsAvailableInsteadOfDropping() = runTest {
        val processed = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()

        val onEvent = sequential<Int>(
            scope = this,
            capacity = 1,
            onBufferOverflow = BufferOverflow.SUSPEND
        ) { item ->
            if (item == 1) gate.await()
            processed += item
        }

        // First item starts processing and waits at gate
        onEvent.enqueue(1)
        runCurrent()

        // Second item occupies the 1-slot buffer
        onEvent.enqueue(2)
        runCurrent()

        // Third item is enqueued via suspending enqueue().
        // Since buffer is full with BufferOverflow.SUSPEND, enqueue(3) must suspend!
        var item3Enqueued = false
        val enqueueJob = launch {
            onEvent.enqueue(3)
            item3Enqueued = true
        }
        runCurrent()

        // Verify that enqueue(3) is suspended waiting for capacity, NOT dropped!
        assertFalse(item3Enqueued)
        assertTrue(enqueueJob.isActive)
        assertEquals(emptyList(), processed)

        // Release gate for item 1 -> frees buffer when item 2 is picked up by consumer
        gate.complete(Unit)
        runCurrent()

        // Item 3 has now successfully enqueued into the freed buffer slot
        assertTrue(item3Enqueued)
        assertTrue(enqueueJob.isCompleted)

        // Allow consumer to finish processing items 2 and 3
        runCurrent()

        // All 3 items processed in strict FIFO order without dropping!
        assertEquals(listOf(1, 2, 3), processed)
        onEvent.close()
    }

    @Test
    fun sequentialActionSuspendEnqueueAndTryEnqueue() = runTest {
        var processedCount = 0
        val gate = CompletableDeferred<Unit>()

        val action = sequentialAction(
            scope = this,
            capacity = 1,
            onBufferOverflow = BufferOverflow.SUSPEND
        ) {
            if (processedCount == 0) gate.await()
            processedCount++
        }

        assertTrue(action.tryEnqueue())
        runCurrent()

        assertTrue(action.tryEnqueue())
        runCurrent()

        // Buffer full -> tryEnqueue returns false
        assertFalse(action.tryEnqueue())

        var enqueueResumed = false
        val job = launch {
            action.enqueue()
            enqueueResumed = true
        }
        runCurrent()
        assertFalse(enqueueResumed)

        gate.complete(Unit)
        runCurrent()

        assertTrue(enqueueResumed)
        assertTrue(job.isCompleted)

        assertEquals(3, processedCount)
        action.close()
    }

    @Test
    fun sequentialQueueSuspendEnqueueAndTryEnqueue() = runTest {
        val executed = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()

        val queue = sequentialQueue(
            scope = this,
            capacity = 1,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        assertTrue(queue.tryEnqueue {
            gate.await()
            executed += "first"
        })
        runCurrent()

        assertTrue(queue.tryEnqueue {
            executed += "second"
        })
        runCurrent()

        // Buffer full -> tryEnqueue returns false
        assertFalse(queue.tryEnqueue {
            executed += "dropped"
        })

        // Suspend enqueue waits until capacity is available
        var thirdEnqueued = false
        val enqueueJob = launch {
            queue.enqueue {
                executed += "third"
            }
            thirdEnqueued = true
        }
        runCurrent()
        assertFalse(thirdEnqueued)

        gate.complete(Unit)
        runCurrent()

        assertTrue(thirdEnqueued)
        assertTrue(enqueueJob.isCompleted)
        assertEquals(listOf("first", "second", "third"), executed)
        queue.close()
    }

    // endregion

    // region debounceWithLatest

    @Test
    fun debounceWithLatestDebouncesRapidEmissions() = runTest {
        val upstream = flow {
            emit("k")
            delay(50)
            emit("ko")
            delay(50)
            emit("kot")
            delay(200) // timeout (100ms) elapses here -> emits "kot"
            emit("kotl")
            delay(50)
            emit("kotlin")
            delay(200) // timeout (100ms) elapses here -> emits "kotlin"
        }

        val results = mutableListOf<Pair<Long, String>>()
        upstream.debounceWithLatest(100) { query ->
            "$query:result"
        }.collect { result ->
            results += (currentTime to result)
        }

        assertEquals(
            listOf(
                200L to "kot:result",
                450L to "kotlin:result"
            ),
            results
        )
    }

    @Test
    fun debounceWithLatestImmediateCancellationCancelsInFlightOnNewEmission() = runTest {
        val cancellationEvents = mutableListOf<String>()
        val completions = mutableListOf<String>()

        val upstream = flow {
            emit("query1") // t = 0 -> debounce waits 100ms -> starts at 100ms
            delay(150)
            emit("query2") // t = 150 -> in-flight query1 is running -> cancelled immediately at 150ms!
            delay(200) // t = 350 -> query2 debounces 100ms (starts at 250ms, finishes at 350ms)
        }

        upstream.debounceWithLatestFlow(
            timeoutMillis = 100,
            cancellationPolicy = DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION
        ) { query ->
            flow {
                try {
                    delay(100) // takes 100ms to complete
                    completions += query
                    emit("$query-done")
                } finally {
                    cancellationEvents += "$query-cancelled-at-$currentTime"
                }
            }
        }.toList()

        // query1 started at 100ms and was cancelled at 150ms when query2 arrived
        assertTrue(cancellationEvents.any { it.startsWith("query1-cancelled-at-150") })
        // query1 never completed
        assertFalse("query1" in completions)
        // query2 started at 250ms and completed at 350ms
        assertTrue("query2" in completions)
    }

    @Test
    fun debounceWithLatestAfterTimeoutCancellationAllowsInFlightToRunDuringDebounce() = runTest {
        val completions = mutableListOf<String>()

        val upstream = flow {
            emit("query1") // t = 0 -> debounce waits 50ms -> starts at 50ms
            delay(70)      // at t = 70ms:
            emit("query2") // query1 has been running for 20ms (needs 40ms total, so completes at 90ms).
                           // Debounce for query2 runs from 70ms to 120ms (50ms).
                           // Under AFTER_DEBOUNCE_TIMEOUT, query1 finishes at 90ms BEFORE query2 timeout expires!
            delay(200)
        }

        val results = upstream.debounceWithLatestFlow(
            timeoutMillis = 50,
            cancellationPolicy = DebounceCancellationPolicy.AFTER_DEBOUNCE_TIMEOUT
        ) { query ->
            flow {
                delay(40)
                completions += query
                emit("$query-done")
            }
        }.toList()

        // Both query1 and query2 completed because query1 finished before query2's debounce window expired!
        assertEquals(listOf("query1", "query2"), completions)
        assertEquals(listOf("query1-done", "query2-done"), results)
    }

    // endregion
}
