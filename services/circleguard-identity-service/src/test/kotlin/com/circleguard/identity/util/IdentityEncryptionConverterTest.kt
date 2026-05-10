package com.circleguard.identity.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdentityEncryptionConverterTest {

    // Use the same values as application.yml (vault.secret / vault.salt)
    private val converter = IdentityEncryptionConverter(
        "746573742d7365637265742d33322d63686172732d6c6f6e672d313233343536",
        "deadbeef"
    )

    @Test
    fun `encrypt then decrypt round-trip preserves original value`() {
        val original = "john.doe@university.edu"

        val encrypted = converter.convertToDatabaseColumn(original)
        val decrypted = converter.convertToEntityAttribute(encrypted)

        assertEquals(original, decrypted)
    }

    @Test
    fun `different plaintext inputs produce different ciphertext`() {
        val encrypted1 = converter.convertToDatabaseColumn("user1@university.edu")
        val encrypted2 = converter.convertToDatabaseColumn("user2@university.edu")

        assertFalse(encrypted1.contentEquals(encrypted2))
    }
}
