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
        assertEquals(deleted.size, this.deleted.size)
        assertEquals(deleted, this.deleted)
        assertEquals(hash, this.hash)
        assertEquals(items.size, this.items.size)
        items.forEachIndexed { index, expected ->
            val actual = this.items[index]
            assertEquals(expected.id, actual.id)
            assertEquals(expected.info, actual.info, "id: ${expected.id}")
            assertEquals(expected, actual)
        }
        assertEquals(items, this.items)
    }

    private fun SyncInfo.assert(
        meta: Map<UUID, ItemInfo> = emptyMap(),
        deleted: Set<UUID> = emptySet(),
    ) {
        assertEquals(meta.size, this.meta.size)
        meta.forEach { (itemId, expected) ->
            val actual = this.meta[itemId] ?: error("No item info!")
            assertEquals(expected, actual, "id: $itemId")
        }
        assertEquals(deleted.size, this.deleted.size)
        assertEquals(deleted, this.deleted)
    }

    private fun MergeInfo.assert(
        download: Set<UUID>,
        items: List<Described<ByteArray>>,
        deleted: Set<UUID>,
    ) {
        assertEquals(deleted.size, this.deleted.size)
        assertEquals(deleted, this.deleted)
        assertEquals(download.size, this.download.size)
        assertEquals(download, this.download)
        assertEquals(items.size, this.items.size)
        items.forEachIndexed { index, expected ->
            val actual = this.items[index]
            assertEquals(expected.id, actual.id)
            assertEquals(expected.info, actual.info, "id: ${expected.id}")
            assertEquals(expected, actual)
        }
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
    fun hashTest() {
        val storageId = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val defaultItems = (0..3).map { index ->
            Described(
                id = mockUUID(index),
                info = ItemInfo(
                    created = (1_000 + index).milliseconds,
                    updated = (1_000 + index).milliseconds,
                    hash = "item:hash:$index"
                ),
                item = "item:$index",
            )
        }
        check(defaultItems.size == 4)
        val updatedItems = mutableListOf<Described<String>>().also {
            it.add(defaultItems[0].copy(updated = (2_000 + 0).milliseconds, hash = "item:hash:0:updated", item = "item:0:updated"))
            it.add(defaultItems[2])
            it.add(defaultItems[3])
        }
        check(updatedItems.size == 3)
        check(defaultItems[0] != updatedItems[0])
        check(defaultItems[2] == updatedItems[1])
        check(defaultItems[3] == updatedItems[2])
        val storageHash = "storageHash"
        val storageHashUpdated = "storageHashUpdated"
        val hashes = defaultItems.map {
            it.item.toByteArray() to it.info.hash
        } + updatedItems.map {
            it.item.toByteArray() to it.info.hash
        } + listOf(
            defaultItems.joinToString(separator = "") { it.info.hash }.toByteArray() to storageHash,
            updatedItems.joinToString(separator = "") { it.info.hash }.toByteArray() to storageHashUpdated,
        )
        val transformer = defaultItems.map {
            it.item.toByteArray() to it.item
        } + updatedItems.map {
            it.item.toByteArray() to it.item
        }
        val storage: SyncStorage<String> = MockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        defaultItems.forEachIndexed { index, described ->
            itemId = described.id
            time = (1_000 + index).milliseconds
            storage.add(described.item)
        }
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = defaultItems,
        )
        //
        time = (2_000 + 0).milliseconds
        assertEquals(updatedItems[0].info, storage.update(defaultItems[0].id, updatedItems[0].item))
        assertTrue(storage.delete(defaultItems[1].id))
        storage.assert(
            id = storageId,
            hash = storageHashUpdated,
            items = updatedItems,
            deleted = setOf(defaultItems[1].id),
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
        val storageId = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = "itemHash"
        val time = 1.milliseconds
        val timeProvider = mockProvider { time }
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val expected = Described(
            id = itemId,
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
            id = storageId,
            hashes = listOf(
                "".toByteArray() to storageEmptyHash,
                expected.item.toByteArray() to itemHash,
                itemHash.toByteArray() to storageHash,
            ),
            uuidProvider = uuidProvider,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.item.toByteArray() to expected.item,
            ),
        )
        storage.assert(
            id = storageId,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.item))
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.id)
        assertFalse(storage.delete(id = notExists))
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = listOf(expected),
        )
        assertTrue(storage.delete(id = expected.id))
        storage.assert(
            id = storageId,
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
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val expected = Described(
            id = itemId,
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
            uuidProvider = uuidProvider,
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
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val expected = Described(
            id = itemId,
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
            uuidProvider = uuidProvider,
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
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val expected = Described(
            id = itemId,
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
            uuidProvider = uuidProvider,
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
        storage.getSyncInfo().assert()
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
        storage.getSyncInfo().assert(meta = mapOf(updated.id to updated.info))
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
        storage.getSyncInfo().assert(deleted = setOf(expected.id))
    }

    @Test
    fun getMergeInfoTest() {
        val storageId = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = mockProvider { itemId }
        val defaultItems = (0..3).map { index ->
            Described(
                id = mockUUID(index),
                info = ItemInfo(
                    created = (1_000 + index).milliseconds,
                    updated = (1_000 + index).milliseconds,
                    hash = "item:hash:$index"
                ),
                item = "item:$index",
            )
        }
        check(defaultItems.size == 4)
        val rItems = mutableListOf<Described<String>>().also {
            it.add(defaultItems[0].copy(updated = (2_000 + 0).milliseconds, hash = "item:hash:0:updated", item = "item:0:updated"))
            it.add(defaultItems[2])
            it.add(defaultItems[3])
        }
        check(rItems.size == 3)
        val tItems = mutableListOf<Described<String>>().also {
            it.add(defaultItems[0])
            it.add(defaultItems[1])
            it.add(defaultItems[3].copy(updated = (3_000 + 0).milliseconds, hash = "item:hash:3:updated", item = "item:3:updated"))
        }
        check(tItems.size == 3)
        val storageHash = "storageHash"
        val storageRHash = "storageRHash"
        val storageTHash = "storageTHash"
        val hashes = defaultItems.map {
            it.item.toByteArray() to it.info.hash
        } + rItems.map {
            it.item.toByteArray() to it.info.hash
        } + tItems.map {
            it.item.toByteArray() to it.info.hash
        } + listOf(
            defaultItems.joinToString(separator = "") { it.info.hash }.toByteArray() to storageHash,
            rItems.joinToString(separator = "") { it.info.hash }.toByteArray() to storageRHash,
            tItems.joinToString(separator = "") { it.info.hash }.toByteArray() to storageTHash,
        )
        val transformer = defaultItems.map {
            it.item.toByteArray() to it.item
        } + rItems.map {
            it.item.toByteArray() to it.item
        } + tItems.map {
            it.item.toByteArray() to it.item
        }
        val rStorage: SyncStorage<String> = MockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        val tStorage: SyncStorage<String> = MockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        defaultItems.forEachIndexed { index, described ->
            itemId = described.id
            time = (1_000 + index).milliseconds
            rStorage.add(described.item)
            tStorage.add(described.item)
        }
        rStorage.assert(
            id = storageId,
            hash = storageHash,
            items = defaultItems,
        )
        tStorage.assert(
            id = storageId,
            hash = storageHash,
            items = defaultItems,
        )
        //
        time = (2_000 + 0).milliseconds
        assertEquals(rItems[0].info, rStorage.update(defaultItems[0].id, rItems[0].item))
        assertTrue(rStorage.delete(defaultItems[1].id))
        rStorage.assert(
            id = storageId,
            hash = storageRHash,
            items = rItems,
            deleted = setOf(defaultItems[1].id),
        )
        //
        time = (3_000 + 0).milliseconds
        assertEquals(tItems[2].info, tStorage.update(defaultItems[3].id, tItems[2].item))
        assertTrue(tStorage.delete(defaultItems[2].id))
        tStorage.assert(
            id = storageId,
            hash = storageTHash,
            items = tItems,
            deleted = setOf(defaultItems[2].id),
        )
        //
        val rSyncInfo = rStorage.getSyncInfo()
        rSyncInfo.assert(meta = rItems.associate { it.id to it.info }, deleted = setOf(defaultItems[1].id))
        val tMergeInfo = tStorage.getMergeInfo(rSyncInfo)
        tMergeInfo.assert(
            download = setOf(defaultItems[0].id),
            items = listOf(
                tItems[2].map { it.toByteArray() },
            ),
            deleted = setOf(defaultItems[2].id),
        )
        //
        val tSyncInfo = tStorage.getSyncInfo()
        tSyncInfo.assert(meta = tItems.associate { it.id to it.info }, deleted = setOf(defaultItems[2].id))
        val rMergeInfo = rStorage.getMergeInfo(tSyncInfo)
        rMergeInfo.assert(
            download = setOf(defaultItems[3].id),
            items = listOf(
                rItems[0].map { it.toByteArray() },
            ),
            deleted = setOf(defaultItems[1].id),
        )
    }
}
