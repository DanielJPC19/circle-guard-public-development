package com.circleguard.identity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

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
class IdentityVaultIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockBean
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16").apply {
            withDatabaseName("circleguard_identity")
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
    fun `POST map returns a valid UUID for a new real identity`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"realIdentity":"test.student@university.edu"}"""

        val response = restTemplate.exchange(
            "/api/v1/identities/map", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val anonymousId = response.body?.get("anonymousId") as? String
        assertNotNull(anonymousId)
        assertDoesNotThrow { java.util.UUID.fromString(anonymousId) }
    }

    @Test
    fun `POST map with the same identity returns the same anonymousId`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"realIdentity":"idempotent.student@university.edu"}"""

        val resp1 = restTemplate.exchange(
            "/api/v1/identities/map", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )
        val resp2 = restTemplate.exchange(
            "/api/v1/identities/map", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )

        assertEquals(HttpStatus.OK, resp1.statusCode)
        assertEquals(HttpStatus.OK, resp2.statusCode)
        assertEquals(
            resp1.body?.get("anonymousId"),
            resp2.body?.get("anonymousId"),
            "Same identity must always map to the same anonymousId"
        )
    }
}
