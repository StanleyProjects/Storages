package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class StreamsStorageTest {
    @Test
    fun createTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val storageHash = "storageHash"
        val storage = MockStreamsStorage<String>(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageHash,
            ),
        )
        assertEquals(id, storage.id)
        assertEquals(storageHash, storage.hash)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun addTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val item = "foobar"
        val created = 1.milliseconds
        val itemId = UUID.fromString("a9971314-2b26-4704-b145-f2473a7e068c")
        val itemHash = "itemHash"
        val storageHash = "storageHash"
        val storage = MockStreamsStorage<String>(
            id = id,
            now = created,
            randomUUID = itemId,
            hashes = listOf(
                item.toByteArray() to itemHash,
                itemHash.toByteArray() to storageHash,
            ),
            transformer = listOf(item.toByteArray() to item),
        )
        val described = storage.add(item)
        assertEquals(itemId, described.id)
        assertEquals(created, described.info.created)
        assertEquals(created, described.info.updated)
        assertEquals(itemHash, described.info.hash)
        assertEquals(item, described.item)
        assertEquals(storageHash, storage.hash)
        assertTrue(storage.deleted.isEmpty())
        assertEquals(described, storage.items.single())
    }
}
