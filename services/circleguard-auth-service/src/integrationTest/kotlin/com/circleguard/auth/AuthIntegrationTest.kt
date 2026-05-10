package com.circleguard.auth

import com.circleguard.auth.client.IdentityClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

// Gradle integrationTest task injects DOCKER_HOST and DOCKER_API_VERSION via environment/systemProperty

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate"
    ]
)
@Testcontainers
class AuthIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockBean
    lateinit var identityClient: IdentityClient

    companion object {

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16").apply {
            withDatabaseName("circleguard_auth")
            withUsername("admin")
            withPassword("password")
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Test
    fun `valid local credentials return HTTP 200 with a JWT token`() {
        val anonymousId = UUID.randomUUID()
        whenever(identityClient.getAnonymousId(any())).thenReturn(anonymousId)

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"username":"staff_guard","password":"password"}"""
        val response = restTemplate.exchange(
            "/api/v1/auth/login", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val token = response.body?.get("token") as? String
        assertNotNull(token, "Response body must contain a token")
        assertEquals(3, token!!.split(".").size, "JWT must have header.payload.signature")
        assertEquals("Bearer", response.body?.get("type"))
    }

    @Test
    fun `invalid credentials return HTTP 401`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"username":"staff_guard","password":"wrong-password"}"""
        val response = restTemplate.exchange(
            "/api/v1/auth/login", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
