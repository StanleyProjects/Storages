package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStreamsStorageTest {
    @Test
    fun idTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val storageHash = "storageHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
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
    fun deletedTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val deleted = setOf(UUID.fromString("ed529649-c13b-4caf-b290-710da103bd25"))
        val storageHash = "storageHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageHash,
            ),
            defaultDeleted = deleted,
        )
        assertEquals(id, storage.id)
        assertEquals(storageHash, storage.hash)
        assertEquals(deleted, storage.deleted)
    }

    @Test
    fun deleteTest() {TODO("SyncStreamsStorageTest:deleteTest")}

    @Test
    fun addTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val now = 1.milliseconds
        val expected = Described(
            id = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935"),
            info = ItemInfo(
                created = now,
                updated = now,
                hash = itemHash,
            ),
            item = "foobar",
        )
        val storageHash = "storageHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                expected.item.toByteArray() to itemHash,
                itemHash.toByteArray() to storageHash,
            ),
            randomUUID = expected.id,
            now = now,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
            ),
        )
        assertEquals(id, storage.id)
        assertTrue(storage.items.isEmpty())
        assertTrue(storage.deleted.isEmpty())
        val actual = storage.add(expected.item)
        assertEquals(expected, actual)
        assertEquals(id, storage.id)
        assertEquals(storageHash, storage.hash)
        assertTrue(storage.deleted.isEmpty())
        assertEquals(expected, storage.items.single())
    }

    @Test
    fun updateTest() {TODO("SyncStreamsStorageTest:updateTest")}

    @Test
    fun mergeTest() {TODO("SyncStreamsStorageTest:mergeTest")}

    @Test
    fun getSyncInfoTest() {TODO("SyncStreamsStorageTest:getSyncInfoTest")}

    @Test
    fun getMergeInfoTest() {TODO("SyncStreamsStorageTest:getMergeInfoTest")}
}
