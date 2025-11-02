package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class PayloadTest {
    @Test
    fun toStringTest() {
        val id = UUID(0, 1)
        val created = 1.milliseconds
        val updated = 2.milliseconds
        val issuer = Payload(
            id = id,
            created = created,
            updated = updated,
            value = "foo bar baz",
        )
        val expected = "Payload(id: $id, created: $created, updated: $updated, value: String)"
        assertEquals(expected, issuer.toString())
    }

    @Test
    fun getTest() {
        val id = UUID(0, 1)
        val created = 1.milliseconds
        val updated = 2.milliseconds
        val value = "foo bar baz"
        val issuer = Payload(
            id = id,
            created = created,
            updated = updated,
            value = value,
        )
        assertEquals(id, issuer.id)
        assertEquals(created, issuer.created)
        assertEquals(updated, issuer.updated)
        assertEquals(value, issuer.value)
    }
}
