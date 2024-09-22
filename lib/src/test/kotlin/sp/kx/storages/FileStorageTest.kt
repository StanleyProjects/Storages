package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FileStorageTest {
    @Test
    fun interfaceTest() {
        val expected = "foobarbaz".toByteArray()
        val storage: FileStorage = MockFileStorage(
            values = mapOf(
                Raw(id = mockUUID(1), info = mockItemInfo(1)) to expected,
            ),
        )
        assertTrue(expected.contentEquals(storage.getBytes(id = mockUUID(1))))
    }
}
