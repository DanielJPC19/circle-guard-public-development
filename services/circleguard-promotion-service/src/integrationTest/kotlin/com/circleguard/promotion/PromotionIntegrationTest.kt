package com.circleguard.promotion

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.neo4j.uri=bolt://localhost:7687",
        "spring.neo4j.authentication.username=neo4j",
        "spring.neo4j.authentication.password=password",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.listener.auto-startup=false"
    ]
)
@TestPropertySource(properties = [
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
])
class PromotionIntegrationTest {

    @MockBean
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @MockBean
    lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun `context loads with mocked external dependencies`() {
        assertNotNull(kafkaTemplate)
        assertNotNull(redisTemplate)
    }

    @Test
    fun `mock services are properly configured`() {
        assertNotNull(kafkaTemplate)
        assertNotNull(redisTemplate)
    }
}