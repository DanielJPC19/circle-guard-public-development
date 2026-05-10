package com.circleguard.form

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
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
    ]
)
@Testcontainers
class SurveyIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockBean
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16").apply {
            withDatabaseName("circleguard_form")
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
    fun `POST survey persists the record and returns it with an assigned id`() {
        val anonymousId = UUID.randomUUID().toString()
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"anonymousId":"$anonymousId","hasFever":true,"hasCough":false}"""

        val response = restTemplate.exchange(
            "/api/v1/surveys", HttpMethod.POST, HttpEntity(body, headers), Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.get("id"), "Saved survey must have an assigned id")
        assertEquals(anonymousId, response.body?.get("anonymousId").toString())
        assertEquals(true, response.body?.get("hasFever"))
    }

    @Test
    fun `GET questionnaires returns HTTP 200 with a list`() {
        val response = restTemplate.getForEntity("/api/v1/questionnaires", List::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }
}
