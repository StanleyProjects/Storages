package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStreamsStorageTest {
    private fun <T : Any> Storage<T>.assert(
        id: UUID,
        deleted: Set<UUID> = emptySet(),
        hash: String,
        items: List<Described<T>> = emptyList(),
    ) {
        assertEquals(id, this.id)
        assertEquals(deleted, this.deleted)
        assertEquals(hash, this.hash)
        assertEquals(items, this.items)
    }

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
        storage.assert(
            id = id,
            hash = storageHash,
        )
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
        storage.assert(
            id = id,
            deleted = deleted,
            hash = storageHash,
        )
    }

    @Test
    fun deleteTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val time = 1.milliseconds
        val timeProvider = mockProvider { time }
        val expected = Described(
            id = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935"),
            info = ItemInfo(
                created = time,
                updated = time,
                hash = itemHash,
            ),
            item = "foobar",
        )
        val storageEmptyHash = "storageEmptyHash"
        val storageHash = "storageHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageEmptyHash,
                expected.item.toByteArray() to itemHash,
                itemHash.toByteArray() to storageHash,
            ),
            randomUUID = expected.id,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.item))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.id)
        assertFalse(storage.delete(id = notExists))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        assertTrue(storage.delete(id = expected.id))
        storage.assert(
            id = id,
            hash = storageEmptyHash,
            deleted = setOf(expected.id),
        )
    }

    @Test
    fun addTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val time = 1.milliseconds
        val timeProvider = mockProvider { time }
        val expected = Described(
            id = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935"),
            info = ItemInfo(
                created = time,
                updated = time,
                hash = itemHash,
            ),
            item = "foobar",
        )
        val storageEmptyHash = "storageEmptyHash"
        val storageHash = "storageHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageEmptyHash,
                expected.item.toByteArray() to itemHash,
                itemHash.toByteArray() to storageHash,
            ),
            randomUUID = expected.id,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.item))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
    }

    @Test
    fun updateTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val itemUpdated = "itemUpdated"
        val itemUpdatedHash = "itemUpdatedHash"
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        val expected = Described(
            id = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935"),
            info = ItemInfo(
                created = time,
                updated = time,
                hash = itemHash,
            ),
            item = "foobar",
        )
        val storageEmptyHash = "storageEmptyHash"
        val storageHash = "storageHash"
        val storageUpdatedHash = "storageUpdatedHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageEmptyHash,
                expected.item.toByteArray() to itemHash,
                itemUpdated.toByteArray() to itemUpdatedHash,
                itemHash.toByteArray() to storageHash,
                itemUpdatedHash.toByteArray() to storageUpdatedHash,
            ),
            randomUUID = expected.id,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
                itemUpdated.toByteArray() to itemUpdated,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.item))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.id)
        assertNull(storage.update(id = notExists, item = itemUpdated))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        time = 2.milliseconds
        val updated = Described(
            id = expected.id,
            info = ItemInfo(
                created = expected.info.created,
                updated = time,
                hash = itemUpdatedHash,
            ),
            item = itemUpdated,
        )
        assertEquals(
            updated.info,
            storage.update(id = expected.id, item = itemUpdated),
        )
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
    }

    @Test
    fun mergeTest() {TODO("SyncStreamsStorageTest:mergeTest")}

    @Test
    fun getSyncInfoTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val itemUpdated = "itemUpdated"
        val itemUpdatedHash = "itemUpdatedHash"
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        val expected = Described(
            id = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935"),
            info = ItemInfo(
                created = time,
                updated = time,
                hash = itemHash,
            ),
            item = "foobar",
        )
        val storageEmptyHash = "storageEmptyHash"
        val storageHash = "storageHash"
        val storageUpdatedHash = "storageUpdatedHash"
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageEmptyHash,
                expected.item.toByteArray() to itemHash,
                itemUpdated.toByteArray() to itemUpdatedHash,
                itemHash.toByteArray() to storageHash,
                itemUpdatedHash.toByteArray() to storageUpdatedHash,
            ),
            randomUUID = expected.id,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
                itemUpdated.toByteArray() to itemUpdated,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(SyncInfo(meta = emptyMap(), deleted = emptySet()), storage.getSyncInfo())
        assertEquals(expected, storage.add(expected.item))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        assertEquals(SyncInfo(meta = mapOf(expected.id to expected.info), deleted = emptySet()), storage.getSyncInfo())
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.id)
        assertNull(storage.update(id = notExists, item = itemUpdated))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        time = 2.milliseconds
        val updated = Described(
            id = expected.id,
            info = ItemInfo(
                created = expected.info.created,
                updated = time,
                hash = itemUpdatedHash,
            ),
            item = itemUpdated,
        )
        assertEquals(
            updated.info,
            storage.update(id = expected.id, item = itemUpdated),
        )
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
        assertEquals(SyncInfo(meta = mapOf(updated.id to updated.info), deleted = emptySet()), storage.getSyncInfo())
        assertFalse(storage.delete(id = notExists))
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
        assertTrue(storage.delete(id = expected.id))
        storage.assert(
            id = id,
            hash = storageEmptyHash,
            deleted = setOf(expected.id),
        )
        assertEquals(SyncInfo(meta = emptyMap(), deleted = setOf(expected.id)), storage.getSyncInfo())
    }

    @Test
    fun getMergeInfoTest() {TODO("SyncStreamsStorageTest:getMergeInfoTest")}
}
