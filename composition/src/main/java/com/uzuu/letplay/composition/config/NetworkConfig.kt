package com.uzuu.letplay.composition.config

/**
 * Network values supplied by the application composition root.
 * The application validates its environment config before providing this value.
 */
data class NetworkConfig(val apiBaseUrl: String)
