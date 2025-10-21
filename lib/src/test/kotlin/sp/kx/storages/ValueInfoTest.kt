package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class ValueInfoTest {
    @Test
    fun toStringTest() {
        val id = UUID(0, 1)
        val created = 42.milliseconds
        val issuer = ValueInfo(
            id = id,
            created = created,
        )
        val expected = "ValueInfo(id=$id, created=$created)"
        assertEquals(expected, issuer.toString())
    }
}
