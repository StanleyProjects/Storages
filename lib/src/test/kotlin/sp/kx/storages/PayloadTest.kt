package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class PayloadTest {
    @Test
    fun toStringTest() {
        val valueInfo = ValueInfo(
            id = UUID(0, 1),
            created = 1.milliseconds,
        )
        val valueState = ValueState(
            updated = 2.milliseconds,
            hash = byteArrayOf(4, 3, 2, 1),
        )
        val issuer = Payload(
            value = "foo bar baz",
            valueInfo = valueInfo,
            valueState = valueState,
        )
        val expected = "Payload(value: String, valueInfo: $valueInfo, valueState: $valueState)"
        assertEquals(expected, issuer.toString())
    }
}
