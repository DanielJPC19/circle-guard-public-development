package com.circleguard.auth.controller

import com.circleguard.auth.client.IdentityClient
import com.circleguard.auth.security.SecurityConfig
import com.circleguard.auth.service.CustomUserDetailsService
import com.circleguard.auth.service.JwtTokenService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@WebMvcTest(LoginController::class)
@Import(SecurityConfig::class)
class LoginControllerKtTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockBean lateinit var authManager: AuthenticationManager
    @MockBean lateinit var jwtService: JwtTokenService
    @MockBean lateinit var identityClient: IdentityClient
    @MockBean lateinit var userDetailsService: CustomUserDetailsService

    @Test
    fun `login with valid credentials returns HTTP 200 with token and anonymousId`() {
        val anonymousId = UUID.randomUUID()
        val auth = UsernamePasswordAuthenticationToken("admin", null, listOf())

        whenever(authManager.authenticate(any())).thenReturn(auth)
        whenever(identityClient.getAnonymousId("admin")).thenReturn(anonymousId)
        whenever(jwtService.generateToken(eq(anonymousId), any())).thenReturn("header.payload.signature")

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"admin","password":"password"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("header.payload.signature"))
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.anonymousId").value(anonymousId.toString()))
    }

    @Test
    fun `login with invalid credentials returns HTTP 401`() {
        whenever(authManager.authenticate(any()))
            .thenThrow(BadCredentialsException("Invalid credentials"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"wrong","password":"wrong"}""")
        )
            .andExpect(status().isUnauthorized)
    }
}
