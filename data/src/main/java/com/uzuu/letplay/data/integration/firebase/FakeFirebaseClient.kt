package com.uzuu.letplay.data.integration.firebase

import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory Fake Firebase client for verifying variant separation,
 * snapshot listener cleanup upon coroutine cancellation, and domain mapping.
 */
class FakeFirebaseClient(
    override val config: FirebaseConfig = FirebaseConfig(
        applicationId = "com.uzuu.letplay.dev.debug",
        projectId = "sample-app-dev",
        apiKey = "AIzaSyFakeKeyDev"
    ),
    private val errorMapper: FirebaseErrorMapper = FirebaseErrorMapper()
) : FirebaseAdapter {

    // Storage: collection -> (documentId -> FirebaseDocument)
    private val collections: MutableMap<String, MutableMap<String, FirebaseDocument>> = mutableMapOf()

    // Listener callbacks: collection -> set of callbacks
    private val listeners: MutableMap<String, MutableSet<(List<FirebaseDocument>?, Throwable?) -> Unit>> = mutableMapOf()

    override val activeListenersCount: Int
        get() = listeners.values.sumOf { it.size }

    // Fault injection
    var simulatedError: Throwable? = null
    var simulatedFirebaseCode: String? = null

    fun seedDocuments(collection: String, docs: List<FirebaseDocument>) {
        val col = collections.getOrPut(collection) { mutableMapOf() }
        for (doc in docs) {
            col[doc.id] = doc
        }
    }

    override suspend fun getDocument(collection: String, documentId: String): Result<FirebaseDocument> {
        simulatedError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        simulatedFirebaseCode?.let { return Result.Failure(errorMapper.mapFirebaseCode(it, null)) }

        val doc = collections[collection]?.get(documentId)
        return if (doc != null) {
            Result.Success(doc)
        } else {
            Result.Failure(errorMapper.mapFirebaseCode("NOT_FOUND", "Document $documentId not found in $collection"))
        }
    }

    override suspend fun setDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any?>
    ): Result<Unit> {
        simulatedError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        simulatedFirebaseCode?.let { return Result.Failure(errorMapper.mapFirebaseCode(it, null)) }

        val col = collections.getOrPut(collection) { mutableMapOf() }
        val doc = FirebaseDocument(id = documentId, data = data)
        col[documentId] = doc
        notifyListeners(collection)
        return Result.Success(Unit)
    }

    override suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> {
        simulatedError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        simulatedFirebaseCode?.let { return Result.Failure(errorMapper.mapFirebaseCode(it, null)) }

        val col = collections[collection]
        col?.remove(documentId)
        notifyListeners(collection)
        return Result.Success(Unit)
    }

    override fun addSnapshotListener(
        collection: String,
        onUpdate: (documents: List<FirebaseDocument>?, error: Throwable?) -> Unit
    ): FirebaseListenerRegistration {
        val set = listeners.getOrPut(collection) { mutableSetOf() }
        set.add(onUpdate)

        // Immediately deliver current snapshot to the new listener
        val currentDocs = collections[collection]?.values?.toList() ?: emptyList()
        onUpdate(currentDocs, null)

        return FirebaseListenerRegistration {
            set.remove(onUpdate)
            if (set.isEmpty()) {
                listeners.remove(collection)
            }
        }
    }

    override fun observeCollection(collection: String): Flow<Result<List<FirebaseDocument>>> = callbackFlow {
        val registration = addSnapshotListener(collection) { docs, error ->
            if (error != null) {
                trySend(Result.Failure(errorMapper.mapThrowable(error)))
            } else if (docs != null) {
                trySend(Result.Success(docs))
            }
        }
        awaitClose {
            registration.remove()
        }
    }

    /**
     * Helper to observe a collection and map items directly into a domain [Result] of [BaseOutput].
     */
    fun <Output : BaseOutput> observeDomainCollection(
        collection: String,
        mapper: (List<FirebaseDocument>) -> Output
    ): Flow<Result<Output>> {
        return observeCollection(collection).map { result ->
            when (result) {
                is Result.Failure -> Result.Failure(result.exception)
                is Result.Success -> Result.Success(mapper(result.data))
            }
        }
    }

    fun emitUpdate(collection: String, docs: List<FirebaseDocument>) {
        val col = collections.getOrPut(collection) { mutableMapOf() }
        col.clear()
        for (doc in docs) {
            col[doc.id] = doc
        }
        notifyListeners(collection)
    }

    fun emitError(collection: String, throwable: Throwable) {
        val set = listeners[collection]?.toList() ?: return
        for (listener in set) {
            listener(null, throwable)
        }
    }

    private fun notifyListeners(collection: String) {
        val set = listeners[collection]?.toList() ?: return
        val currentDocs = collections[collection]?.values?.toList() ?: emptyList()
        for (listener in set) {
            listener(currentDocs, null)
        }
    }
}
