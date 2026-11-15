package com.uzuu.letplay.data.integration.firebase

/**
 * Representation of a document snapshot from Firebase Firestore / Realtime DB.
 */
data class FirebaseDocument(
    val id: String,
    val data: Map<String, Any?>
)

/**
 * Handle returned when attaching a realtime snapshot listener.
 * Invoking [remove] detaches the listener and frees associated resources cleanly.
 */
fun interface FirebaseListenerRegistration {
    fun remove()
}
