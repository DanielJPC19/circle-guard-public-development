package com.circleguard.promotion

import com.circleguard.promotion.model.graph.UserNode
import com.circleguard.promotion.repository.graph.UserNodeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(
    properties = [
        // JPA → H2 (no PostgreSQL container needed; Neo4j is the focus)
        "spring.datasource.url=jdbc:h2:mem:promotiontest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        // Neo4j credentials match the container defaults
        "spring.neo4j.authentication.username=neo4j",
        "spring.neo4j.authentication.password=password",
        // Prevent Kafka consumer from connecting
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.listener.auto-startup=false"
    ]
)
@Testcontainers
class UserNodeRepositoryIntegrationTest {

    @Autowired
    lateinit var userNodeRepository: UserNodeRepository

    @MockBean
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @MockBean
    lateinit var redisTemplate: StringRedisTemplate

    companion object {
        @Container
        @JvmStatic
        val neo4j = Neo4jContainer<Nothing>("neo4j:5.26")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri", neo4j::getBoltUrl)
        }
    }

    @Test
    fun `saved UserNode is retrievable with ACTIVE status`() {
        val id = UUID.randomUUID().toString()
        val node = UserNode.builder()
            .anonymousId(id)
            .status("ACTIVE")
            .build()

        userNodeRepository.save(node)
        val found = userNodeRepository.findById(id)

        assertTrue(found.isPresent, "Node must be found after save")
        assertEquals("ACTIVE", found.get().status)
    }

    @Test
    fun `saved UserNode with SUSPECT status is retrievable`() {
        val id = UUID.randomUUID().toString()
        val node = UserNode.builder()
            .anonymousId(id)
            .status("SUSPECT")
            .build()

        userNodeRepository.save(node)
        val found = userNodeRepository.findById(id)

        assertTrue(found.isPresent, "Node must be found after save")
        assertEquals("SUSPECT", found.get().status)
    }
}
