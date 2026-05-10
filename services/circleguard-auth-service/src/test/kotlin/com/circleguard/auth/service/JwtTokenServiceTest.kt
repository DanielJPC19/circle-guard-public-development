package com.circleguard.auth.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

class JwtTokenServiceTest {

    private lateinit var jwtService: JwtTokenService

    @BeforeEach
    fun setup() {
        jwtService = JwtTokenService(
            "my-super-secret-test-key-32-chars-long",
            3600000L
        )
    }

    @Test
    fun `generateToken produces non-null token with three dot-separated parts`() {
        val auth = UsernamePasswordAuthenticationToken(
            "user123", null, listOf(SimpleGrantedAuthority("ROLE_STUDENT"))
        )
        val token = jwtService.generateToken(UUID.randomUUID(), auth)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertEquals(3, token.split(".").size)
    }

    @Test
    fun `token generated with 1ms expiration still has valid structure`() {
        val shortLivedService = JwtTokenService(
            "my-super-secret-test-key-32-chars-long",
            1L
        )
        val auth = UsernamePasswordAuthenticationToken(
            "user123", null, listOf()
        )

        val token = shortLivedService.generateToken(UUID.randomUUID(), auth)
        Thread.sleep(10)

        assertNotNull(token)
        assertEquals(3, token.split(".").size, "JWT must have header.payload.signature")
    }
}
