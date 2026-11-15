package com.example.base.data.integration

import com.example.base.data.integration.firebase.FakeFirebaseClient
import com.example.base.data.integration.firebase.FirebaseConfig
import com.example.base.data.integration.firebase.FirebaseDocument
import com.example.base.domain.model.Todo
import com.example.base.domain.usecase.GetTodosOutput
import com.example.base.shared.error.AppException
import com.example.base.shared.error.NetworkErrorKind
import com.example.base.shared.result.Result
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirebaseAdapterTest {

    private lateinit var client: FakeFirebaseClient

    @BeforeTest
    fun setUp() {
        client = FakeFirebaseClient()
    }

    @Test
    fun enforcesVariantSeparationAndConfigValidation() {
        val devConfig = FirebaseConfig(
            applicationId = "com.example.base.dev.debug",
            projectId = "base-app-dev",
            apiKey = "AIzaSyDevKey"
        )
        val prodConfig = FirebaseConfig(
            applicationId = "com.example.base",
            projectId = "base-app-prod",
            apiKey = "AIzaSyProdKey"
        )

        assertTrue(devConfig.isDev)
        assertEquals(false, devConfig.isProd)

        assertTrue(prodConfig.isProd)
        assertEquals(false, prodConfig.isDev)

        assertFailsWith<IllegalArgumentException> {
            FirebaseConfig(applicationId = "", projectId = "dev", apiKey = "key")
        }
        assertFailsWith<IllegalArgumentException> {
            FirebaseConfig(applicationId = "app", projectId = "", apiKey = "key")
        }
        assertFailsWith<IllegalArgumentException> {
            FirebaseConfig(applicationId = "app", projectId = "dev", apiKey = "")
        }
    }

    @Test
    fun performsDocumentCrudOperationsCleanly() = runTest {
        // Set document
        val setResult = client.setDocument(
            collection = "todos",
            documentId = "todo-firebase-1",
            data = mapOf("title" to "Firebase Document", "isCompleted" to false)
        )
        assertIs<Result.Success<*>>(setResult)

        // Get document
        val getResult = client.getDocument("todos", "todo-firebase-1")
        assertIs<Result.Success<*>>(getResult)
        val doc = (getResult as Result.Success).data
        assertEquals("todo-firebase-1", doc.id)
        assertEquals("Firebase Document", doc.data["title"])

        // Delete document
        val deleteResult = client.deleteDocument("todos", "todo-firebase-1")
        assertIs<Result.Success<*>>(deleteResult)

        // Get after delete returns NOT_FOUND
        val getAfterDelete = client.getDocument("todos", "todo-firebase-1")
        val failure = assertIs<Result.Failure>(getAfterDelete)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(404, exception.httpCode)
    }

    @Test
    fun cleansUpSnapshotListenersUponCoroutineCancellation() = runTest {
        assertEquals(0, client.activeListenersCount)

        val emissions = mutableListOf<Result<List<FirebaseDocument>>>()
        val job = launch {
            client.observeCollection("todos").collect { result ->
                emissions.add(result)
            }
        }
        testScheduler.runCurrent()

        // Active listener must now be registered
        assertEquals(1, client.activeListenersCount)
        assertEquals(1, emissions.size)

        // Mutate document: realtime update should trigger
        client.setDocument(
            collection = "todos",
            documentId = "doc-1",
            data = mapOf("title" to "Realtime Title", "isCompleted" to true)
        )
        testScheduler.runCurrent()
        assertEquals(2, emissions.size)
        val lastData = (emissions.last() as Result.Success).data
        assertEquals(1, lastData.size)
        assertEquals("doc-1", lastData.first().id)

        // Cancel the coroutine collecting the Flow
        job.cancelAndJoin()
        testScheduler.runCurrent()

        // Listener must be cleanly cleaned up upon cancellation
        assertEquals(0, client.activeListenersCount, "Active listener count must be 0 after cancellation")
    }

    @Test
    fun registrationRemoveDirectlyCleansUpListener() {
        assertEquals(0, client.activeListenersCount)
        val registration = client.addSnapshotListener("todos") { _, _ -> }
        assertEquals(1, client.activeListenersCount)

        registration.remove()
        assertEquals(0, client.activeListenersCount)
    }

    @Test
    fun mapsFirebaseErrorCodesToAppException() = runTest {
        client.simulatedFirebaseCode = "PERMISSION_DENIED"
        val permResult = client.getDocument("secure_col", "doc-1")
        val permFailure = assertIs<Result.Failure>(permResult)
        val permEx = assertIs<AppException.ServerError>(permFailure.exception)
        assertEquals(403, permEx.httpCode)
        assertEquals(false, permEx.canRetry)

        client.simulatedFirebaseCode = "UNAVAILABLE"
        val unavailResult = client.getDocument("todos", "doc-1")
        val unavailFailure = assertIs<Result.Failure>(unavailResult)
        val unavailEx = assertIs<AppException.NetworkError>(unavailFailure.exception)
        assertEquals(NetworkErrorKind.Connection, unavailEx.kind)
        assertTrue(unavailEx.retryable)

        client.simulatedFirebaseCode = "DEADLINE_EXCEEDED"
        val timeoutResult = client.getDocument("todos", "doc-1")
        val timeoutFailure = assertIs<Result.Failure>(timeoutResult)
        val timeoutEx = assertIs<AppException.NetworkError>(timeoutFailure.exception)
        assertEquals(NetworkErrorKind.Timeout, timeoutEx.kind)
        assertTrue(timeoutEx.retryable)

        client.simulatedFirebaseCode = "ALREADY_EXISTS"
        val conflictResult = client.setDocument("todos", "doc-1", emptyMap())
        val conflictFailure = assertIs<Result.Failure>(conflictResult)
        val conflictEx = assertIs<AppException.ValidationError>(conflictFailure.exception)
        assertEquals("ALREADY_EXISTS", conflictEx.code)
    }

    @Test
    fun mapsLowLevelExceptionsCleanly() = runTest {
        client.simulatedFirebaseCode = null
        client.simulatedError = SocketTimeoutException("Firebase stream timed out")

        val result = client.getDocument("todos", "doc-1")
        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.NetworkError>(failure.exception)
        assertEquals(NetworkErrorKind.Timeout, exception.kind)

        client.simulatedError = IOException("Broken pipe")
        val ioResult = client.getDocument("todos", "doc-1")
        val ioFailure = assertIs<Result.Failure>(ioResult)
        val ioEx = assertIs<AppException.NetworkError>(ioFailure.exception)
        assertEquals(NetworkErrorKind.Connection, ioEx.kind)
    }

    @Test
    fun observesCollectionMappingToDomainResultBaseOutput() = runTest {
        client.seedDocuments(
            collection = "todos",
            docs = listOf(
                FirebaseDocument("f1", mapOf("title" to "Firebase Todo 1", "isCompleted" to false)),
                FirebaseDocument("f2", mapOf("title" to "Firebase Todo 2", "isCompleted" to true))
            )
        )

        val flow = client.observeDomainCollection("todos") { docs ->
            val todos = docs.map {
                Todo(
                    id = it.id,
                    title = it.data["title"] as String,
                    isCompleted = it.data["isCompleted"] as Boolean
                )
            }
            GetTodosOutput(todos)
        }

        val emissions = flow.take(1).toList()
        val firstEmission = emissions.first()
        assertIs<Result.Success<*>>(firstEmission)
        val output = (firstEmission as Result.Success).data
        assertEquals(2, output.todos.size)
        assertEquals("Firebase Todo 1", output.todos[0].title)
        assertEquals("Firebase Todo 2", output.todos[1].title)
    }
}
