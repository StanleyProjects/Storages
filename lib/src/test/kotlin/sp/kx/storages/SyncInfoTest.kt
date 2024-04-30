package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SyncInfoTest {
    @Test
    fun createTest() {
        val value = SyncInfo(
            meta = emptyMap(),
            deleted = emptySet(),
        )
        assertTrue(value.meta.isEmpty())
        assertTrue(value.deleted.isEmpty())
    }
}
