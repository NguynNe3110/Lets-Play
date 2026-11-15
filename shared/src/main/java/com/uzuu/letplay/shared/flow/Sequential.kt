package com.uzuu.letplay.shared.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch

/**
 * Handle for a sequential action without parameters.
 */
interface SequentialAction : () -> Unit {
    /**
     * Suspends until capacity is available and enqueues the action invocation.
     * When using [BufferOverflow.SUSPEND] with a bounded buffer, this suspends
     * rather than dropping the invocation.
     */
    suspend fun enqueue()

    /**
     * Non-blocking attempt to enqueue an execution.
     * @return `true` if successfully enqueued into the buffer/queue, or `false` if full or closed.
     */
    fun tryEnqueue(): Boolean

    /**
     * Alias for [enqueue].
     */
    suspend fun send() = enqueue()

    /**
     * Alias for [tryEnqueue].
     */
    fun trySend(): Boolean = tryEnqueue()

    /**
     * Closes the underlying queue and cancels the processor job.
     */
    fun close()
}

/**
 * Handle for a sequential action with parameter of type [T].
 */
interface SequentialHandler<in T> : (T) -> Unit {
    /**
     * Suspends until capacity is available and enqueues [param].
     * When using [BufferOverflow.SUSPEND] with a bounded buffer, this suspends
     * rather than dropping the invocation.
     */
    suspend fun enqueue(param: T)

    /**
     * Non-blocking attempt to enqueue [param].
     * @return `true` if successfully enqueued into the buffer/queue, or `false` if full or closed.
     */
    fun tryEnqueue(param: T): Boolean

    /**
     * Alias for [enqueue].
     */
    suspend fun send(param: T) = enqueue(param)

    /**
     * Alias for [trySend].
     */
    fun trySend(param: T): Boolean = tryEnqueue(param)

    /**
     * Closes the underlying queue and cancels the processor job.
     */
    fun close()
}

/**
 * Handle for a sequential execution queue that accepts arbitrary suspending actions.
 */
interface SequentialQueue {
    /**
     * Suspends until capacity is available and enqueues [action].
     * When using [BufferOverflow.SUSPEND] with a bounded buffer, this suspends
     * rather than dropping the invocation.
     */
    suspend fun enqueue(action: suspend () -> Unit)

    /**
     * Non-blocking attempt to enqueue [action].
     * @return `true` if successfully enqueued into the buffer/queue, or `false` if full or closed.
     */
    fun tryEnqueue(action: suspend () -> Unit): Boolean

    /**
     * Alias for [enqueue].
     */
    suspend fun send(action: suspend () -> Unit) = enqueue(action)

    /**
     * Alias for [trySend].
     */
    fun trySend(action: suspend () -> Unit): Boolean = tryEnqueue(action)

    /**
     * Closes the underlying queue and cancels the processor job.
     */
    fun close()
}

/**
 * Creates a sequential callback wrapper that buffers invocations and executes them
 * strictly sequentially in FIFO order on [scope].
 *
 * Buffer capacity and overflow policy can be declared explicitly.
 */
fun sequential(
    scope: CoroutineScope,
    capacity: Int = Channel.UNLIMITED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onError: ((Throwable) -> Unit)? = null,
    action: suspend () -> Unit
): SequentialAction {
    val channel = Channel<Unit>(capacity, onBufferOverflow)
    val job = scope.launch {
        try {
            for (item in channel) {
                try {
                    action()
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    if (onError != null) onError(throwable) else throw throwable
                }
            }
        } finally {
            channel.close()
        }
    }
    return object : SequentialAction {
        override fun invoke() {
            tryEnqueue()
        }

        override suspend fun enqueue() {
            channel.send(Unit)
        }

        override fun tryEnqueue(): Boolean {
            return channel.trySend(Unit).isSuccess
        }

        override fun close() {
            channel.close()
            job.cancel()
        }
    }
}

/**
 * Explicit non-parameterized alias for [sequential].
 */
fun sequentialAction(
    scope: CoroutineScope,
    capacity: Int = Channel.UNLIMITED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onError: ((Throwable) -> Unit)? = null,
    action: suspend () -> Unit
): SequentialAction = sequential(scope, capacity, onBufferOverflow, onError, action)

/**
 * Parameterized overload of [sequential].
 */
fun <T> sequential(
    scope: CoroutineScope,
    capacity: Int = Channel.UNLIMITED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onError: ((Throwable) -> Unit)? = null,
    action: suspend (T) -> Unit
): SequentialHandler<T> {
    val channel = Channel<T>(capacity, onBufferOverflow)
    val job = scope.launch {
        try {
            for (item in channel) {
                try {
                    action(item)
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    if (onError != null) onError(throwable) else throw throwable
                }
            }
        } finally {
            channel.close()
        }
    }
    return object : SequentialHandler<T> {
        override fun invoke(param: T) {
            tryEnqueue(param)
        }

        override suspend fun enqueue(param: T) {
            channel.send(param)
        }

        override fun tryEnqueue(param: T): Boolean {
            return channel.trySend(param).isSuccess
        }

        override fun close() {
            channel.close()
            job.cancel()
        }
    }
}

/**
 * Creates a sequential execution queue that executes enqueued suspending actions
 * strictly sequentially in FIFO order on [scope].
 */
fun sequentialQueue(
    scope: CoroutineScope,
    capacity: Int = Channel.UNLIMITED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onError: ((Throwable) -> Unit)? = null
): SequentialQueue {
    val channel = Channel<suspend () -> Unit>(capacity, onBufferOverflow)
    val job = scope.launch {
        try {
            for (action in channel) {
                try {
                    action()
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    if (onError != null) onError(throwable) else throw throwable
                }
            }
        } finally {
            channel.close()
        }
    }
    return object : SequentialQueue {
        override suspend fun enqueue(action: suspend () -> Unit) {
            channel.send(action)
        }

        override fun tryEnqueue(action: suspend () -> Unit): Boolean {
            return channel.trySend(action).isSuccess
        }

        override fun close() {
            channel.close()
            job.cancel()
        }
    }
}

/**
 * Processes elements from this [Flow] sequentially using [action] on [scope].
 */
fun <T> Flow<T>.sequential(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    action: suspend (T) -> Unit
): Job = scope.launch {
    val upstream = if (capacity == Channel.RENDEZVOUS && onBufferOverflow == BufferOverflow.SUSPEND) {
        this@sequential
    } else {
        this@sequential.buffer(capacity, onBufferOverflow)
    }
    upstream.collect { value ->
        action(value)
    }
}
