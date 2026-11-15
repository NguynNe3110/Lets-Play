package com.example.base.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {

    @Test
    fun appEnvironmentFromParsesKnownEnvironments() {
        assertEquals(AppEnvironment.DEV, AppEnvironment.from("dev"))
        assertEquals(AppEnvironment.DEV, AppEnvironment.from("DEV"))
        assertEquals(AppEnvironment.DEV, AppEnvironment.from(" dev "))
        assertEquals(AppEnvironment.PROD, AppEnvironment.from("prod"))
        assertEquals(AppEnvironment.PROD, AppEnvironment.from("PROD"))
        assertEquals(AppEnvironment.PROD, AppEnvironment.from(" prod "))
    }

    @Test
    fun appEnvironmentFromThrowsOnUnknown() {
        assertFailsWith<IllegalArgumentException> {
            AppEnvironment.from("staging")
        }
    }

    @Test
    fun appConfigValidationAcceptsValidUrls() {
        val config = AppConfig(
            environment = AppEnvironment.DEV,
            apiBaseUrl = "https://jsonplaceholder.typicode.com/",
            isDebug = true
        )
        assertEquals(AppEnvironment.DEV, config.environment)
        assertEquals("https://jsonplaceholder.typicode.com/", config.apiBaseUrl)
        assertTrue(config.isDebug)
        assertTrue(config.environment.isDev)
        assertFalse(config.environment.isProd)
    }

    @Test
    fun appConfigValidationRejectsMissingTrailingSlash() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "https://jsonplaceholder.typicode.com",
                isDebug = false
            )
        }
    }

    @Test
    fun appConfigValidationRejectsInvalidScheme() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "ftp://jsonplaceholder.typicode.com/",
                isDebug = false
            )
        }
    }

    @Test
    fun appConfigValidationRejectsBlankUrl() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "   ",
                isDebug = false
            )
        }
    }
}
