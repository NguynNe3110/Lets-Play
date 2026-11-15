package com.uzuu.letplay.data.integration.firebase

import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Adapter contract for Firebase operations, supporting Firestore / Realtime DB document CRUD,
 * realtime observation via [Flow], and explicit cancellation/cleanup of listener registrations.
 */
interface FirebaseAdapter {
    val config: FirebaseConfig
    val activeListenersCount: Int

    suspend fun getDocument(collection: String, documentId: String): Result<FirebaseDocument>
    suspend fun setDocument(collection: String, documentId: String, data: Map<String, Any?>): Result<Unit>
    suspend fun deleteDocument(collection: String, documentId: String): Result<Unit>

    fun addSnapshotListener(
        collection: String,
        onUpdate: (documents: List<FirebaseDocument>?, error: Throwable?) -> Unit
    ): FirebaseListenerRegistration

    fun observeCollection(collection: String): Flow<Result<List<FirebaseDocument>>>
}
