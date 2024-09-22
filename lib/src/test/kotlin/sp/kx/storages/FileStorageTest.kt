package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FileStorageTest {
    @Test
    fun interfaceTest() {
        val expected = "foobarbaz".toByteArray()
        val meta = mockMetadata(1)
        val storage: FileStorage = MockFileStorage(
            values = mapOf(meta to expected),
        )
        assertTrue(expected.contentEquals(storage.getBytes(id = mockUUID(1))))
    }
}
