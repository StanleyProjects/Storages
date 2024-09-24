package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toHEX
import sp.kx.storages.SyncStreamsStoragesTest.Companion.map
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStreamsStorageTest {
    companion object {
        fun assert(
            expected: Payload<out Any>,
            actual: Payload<out Any>,
        ) {
            assertEquals(expected.meta.id, actual.meta.id)
            assertEquals(expected.meta.created, actual.meta.created)
            assert(expected.meta.info, actual.meta.info)
            assertEquals(expected.value, actual.value, "payload: ${expected.meta.id}\n")
        }

        fun assert(
            expected: MergeInfo,
            actual: MergeInfo,
        ) {
            actual.assert(
                downloaded = expected.downloaded,
                items = expected.items,
                deleted = expected.deleted,
            )
        }

        private fun MergeInfo.assert(
            downloaded: Set<UUID>,
            items: List<RawPayload>,
            deleted: Set<UUID>,
        ) {
            assertEquals(deleted.size, this.deleted.size, "deleted:\n$deleted\n${this.deleted}\n")
            assertEquals(deleted, this.deleted, "deleted:\n")
            assertEquals(downloaded.size, this.downloaded.size, "download:\n$downloaded\n${this.downloaded}\n")
            assertEquals(downloaded, this.downloaded, "download:\n")
            assertEquals(items.size, this.items.size, "upload:\n${items.map { it.meta.id }}\n${this.items.map { it.meta.id }}\n")
            val sorted = this.items.sortedBy { it.meta.created }
            items.sortedBy { it.meta.created }.forEachIndexed { index, expected ->
                val actual = sorted[index]
                assertEquals(expected.meta.id, actual.meta.id)
                assertEquals(expected.meta.info, actual.meta.info, "id: ${expected.meta.id}")
                val message = """
                    expected: $expected
                    actual: $actual
                    expected.value(${expected.bytes.size}): ${expected.bytes.toHEX()} "${String(expected.bytes)}"
                    actual.payload  (${actual.bytes.size}): ${actual.bytes.toHEX()} "${String(actual.bytes)}"
                """.trimIndent()
                assertTrue(expected.bytes.contentEquals(actual.bytes), message)
                assertEquals(expected, actual)
            }
        }

        fun assert(
            expected: CommitInfo,
            actual: CommitInfo,
        ) {
            actual.assert(
                hash = expected.hash,
                items = expected.items,
                deleted = expected.deleted,
            )
        }

        private fun CommitInfo.assert(
            hash: ByteArray,
            items: List<RawPayload>,
            deleted: Set<UUID>,
        ) {
            assertEquals(hash.toHEX(), this.hash.toHEX())
            assertEquals(deleted.size, this.deleted.size, "deleted:\n$deleted\n${this.deleted}\n")
            assertEquals(deleted, this.deleted)
            assertEquals(items.size, this.items.size, "upload:\n${items.map { it.meta.id }}\n${this.items.map { it.meta.id }}\n")
            items.forEachIndexed { index, expected ->
                val actual = this.items[index]
                assertEquals(expected.meta.id, actual.meta.id)
                assertEquals(expected.meta.info, actual.meta.info, "id: ${expected.meta.id}")
                assertEquals(expected, actual)
            }
        }

        fun assert(
            expected: ItemInfo,
            actual: ItemInfo,
        ) {
            assertEquals(expected.updated, actual.updated)
            assertEquals(expected.hash.toHEX(), actual.hash.toHEX())
            assertEquals(expected.size, actual.size)
            assertEquals(expected, actual)
        }

        private fun ItemInfo.assert(
            storageId: UUID,
            itemId: UUID,
            updated: Duration,
            hash: ByteArray,
            size: Int,
        ) {
            assertEquals(updated, this.updated, "storageId: $storageId\nitemId: $itemId\nupdated:\n")
            assertEquals(hash.toHEX(), this.hash.toHEX(), "storageId: $storageId\nitemId: $itemId\nhash:\n")
            assertEquals(size, this.size, "storageId: $storageId\nitemId: $itemId\nsize:\n")
        }
    }

    private fun <T : Any> SyncStorage<T>.assert(
        id: UUID,
        hash: ByteArray,
        items: List<Payload<T>> = emptyList(),
    ) {
        assertEquals(id, this.id)
        assertTrue(hash.contentEquals(this.hash), "e: ${hash.toHEX()}, a: ${this.hash.toHEX()}")
        assertEquals(items.size, this.items.size)
        items.forEachIndexed { index, expected ->
            val actual = this.items[index]
            assertEquals(expected.meta.id, actual.meta.id)
            actual.meta.info.assert(
                storageId = id,
                itemId = expected.meta.id,
                updated = expected.meta.info.updated,
                hash = expected.meta.info.hash,
                size = expected.meta.info.size,
            )
            assertEquals(expected, actual)
        }
        assertEquals(items, this.items)
    }

    private fun SyncInfo.assert(
        meta: Map<UUID, ItemInfo> = emptyMap(),
        deleted: Set<UUID> = emptySet(),
    ) {
        assertEquals(meta.size, this.infos.size)
        meta.forEach { (itemId, expected) ->
            val actual = this.infos[itemId] ?: error("No item info!")
            assertEquals(expected, actual, "id: $itemId")
        }
        assertEquals(deleted.size, this.deleted.size)
        assertEquals(deleted, this.deleted)
    }

    @Test
    fun idTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val storageHash = MockHashFunction.map("storageHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
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
        val timeProvider = MockProvider { time }
        var itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = MockProvider { itemId }
        val defaultItems = (0..3).map { index ->
            val value = "payload:$index"
            Payload(
                meta = Metadata(
                    id = mockUUID(index),
                    created = (1_000 + index).milliseconds,
                    info = ItemInfo(
                        updated = (1_000 + index).milliseconds,
                        hash = MockHashFunction.map("item:hash:$index"),
                        size = StringTransformer.encode(value).size,
                    ),
                ),
                value = value,
            )
        }
        check(defaultItems.size == 4)
        val updatedItems = mutableListOf<Payload<String>>().also {
            val value = "payload:0:updated"
            it.add(
                defaultItems[0].copy(
                    updated = (2_000 + 0).milliseconds,
                    hash = MockHashFunction.map("item:hash:0:updated"),
                    value = value,
                    size = StringTransformer.encode(value).size,
                ),
            )
            it.add(defaultItems[2])
            it.add(defaultItems[3])
        }
        check(updatedItems.size == 3)
        check(defaultItems[0] != updatedItems[0])
        check(defaultItems[2] == updatedItems[1])
        check(defaultItems[3] == updatedItems[2])
        val storageHash = MockHashFunction.map("storageHash")
        val storageHashUpdated = MockHashFunction.map("storageHashUpdated")
        val hashes = defaultItems.map {
            StringTransformer.hashPair(it)
        } + updatedItems.map {
            StringTransformer.hashPair(it)
        } + listOf(
            MockHashFunction.hash(defaultItems) to storageHash,
            MockHashFunction.hash(updatedItems) to storageHashUpdated,
        )
        val transformer = defaultItems.map {
            it.value.toByteArray() to it.value
        } + updatedItems.map {
            it.value.toByteArray() to it.value
        }
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        defaultItems.forEachIndexed { index, payload ->
            itemId = payload.meta.id
            time = (1_000 + index).milliseconds
            storage.add(payload.value)
        }
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = defaultItems,
        )
        //
        time = (2_000 + 0).milliseconds
        assertEquals(updatedItems[0].meta.info, storage.update(defaultItems[0].meta.id, updatedItems[0].value))
        assertTrue(storage.delete(defaultItems[1].meta.id))
        storage.assert(
            id = storageId,
            hash = storageHashUpdated,
            items = updatedItems,
        )
    }

    @Test
    fun deletedTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val deleted = setOf(UUID.fromString("ed529649-c13b-4caf-b290-710da103bd25"))
        val storageHash = MockHashFunction.map("storageHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageHash,
            ),
            defaultDeleted = deleted,
        )
        storage.assert(
            id = id,
            hash = storageHash,
        )
    }

    private fun mockPayload(
        id: UUID,
        time: Duration,
        hash: ByteArray,
        value: String,
    ): Payload<String> {
        return Payload(
            meta = Metadata(
                id = id,
                created = time,
                info = ItemInfo(
                    updated = time,
                    hash = hash,
                    size = StringTransformer.encode(value).size,
                ),
            ),
            value = value,
        )
    }

    @Test
    fun deleteTest() {
        val storageId = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = MockHashFunction.map("itemHash")
        val time = 1.milliseconds
        val timeProvider = MockProvider { time }
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = MockProvider { itemId }
        val expected = mockPayload(
            id = itemId,
            time = time,
            hash = itemHash,
            value = "foobar",
        )
        val storageEmptyHash = MockHashFunction.map("storageEmptyHash")
        val storageHash = MockHashFunction.map("storageHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            hashes = listOf(
                ByteArray(0) to storageEmptyHash,
                StringTransformer.encode(expected.value) to itemHash,
                MockHashFunction.hash(listOf(expected)) to storageHash,
            ),
            uuidProvider = uuidProvider,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.value.toByteArray() to expected.value,
            ),
        )
        storage.assert(
            id = storageId,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.value))
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.meta.id)
        assertFalse(storage.delete(id = notExists))
        storage.assert(
            id = storageId,
            hash = storageHash,
            items = listOf(expected),
        )
        assertTrue(storage.delete(id = expected.meta.id))
        storage.assert(
            id = storageId,
            hash = storageEmptyHash,
        )
    }

    @Test
    fun addTest() {
        val id = mockUUID(1)
        val itemHash = MockHashFunction.map("itemHash")
        val time = 1.milliseconds
        val timeProvider = MockProvider { time }
        val itemId = mockUUID(2)
        val uuidProvider = MockProvider { itemId }
        val expected = mockPayload(
            id = itemId,
            time = time,
            hash = itemHash,
            value = "foobar",
        )
        val storageEmptyHash = MockHashFunction.map("storageEmptyHash")
        val storageHash = MockHashFunction.map("storageHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                ByteArray(0) to storageEmptyHash,
                StringTransformer.hashPair(expected),
                MockHashFunction.hash(listOf(expected)) to storageHash,
            ),
            uuidProvider = uuidProvider,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.value.toByteArray() to expected.value,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.value))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
    }

    @Test
    fun updateTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val itemHash = MockHashFunction.map("itemHash")
        val itemUpdated = "itemUpdated"
        val itemUpdatedHash = MockHashFunction.map("itemUpdatedHash")
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        val itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = MockProvider { itemId }
        val expected = mockPayload(
            id = itemId,
            time = time,
            hash = itemHash,
            value = "foobar",
        )
        val storageEmptyHash = MockHashFunction.map("storageEmptyHash")
        val storageHash = MockHashFunction.map("storageHash")
        val storageUpdatedHash = MockHashFunction.map("storageUpdatedHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                ByteArray(0) to storageEmptyHash,
                StringTransformer.encode(expected.value) to itemHash,
                StringTransformer.encode(itemUpdated) to itemUpdatedHash,
                MockHashFunction.hash(listOf(expected)) to storageHash,
                listOf(itemUpdatedHash).flatMap {
                    MockHashFunction.bytesOf(id = itemId, updated = 2.milliseconds, encoded = itemUpdatedHash).toList()
                }.toByteArray() to storageUpdatedHash,
            ),
            uuidProvider = uuidProvider,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.value.toByteArray() to expected.value,
                itemUpdated.toByteArray() to itemUpdated,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        assertEquals(expected, storage.add(expected.value))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = UUID.fromString("35ca49c2-d716-4eb3-ad1e-3f87337ce360")
        check(notExists != expected.meta.id)
        assertNull(storage.update(id = notExists, value = itemUpdated))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        time = 2.milliseconds
        val updated = Payload(
            meta = Metadata(
                id = expected.meta.id,
                created = expected.meta.created,
                info = ItemInfo(
                    updated = time,
                    hash = itemUpdatedHash,
                    size = StringTransformer.encode(itemUpdated).size,
                ),
            ),
            value = itemUpdated,
        )
        assertEquals(
            updated.meta.info,
            storage.update(id = expected.meta.id, value = itemUpdated),
        )
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
    }

    @Test
    fun getSyncInfoTest() {
        val id = mockUUID(1)
        val itemHash = MockHashFunction.map("itemHash")
        val itemUpdated = "itemUpdated"
        val itemUpdatedHash = MockHashFunction.map("itemUpdatedHash")
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        val itemId = mockUUID(11)
        val uuidProvider = MockProvider { itemId }
        val expected = mockPayload(
            id = itemId,
            time = time,
            hash = itemHash,
            value = "foobar",
        )
        val storageEmptyHash = MockHashFunction.map("storageEmptyHash")
        val storageHash = MockHashFunction.map("storageHash")
        val storageUpdatedHash = MockHashFunction.map("storageUpdatedHash")
        val storage: SyncStorage<String> = mockSyncStreamsStorage(
            id = id,
            hashes = listOf(
                ByteArray(0) to storageEmptyHash,
                StringTransformer.encode(expected.value) to itemHash,
                StringTransformer.encode(itemUpdated) to itemUpdatedHash,
                MockHashFunction.hash(listOf(expected)) to storageHash,
                listOf(itemUpdatedHash).flatMap {
                    MockHashFunction.bytesOf(id = itemId, updated = 2.milliseconds, encoded = itemUpdatedHash).toList()
                }.toByteArray() to storageUpdatedHash,
            ),
            uuidProvider = uuidProvider,
            timeProvider = timeProvider,
            transformer = listOf(
                expected.value.toByteArray() to expected.value,
                itemUpdated.toByteArray() to itemUpdated,
            ),
        )
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        storage.getSyncInfo().assert()
        assertEquals(expected, storage.add(expected.value))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        val notExists = mockUUID(12)
        check(notExists != expected.meta.id)
        assertNull(storage.update(id = notExists, value = itemUpdated))
        storage.assert(
            id = id,
            hash = storageHash,
            items = listOf(expected),
        )
        time = 2.milliseconds
        val updated = Payload(
            meta = Metadata(
                id = expected.meta.id,
                created = expected.meta.created,
                info = ItemInfo(
                    updated = time,
                    hash = itemUpdatedHash,
                    size = StringTransformer.encode(itemUpdated).size,
                ),
            ),
            value = itemUpdated,
        )
        assertEquals(
            updated.meta.info,
            storage.update(id = expected.meta.id, value = itemUpdated),
        )
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
        storage.getSyncInfo().assert(meta = mapOf(updated.meta.id to updated.meta.info))
        assertFalse(storage.delete(id = notExists))
        storage.assert(
            id = id,
            hash = storageUpdatedHash,
            items = listOf(updated),
        )
        assertTrue(storage.delete(id = expected.meta.id))
        storage.assert(
            id = id,
            hash = storageEmptyHash,
        )
        storage.getSyncInfo().assert(deleted = emptySet())
    }

    @Test
    fun mergeAndCommitTest() {
        val storageId = mockUUID(1)
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = UUID.fromString("10a325bd-3b99-4ff8-8865-086af338e935")
        val uuidProvider = MockProvider { itemId }
        val defaultItems = (0..4).map { index ->
            mockPayload(pointer = index)
        }
        check(defaultItems.size == 5)
        val rItems = mutableListOf<Payload<String>>().also {
            it.add(
                defaultItems[0].copy(
                    updated = (2_000 + 0).milliseconds,
                    hash = MockHashFunction.map("item:hash:0:updated"),
                    value = "item:0:updated",
                    size = StringTransformer.encode("item:0:updated").size,
                ),
            )
            it.add(defaultItems[2])
            it.add(defaultItems[3])
            it.add(defaultItems[4])
            it.add(mockPayload(pointer = 11))
        }
        check(rItems.size == 5)
        val tItems = mutableListOf<Payload<String>>().also {
            it.add(defaultItems[0])
            it.add(defaultItems[1])
            it.add(
                defaultItems[3].copy(
                    updated = (3_000 + 0).milliseconds,
                    hash = MockHashFunction.map("item:hash:3:updated"),
                    value = "item:3:updated",
                    size = StringTransformer.encode("item:3:updated").size,
                ),
            )
            it.add(defaultItems[4])
            it.add(mockPayload(pointer = 21))
        }
        check(tItems.size == 5)
        val itemsMerged = listOf(
            rItems[0],
            tItems[2],
            defaultItems.last(),
            rItems.last(),
            tItems.last(),
        )
        check(itemsMerged.size == 5)
        val storageHash = MockHashFunction.map("storageHash")
        val storageRDefaultHash = MockHashFunction.map("storageRDHash")
        val storageTDefaultHash = MockHashFunction.map("storageTDHash")
        val storageRHash = MockHashFunction.map("storageRHash")
        val storageTHash = MockHashFunction.map("storageTHash")
        val storageHashMerged = MockHashFunction.map("storageHashMerged")
        val hashes = defaultItems.map {
            StringTransformer.hashPair(it)
        } + rItems.map {
            StringTransformer.hashPair(it)
        } + tItems.map {
            StringTransformer.hashPair(it)
        } + listOf(
            MockHashFunction.hash(defaultItems + rItems.last()) to storageRDefaultHash,
            MockHashFunction.hash(defaultItems + tItems.last()) to storageTDefaultHash,
            MockHashFunction.hash(defaultItems) to storageHash,
            MockHashFunction.hash(rItems) to storageRHash,
            MockHashFunction.hash(tItems) to storageTHash,
            MockHashFunction.hash(itemsMerged) to storageHashMerged,
        )
        val transformer = defaultItems.map {
            it.value.toByteArray() to it.value
        } + rItems.map {
            it.value.toByteArray() to it.value
        } + tItems.map {
            it.value.toByteArray() to it.value
        }
        val rStorage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        val tStorage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        defaultItems.forEachIndexed { index, payload ->
            itemId = payload.meta.id
            time = (1_000 + index).milliseconds
            rStorage.add(payload.value)
        }
        rStorage.commit(tStorage.merge(rStorage.getMergeInfo(tStorage.getSyncInfo())))
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
        mockPayload(pointer = 11).also { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            rStorage.add(payload.value)
        }
        mockPayload(pointer = 21).also { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            tStorage.add(payload.value)
        }
        rStorage.assert(
            id = storageId,
            hash = storageRDefaultHash,
            items = defaultItems + rItems.last(),
        )
        tStorage.assert(
            id = storageId,
            hash = storageTDefaultHash,
            items = defaultItems + tItems.last(),
        )
        //
        time = (2_000 + 0).milliseconds
        assertEquals(rItems[0].meta.info, rStorage.update(defaultItems[0].meta.id, rItems[0].value))
        assertTrue(rStorage.delete(defaultItems[1].meta.id))
        rStorage.assert(
            id = storageId,
            hash = storageRHash,
            items = rItems,
        )
        //
        time = (3_000 + 0).milliseconds
        assertEquals(tItems[2].meta.info, tStorage.update(defaultItems[3].meta.id, tItems[2].value))
        assertTrue(tStorage.delete(defaultItems[2].meta.id))
        tStorage.assert(
            id = storageId,
            hash = storageTHash,
            items = tItems,
        )
        //
        val rSyncInfo = rStorage.getSyncInfo()
        rSyncInfo.assert(meta = rItems.associate { it.meta.id to it.meta.info }, deleted = setOf(defaultItems[1].meta.id))
        val tMergeInfo = tStorage.getMergeInfo(rSyncInfo)
        tMergeInfo.assert(
            downloaded = setOf(defaultItems[0].meta.id, rItems.last().meta.id),
            items = listOf(
                tItems.last().map(StringTransformer),
                tItems[2].map(StringTransformer),
            ),
            deleted = setOf(defaultItems[2].meta.id),
        )
        //
        val tSyncInfo = tStorage.getSyncInfo()
        tSyncInfo.assert(meta = tItems.associate { it.meta.id to it.meta.info }, deleted = setOf(defaultItems[2].meta.id))
        val rMergeInfo = rStorage.getMergeInfo(tSyncInfo)
        rMergeInfo.assert(
            downloaded = setOf(defaultItems[3].meta.id, tItems.last().meta.id),
            items = listOf(
                rItems.last().map { it.toByteArray() },
                rItems[0].map { it.toByteArray() },
            ),
            deleted = setOf(defaultItems[1].meta.id),
        )
        //
        val commitInfo = rStorage.merge(tMergeInfo)
        commitInfo.assert(
            hash = storageHashMerged,
            items = listOf(
                rItems[0].map { it.toByteArray() },
                rItems.last().map { it.toByteArray() },
            ),
            deleted = setOf(defaultItems[1].meta.id),
        )
        tStorage.commit(commitInfo)
        rStorage.assert(
            id = storageId,
            hash = storageHashMerged,
            items = itemsMerged,
        )
        tStorage.assert(
            id = storageId,
            hash = storageHashMerged,
            items = itemsMerged,
        )
    }

    @Test
    fun commitTest() {
        val rItems = listOf(
            mockPayload(pointer = 1),
            mockPayload(pointer = 11),
        )
        check(rItems.size == 2)
        val tItems = listOf(
            mockPayload(pointer = 1),
            mockPayload(pointer = 21),
        )
        check(tItems.size == 2)
        val itemsMerged = listOf(
            mockPayload(pointer = 1),
            mockPayload(pointer = 11),
            mockPayload(pointer = 21),
        )
        check(itemsMerged.size == 3)
        //
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val hashes = MockHashFunction.hashes(
            rItems to "r:default",
            tItems to "t:default",
            itemsMerged to "merged:hash",
            listOf(
                mockPayload(pointer = 1),
                mockPayload(pointer = 21),
                mockPayload(pointer = 31),
            ) to "t:wrong",
        ) + rItems.map {
            StringTransformer.hashPair(it)
        } + tItems.map {
            StringTransformer.hashPair(it)
        }
        val transformer = rItems.map {
            it.value.toByteArray() to it.value
        } + tItems.map {
            it.value.toByteArray() to it.value
        } + mockPayload(pointer = 31).let {
            it.value.toByteArray() to it.value
        }
        val storageId = mockUUID(pointer = 42)
        val rStorage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        val tStorage: SyncStorage<String> = mockSyncStreamsStorage(
            id = storageId,
            timeProvider = timeProvider,
            uuidProvider = uuidProvider,
            hashes = hashes,
            transformer = transformer,
        )
        rItems.forEach { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            rStorage.add(payload.value)
        }
        rStorage.assert(
            id = storageId,
            hash = MockHashFunction.map("r:default"),
            items = rItems,
        )
        tItems.forEach { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            tStorage.add(payload.value)
        }
        tStorage.assert(
            id = storageId,
            hash = MockHashFunction.map("t:default"),
            items = tItems,
        )
        val rSyncInfo = rStorage.getSyncInfo()
        val tMergeInfo = tStorage.getMergeInfo(rSyncInfo)
        val error = assertThrowsExactly(IllegalStateException::class.java) {
            val info = CommitInfo(
                hash = MockHashFunction.map("wrong:hash"),
                items = listOf(
                    mockPayload(pointer = 31).map { it.toByteArray() },
                ),
                deleted = emptySet(),
            )
            tStorage.commit(info)
        }
        assertEquals("Wrong hash!", error.message)
        val rCommitInfo = rStorage.merge(tMergeInfo)
        tStorage.commit(rCommitInfo)
        assertEquals(rStorage.hash, tStorage.hash)
        rStorage.assert(
            id = storageId,
            hash = MockHashFunction.map("merged:hash"),
            items = itemsMerged,
        )
        tStorage.assert(
            id = storageId,
            hash = MockHashFunction.map("merged:hash"),
            items = itemsMerged,
        )
    }
}
