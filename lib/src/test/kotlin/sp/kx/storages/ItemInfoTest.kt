package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

internal class ItemInfoTest {
    @Test
    fun equalsTest() {
        val expected = mockItemInfo()
        val actual = mockItemInfo()
        assertTrue(expected == actual)
    }

    @Test
    fun equalsNotTest() {
        val expected: Any = mockItemInfo()
        assertFalse(expected == mockItemInfo(created = 11.milliseconds))
        assertFalse(expected == mockItemInfo(updated = 12.milliseconds))
        assertFalse(expected == mockItemInfo(hash = "42"))
        assertFalse(expected == "42")
    }
}
