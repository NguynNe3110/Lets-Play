package com.uzuu.letplay.config

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
        val ex = assertFailsWith<IllegalArgumentException> {
            AppEnvironment.from("staging")
        }
        assertTrue(ex.message?.contains("Unknown environment: 'staging'") == true)
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
    fun appConfigValidationAcceptsVariousValidProductionAndDevUrls() {
        // Standard HTTPS production
        val prodConfig = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "https://api.example.com/",
            isDebug = false
        )
        assertEquals("https://api.example.com/", prodConfig.apiBaseUrl)
        assertTrue(prodConfig.environment.isProd)

        // Custom port and subpath
        val customPortConfig = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "https://api.example.com:8443/v1/api/",
            isDebug = false
        )
        assertEquals("https://api.example.com:8443/v1/api/", customPortConfig.apiBaseUrl)

        // Local HTTP allowed in production for local testing
        val localConfig = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "http://localhost:8080/",
            isDebug = false
        )
        assertEquals("http://localhost:8080/", localConfig.apiBaseUrl)

        val loopbackConfig = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "http://127.0.0.1:3000/",
            isDebug = false
        )
        assertEquals("http://127.0.0.1:3000/", loopbackConfig.apiBaseUrl)

        val emulatorConfig = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "http://10.0.2.2:8000/",
            isDebug = false
        )
        assertEquals("http://10.0.2.2:8000/", emulatorConfig.apiBaseUrl)

        // Dev environment allows remote HTTP
        val devHttpConfig = AppConfig(
            environment = AppEnvironment.DEV,
            apiBaseUrl = "http://internal-dev.company.net/",
            isDebug = true
        )
        assertEquals("http://internal-dev.company.net/", devHttpConfig.apiBaseUrl)
    }

    @Test
    fun appConfigValidationRejectsMissingTrailingSlash() {
        val ex = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "https://jsonplaceholder.typicode.com",
                isDebug = false
            )
        }
        assertTrue(ex.message?.contains("trailing slash") == true)
    }

    @Test
    fun appConfigValidationRejectsInvalidScheme() {
        val exFtp = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "ftp://jsonplaceholder.typicode.com/",
                isDebug = false
            )
        }
        assertTrue(exFtp.message?.contains("http or https scheme") == true)

        val exWs = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.DEV,
                apiBaseUrl = "ws://jsonplaceholder.typicode.com/",
                isDebug = true
            )
        }
        assertTrue(exWs.message?.contains("http or https scheme") == true)

        val exFile = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.DEV,
                apiBaseUrl = "file:///sdcard/api/",
                isDebug = true
            )
        }
        assertTrue(exFile.message?.contains("http or https scheme") == true)
    }

    @Test
    fun appConfigValidationRejectsBlankUrl() {
        val ex = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "   ",
                isDebug = false
            )
        }
        assertTrue(ex.message?.contains("must not be blank") == true)
    }

    @Test
    fun appConfigValidationRejectsMissingHost() {
        // Missing host with https:///
        val exTripleHttps = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "https:///",
                isDebug = false
            )
        }
        assertTrue(exTripleHttps.message?.contains("valid non-empty host") == true)

        // Missing host with http:///
        val exTripleHttp = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.DEV,
                apiBaseUrl = "http:///",
                isDebug = true
            )
        }
        assertTrue(exTripleHttp.message?.contains("valid non-empty host") == true)

        // Port without host
        val exPortOnly = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.DEV,
                apiBaseUrl = "https://:8080/",
                isDebug = true
            )
        }
        assertTrue(exPortOnly.message?.contains("valid non-empty host") == true)
    }

    @Test
    fun appConfigValidationRejectsMalformedUri() {
        val ex = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.DEV,
                apiBaseUrl = "https://invalid host with spaces/",
                isDebug = true
            )
        }
        assertTrue(ex.message?.contains("not a valid URI") == true)
    }

    @Test
    fun appConfigProductionReleaseRejectsNonLocalHttp() {
        // Prod release rejects remote HTTP
        val ex = assertFailsWith<IllegalArgumentException> {
            AppConfig(
                environment = AppEnvironment.PROD,
                apiBaseUrl = "http://api.example.com/",
                isDebug = false
            )
        }
        assertTrue(ex.message?.contains("Production release requires HTTPS") == true)

        // Prod debug permits remote HTTP for developer debugging / proxies
        val debugProd = AppConfig(
            environment = AppEnvironment.PROD,
            apiBaseUrl = "http://api.example.com/",
            isDebug = true
        )
        assertEquals("http://api.example.com/", debugProd.apiBaseUrl)
    }

    @Test
    fun resolveBaseUrlFollowsPropertyEnvPrecedence() {
        val propertyKey = "PROD_API_BASE_URL"

        // 1. Property takes highest precedence over env and default
        val fromProp = AppConfig.resolveBaseUrl(
            key = propertyKey,
            propertyLookup = { "https://property.example.com" },
            envLookup = { "https://env.example.com" },
            defaultUrl = "https://default.example.com/"
        )
        assertEquals("https://property.example.com/", fromProp)

        // 2. Env takes precedence when property is absent
        val fromEnv = AppConfig.resolveBaseUrl(
            key = propertyKey,
            propertyLookup = { null },
            envLookup = { "https://env.example.com" },
            defaultUrl = "https://default.example.com/"
        )
        assertEquals("https://env.example.com/", fromEnv)

        // 3. Env takes precedence when property is empty
        val fromEmptyProp = AppConfig.resolveBaseUrl(
            key = propertyKey,
            propertyLookup = { "   " },
            envLookup = { "https://env.example.com" },
            defaultUrl = "https://default.example.com/"
        )
        assertEquals("https://env.example.com/", fromEmptyProp)

        // 4. Default is used when both property and env are absent
        val fromDefault = AppConfig.resolveBaseUrl(
            key = propertyKey,
            propertyLookup = { null },
            envLookup = { null },
            defaultUrl = "https://default.example.com/"
        )
        assertEquals("https://default.example.com/", fromDefault)
    }

    @Test
    fun normalizeUrlEnsuresTrailingSlashAndRejectsBlank() {
        assertEquals("https://api.example.com/", AppConfig.normalizeUrl("https://api.example.com"))
        assertEquals("https://api.example.com/", AppConfig.normalizeUrl("https://api.example.com/"))
        assertEquals("https://api.example.com/", AppConfig.normalizeUrl("  https://api.example.com  "))

        assertFailsWith<IllegalArgumentException> {
            AppConfig.normalizeUrl("   ")
        }
    }
}
