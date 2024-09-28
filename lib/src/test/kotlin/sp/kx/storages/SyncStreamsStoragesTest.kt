package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class SyncStreamsStoragesTest {
    @Test
    fun addTest() {
        val id1 = mockUUID(1)
        val builder = SyncStreamsStorages.Builder()
        builder.add(id1, String::class.java, StringTransformer)
        assertThrows(IllegalStateException::class.java) {
            builder.add(id1, String::class.java, StringTransformer)
        }
    }

    @Test
    fun noStoragesTest() {
        assertThrows(IllegalStateException::class.java) {
            SyncStreamsStorages.Builder().mock()
        }
    }

    @Test
    fun getTest() {
        val streamer = MockStreamer()
        val id1 = mockUUID(1)
        val storages = SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .mock(
                getStreamerProvider = { ids ->
                    assertEquals(ids, setOf(id1))
                    mockStreamerProvider(id1, streamer)
                },
            )
        val storage = storages.get(id = id1)
        assertNotNull(storage)
        checkNotNull(storage)
        val id2 = mockUUID(2)
        check(id1 != id2)
        assertNull(storages.get(id = id2))
    }

    @Test
    fun requireTest() {
        val streamer = MockStreamer()
        val id1 = mockUUID(1)
        SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .mock(getStreamerProvider = getStreamerProvider(streamers = mapOf(id1 to streamer)))
            .require(id = id1)
    }

    @Test
    fun requireErrorTest() {
        val streamer = MockStreamer()
        val id1 = mockUUID(1)
        val storages = SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .mock(getStreamerProvider = getStreamerProvider(streamers = mapOf(id1 to streamer)))
        val id2 = mockUUID(2)
        check(id1 != id2)
        assertThrows(IllegalStateException::class.java) {
            storages.require(id = id2)
        }
    }

    @Test
    fun getTypeTest() {
        val id1 = mockUUID(1)
        val id2 = mockUUID(2)
        check(id1 != id2)
        val streamers = mapOf(
            id1 to MockStreamer(),
            id2 to MockStreamer(),
        )
        val storages = SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .add(id2, Int::class.java, IntTransformer)
            .mock(getStreamerProvider = getStreamerProvider(streamers = streamers))
        storages.get(type = String::class.java).also { storage ->
            assertNotNull(storage)
            checkNotNull(storage)
            assertEquals(id1, storage.id)
        }
        storages.get(type = Int::class.java).also { storage ->
            assertNotNull(storage)
            checkNotNull(storage)
            assertEquals(id2, storage.id, "Storage: $storage")
        }
        storages.get(type = Boolean::class.java).also { storage ->
            assertNull(storage)
        }
    }

    @Test
    fun hashesTest() {
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, _ ->
            tStorages.assertHashes(
                expected = mapOf(
                    mockUUID(1) to MockHashFunction.map("strings:hash:t:updated"),
                    mockUUID(2) to MockHashFunction.map("ints:hash:t:updated"),
                    mockUUID(3) to MockHashFunction.map("longs:hash"),
                    mockUUID(4) to MockHashFunction.map("foos:hash"),
                ),
            )
            rStorages.assertHashes(
                expected = mapOf(
                    mockUUID(1) to MockHashFunction.map("strings:hash:r:updated"),
                    mockUUID(2) to MockHashFunction.map("ints:hash:r:updated"),
                    mockUUID(3) to MockHashFunction.map("longs:hash"),
                    mockUUID(5) to MockHashFunction.map("bars:hash"),
                ),
            )
        }
    }

    @Test
    fun hashTest() {
        var time = 1.minutes
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val payloadValue = "foo:1"
        val itemHash = MockHashFunction.map("$payloadValue:hash")
        val item1ListHash = MockHashFunction.map("$payloadValue:${mockUUID(11)}:hash:list")
        val item2ListHash = MockHashFunction.map("$payloadValue:${mockUUID(12)}:hash:list")
        val itemFinalListHash = MockHashFunction.map("$payloadValue:final:hash:list")
        val hashes = MockHashFunction.hashes(
            emptyList<Payload<String>>() to "strings:empty",
        ) + listOf(
            StringTransformer.encode(payloadValue) to itemHash,
            listOf(
                MockHashFunction.bytesOf(id = mockUUID(11), updated = 11.minutes, encoded = itemHash),
            ).flatMap { it.toList() }.toByteArray() to item1ListHash,
            listOf(
                MockHashFunction.bytesOf(id = mockUUID(12), updated = 12.minutes, encoded = itemHash),
            ).flatMap { it.toList() }.toByteArray() to item2ListHash,
            listOf(
                MockHashFunction.bytesOf(id = mockUUID(11), updated = 11.minutes, encoded = itemHash),
                MockHashFunction.bytesOf(id = mockUUID(12), updated = 12.minutes, encoded = itemHash),
            ).flatMap { it.toList() }.toByteArray() to itemFinalListHash,
        )
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val tStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "t"),
                        ids = ids,
                    )
                },
            )
        val rStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "r"),
                        ids = ids,
                    )
                },
            )
        time = 11.minutes
        itemId = mockUUID(11)
        tStorages.require<String>().add(payloadValue)
        time = 12.minutes
        itemId = mockUUID(12)
        rStorages.require<String>().add(payloadValue)
        tStorages.require<String>().items.single().also { tItem ->
            val rItem = rStorages.require<String>().items.single()
            check(tItem.meta.id != rItem.meta.id)
            check(tItem.meta.created != rItem.meta.created)
            check(tItem.meta.info.updated != rItem.meta.info.updated)
            check(tItem.meta.info.hash.contentEquals(rItem.meta.info.hash))
            check(tItem.value == payloadValue)
            check(tItem.value == rItem.value)
            val tHash = tStorages.require<String>().hash
            val rHash = rStorages.require<String>().hash
            check(!tHash.contentEquals(rHash))
        }
        val response = rStorages.hashes().let {
            assertEquals(it.keys.single(), mockUUID(1))
            assertTrue(it.values.single().contentEquals(item2ListHash))
            tStorages.getSyncInfo(it)
        }
        assertEquals(response.infos.keys.single(), mockUUID(1))
        val syncInfo = response.infos.values.single()
        assertEquals(syncInfo.infos.keys.single(), mockUUID(11))
        assertEquals(syncInfo.infos.values.single(), mockItemInfo(updated = 11.minutes, hash = itemHash))
        assertTrue(syncInfo.deleted.isEmpty())
        val mis = rStorages.getMergeInfo(session = response.session, infos = response.infos)
        assertEquals(mis.keys.single(), mockUUID(1))
        val mergeInfo = mis.values.single()
        assertTrue(mergeInfo.deleted.isEmpty())
        assertEquals(mergeInfo.downloaded.single(), mockUUID(11))
        mergeInfo.items.single().assert(
            other = mockRawPayload(
                meta = mockMetadata(
                    id = mockUUID(12),
                    created = 12.minutes,
                    info = mockItemInfo(updated = 12.minutes, hash = itemHash),
                ),
                bytes = StringTransformer.encode(payloadValue),
            ),
        )
        val cis = tStorages.merge(session = response.session, infos = mis)
        assertEquals(cis.keys.single(), mockUUID(1))
        val commitInfo = cis.values.single()
        assertTrue(commitInfo.deleted.isEmpty())
        commitInfo.items.single().assert(
            other = mockRawPayload(
                meta = mockMetadata(
                    id = mockUUID(11),
                    created = 11.minutes,
                    info = mockItemInfo(updated = 11.minutes, hash = itemHash),
                ),
                bytes = StringTransformer.encode(payloadValue),
            ),
        )
        assertTrue(commitInfo.hash.contentEquals(itemFinalListHash), "e: ${itemFinalListHash.toHEX()}\na: ${commitInfo.hash.toHEX()}")
        val ids = rStorages.commit(session = response.session, infos = cis)
        assertEquals(ids.single(), mockUUID(1))
        assertEquals(tStorages.hashes().keys.single(), mockUUID(1))
        assertTrue(tStorages.hashes().values.single().contentEquals(itemFinalListHash))
        assertEquals(rStorages.hashes().keys.single(), mockUUID(1))
        assertTrue(rStorages.hashes().values.single().contentEquals(itemFinalListHash))
        val tItems = tStorages.require<String>().items
        val rItems = rStorages.require<String>().items
        assertEquals(tItems.size, 2)
        assertEquals(tItems.size, rItems.size)
        assertEquals(tItems, rItems)
    }

    @Test
    fun deleteTest() {
        val strings = (1..5).map { number ->
            mockPayload(pointer = 10 + number)
        }
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val hashes = MockHashFunction.hashes(
            strings to "strings:1-5:hash",
            (2..5).map { number ->
                mockPayload(pointer = 10 + number)
            } to "strings:2-5:hash",
            emptyList<Payload<String>>() to "strings:empty",
        ) + strings.map {
            StringTransformer.hashPair(it)
        }
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val tStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "t"),
                        ids = ids,
                    )
                },
            )
        val rStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "r"),
                        ids = ids,
                    )
                },
            )
        strings.forEach { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            tStorages.require<String>().add(payload.value)
        }
        val response = tStorages.getSyncInfo(hashes = rStorages.hashes())
        val mis = rStorages.getMergeInfo(session = response.session, infos = response.infos)
        val cis = tStorages.merge(session = response.session, infos = mis)
        rStorages.commit(session = response.session, infos = cis)
        check(tStorages.hashes().keys.sorted() == rStorages.hashes().keys.sorted())
        tStorages.hashes().keys.forEach { storageId ->
            val tStorage = tStorages.require(storageId)
            val tItems = tStorage.items
            val rStorage = rStorages.require(storageId)
            val rItems = rStorage.items
            check(tItems.isNotEmpty())
            check(tItems.size == rItems.size)
            tItems.forEachIndexed { index, payload ->
                check(payload == rItems[index])
            }
        }
        val deleted = rStorages.require(mockUUID(1)).delete(mockUUID(11))
        assertTrue(deleted)
        assertTrue(rStorages.require(mockUUID(1)).items.none { it.meta.id == mockUUID(11) })
        assertTrue(tStorages.require(mockUUID(1)).items.any { it.meta.id == mockUUID(11) })
        rStorages.getSyncInfo(tStorages.hashes()).also { response ->
            assertEquals(response.infos.keys, setOf(mockUUID(1)))
            val si = response.infos[mockUUID(1)]
            assertNotNull(si)
            checkNotNull(si)
            assertEquals(si.deleted.sorted(), listOf(mockUUID(11)))
        }
        tStorages.getSyncInfo(rStorages.hashes()).also { response ->
            assertEquals(response.infos.keys, setOf(mockUUID(1)))
            val si = response.infos[mockUUID(1)]
            assertNotNull(si)
            checkNotNull(si)
            assertEquals(si.deleted.sorted(), emptyList<UUID>())
        }
    }

    @Test
    fun getSyncInfoTest() {
        val stringsTUpdated = listOf(
            mockPayload(12),
            mockPayload(13).updated(113),
            mockPayload(14),
            mockPayload(15),
            mockPayload(16),
        )
        val stringsRUpdated = listOf(
            mockPayload(11),
            mockPayload(13),
            mockPayload(14).updated(114),
            mockPayload(15),
            mockPayload(17),
        )
        val intsTUpdated = listOf(
            mockPayload(pointer = 22, value = 22),
            mockPayload(pointer = 23, value = 23).updated(123, 123),
            mockPayload(pointer = 24, value = 24),
            mockPayload(pointer = 25, value = 25),
            mockPayload(pointer = 26, value = 26),
        )
        val intsRUpdated = listOf(
            mockPayload(pointer = 21, value = 21),
            mockPayload(pointer = 23, value = 23),
            mockPayload(pointer = 24, value = 24).updated(124, 124),
            mockPayload(pointer = 25, value = 25),
            mockPayload(pointer = 27, value = 27),
        )
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, _ ->
            tStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            tStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(rStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) } + mockUUID(5))
            tStorages.assertSyncInfo(
                hashes = rStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsTUpdated.associate { it.meta.id to it.meta.info },
                        deleted = setOf(mockUUID(11)),
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsTUpdated.associate { it.meta.id to it.meta.info },
                        deleted = setOf(mockUUID(21)),
                    ),
                ),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(tStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) } + mockUUID(4))
            rStorages.assertSyncInfo(
                hashes = tStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsRUpdated.associate { it.meta.id to it.meta.info },
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsRUpdated.associate { it.meta.id to it.meta.info },
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
            )
        }
    }

    @Test
    fun getSyncInfoNotCommitedTest() {
        val stringsTUpdated = listOf(
            mockPayload(12),
            mockPayload(13).updated(113),
            mockPayload(14),
            mockPayload(15),
            mockPayload(16),
        )
        val stringsRUpdated = listOf(
            mockPayload(11),
            mockPayload(13),
            mockPayload(14).updated(114),
            mockPayload(15),
            mockPayload(17),
        )
        val intsTUpdated = listOf(
            mockPayload(pointer = 22, value = 22),
            mockPayload(pointer = 23, value = 23).updated(123, 123),
            mockPayload(pointer = 24, value = 24),
            mockPayload(pointer = 25, value = 25),
            mockPayload(pointer = 26, value = 26),
        )
        val intsRUpdated = listOf(
            mockPayload(pointer = 21, value = 21),
            mockPayload(pointer = 23, value = 23),
            mockPayload(pointer = 24, value = 24).updated(124, 124),
            mockPayload(pointer = 25, value = 25),
            mockPayload(pointer = 27, value = 27),
        )
        onSyncStreamsStorages(commited = false) { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, _ ->
            tStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            tStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(rStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) } + mockUUID(5))
            tStorages.assertSyncInfo(
                hashes = rStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsTUpdated.associate { it.meta.id to it.meta.info },
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsTUpdated.associate { it.meta.id to it.meta.info },
                    ),
                ),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(tStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) } + mockUUID(4))
            rStorages.assertSyncInfo(
                hashes = tStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsRUpdated.associate { it.meta.id to it.meta.info },
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsRUpdated.associate { it.meta.id to it.meta.info },
                    ),
                ),
            )
        }
    }

    @Test
    fun getMergeInfoErrorTest() {
        val hashes = MockHashFunction.hashes(
            emptyList<Payload<String>>() to "items:empty",
        )
        val session = mockSyncSession(
            src = MockHashFunction.hashOf(id = mockUUID(1), decoded = "items:empty"),
        )
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val storages = SyncStreamsStorages.Builder(session)
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "foo"),
                        ids = ids,
                    )
                },
            )
        val id = mockUUID(2)
        val infos = mapOf(id to mockSyncInfo())
        val throwable = assertThrows(IllegalStateException::class.java) {
            storages.getMergeInfo(session = session, infos = infos)
        }
        assertEquals("No storage by ID: \"$id\"!", throwable.message)
    }

    @Test
    fun getMergeInfoTest() {
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, _ ->
            tStorages.assertMergeInfo(
                storage = rStorages,
                expected = mapOf(
                    mockUUID(1) to mockMergeInfo(
                        downloaded = setOf(mockUUID(14), mockUUID(pointer = 17)),
                        items = listOf(
                            mockPayload(13).updated(113).map(StringTransformer::encode),
                            mockPayload(pointer = 16).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(11)),
                    ),
                    mockUUID(2) to mockMergeInfo(
                        downloaded = setOf(mockUUID(pointer = 24), mockUUID(pointer = 27)),
                        items = listOf(
                            mockPayload(pointer = 23, value = 23).updated(123, 123).map(IntTransformer::encode),
                            mockPayload(pointer = 26, value = 26).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(21)),
                    ),
                ),
            )
            rStorages.assertMergeInfo(
                storage = tStorages,
                expected = mapOf(
                    mockUUID(1) to mockMergeInfo(
                        downloaded = setOf(mockUUID(13), mockUUID(pointer = 16)),
                        items = listOf(
                            mockPayload(14).updated(114).map(StringTransformer::encode),
                            mockPayload(pointer = 17).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockMergeInfo(
                        downloaded = setOf(mockUUID(23), mockUUID(26)),
                        items = listOf(
                            mockPayload(pointer = 24, value = 24).updated(124, 124).map(IntTransformer::encode),
                            mockPayload(pointer = 27, value = 27).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
            )
        }
    }

    private fun SyncStreamsStorages.assertMerge(
        storages: SyncStreamsStorages,
        files: Map<File, List<String>>,
        infos: Map<UUID, CommitInfo>,
        items: Map<UUID, List<Payload<out Any>>>,
    ) {
        val response = storages.getSyncInfo(hashes())
        val mis = getMergeInfo(session = response.session, infos = response.infos)
        val cis = storages.merge(session = response.session, infos = mis)
        assertFiles(files = files)
        check(infos.keys.isNotEmpty())
        assertEquals(cis.keys.sorted(), infos.keys.sorted())
        infos.forEach { (id, info) ->
            val actual = cis[id]
            assertNotNull(actual, "id: $id")
            checkNotNull(actual)
            SyncStreamsStorageTest.assert(
                expected = info,
                actual = actual,
            )
        }
        check(items.isNotEmpty())
        items.forEach { (id, list) ->
            check(list.isNotEmpty())
            storages.require(id).items.forEachIndexed { index, actual ->
                val expected = list[index]
                SyncStreamsStorageTest.assert(expected = expected, actual = actual)
            }
        }
    }

    @Test
    fun mergeTest() {
        val stringsFinal = listOf(
            mockPayload(13).updated(pointer = 113),
            mockPayload(14).updated(pointer = 114),
            mockPayload(15),
            mockPayload(16),
            mockPayload(17),
        )
        val intsFinal = listOf(
            mockPayload(23, 23).updated(pointer = 123, 123),
            mockPayload(24, 24).updated(pointer = 124, 124),
            mockPayload(25, 25),
            mockPayload(26, 26),
            mockPayload(27, 27),
        )
        val longs = (1..5).map { number ->
            mockPayload(pointer = 30 + number, value = number.toLong())
        }
        val foos = (1..5).map { number ->
            mockPayload(pointer = 40 + number, value = Foo(text = "foo:${40 + number}"))
        }
        val bars = (1..5).map { number ->
            mockPayload(pointer = 50 + number, value = Bar(number = 50 + number))
        }
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, dir: File ->
            tStorages.assertMerge(
                storages = rStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-1",
                        "${mockUUID(2)}-1",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                infos = mapOf(
                    mockUUID(1) to mockCommitInfo(
                        hash = MockHashFunction.map("strings:hash:final"),
                        items = listOf(
                            mockPayload(14).updated(pointer = 114).map(StringTransformer::encode),
                            mockPayload(17).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockCommitInfo(
                        hash = MockHashFunction.map("ints:hash:final"),
                        items = listOf(
                            mockPayload(24, 24).updated(pointer = 124, 124).map(IntTransformer::encode),
                            mockPayload(27, 27).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(5) to bars,
                ),
            )
        }
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, dir: File ->
            rStorages.assertMerge(
                storages = tStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-1",
                        "${mockUUID(2)}-1",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                infos = mapOf(
                    mockUUID(1) to mockCommitInfo(
                        hash = MockHashFunction.map("strings:hash:final"),
                        items = listOf(
                            mockPayload(13).updated(pointer = 113).map(StringTransformer::encode),
                            mockPayload(16).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(11)),
                    ),
                    mockUUID(2) to mockCommitInfo(
                        hash = MockHashFunction.map("ints:hash:final"),
                        items = listOf(
                            mockPayload(23, 23).updated(pointer = 123, 123).map(IntTransformer::encode),
                            mockPayload(26, 26).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(21)),
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(4) to foos,
                ),
            )
            tStorages.assertMerge(
                storages = rStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                infos = mapOf(
                    mockUUID(1) to mockCommitInfo(
                        hash = MockHashFunction.map("strings:hash:final"),
                        items = emptyList(),
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockCommitInfo(
                        hash = MockHashFunction.map("ints:hash:final"),
                        items = emptyList(),
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(5) to bars,
                ),
            )
        }
    }

    @Test
    fun mergeErrorTest() {
        val hashes = MockHashFunction.hashes(
            emptyList<Payload<String>>() to "items:empty",
        )
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val session = mockSyncSession(
            dst = MockHashFunction.hashOf(id = mockUUID(1), decoded = "items:empty"),
        )
        val storages = SyncStreamsStorages.Builder(session)
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "foo"),
                        ids = ids,
                    )
                },
            )
        val id = mockUUID(2)
        val infos = mapOf(id to mockMergeInfo())
        val throwable = assertThrows(IllegalStateException::class.java) {
            storages.merge(session = session, infos = infos)
        }
        assertEquals("No storage by ID: \"$id\"!", throwable.message)
    }

    private fun assertCommit(
        dstStorages: SyncStreamsStorages,
        srcStorages: SyncStreamsStorages,
        files: Map<File, List<String>>,
        items: Map<UUID, List<Payload<out Any>>>,
    ) {
        val response = srcStorages.getSyncInfo(dstStorages.hashes())
        val mis = dstStorages.getMergeInfo(session = response.session, infos = response.infos)
        val cis = srcStorages.merge(session = response.session, infos = mis)
        dstStorages.commit(session = response.session, infos = cis)
        assertFiles(files = files)
        check(items.isNotEmpty())
        items.forEach { (id, list) ->
            check(list.isNotEmpty())
            dstStorages.require(id).items.forEachIndexed { index, actual ->
                val expected = list[index]
                SyncStreamsStorageTest.assert(expected = expected, actual = actual)
            }
        }
    }

    fun assertCommitFiles(
        dstStorages: SyncStreamsStorages,
        srcStorages: SyncStreamsStorages,
        beforeMerge: Map<File, List<String>>,
        afterMerge: Map<File, List<String>>,
        afterCommit: Map<File, List<String>>,
    ) {
        assertFiles(files = beforeMerge)
        val response = dstStorages.getSyncInfo(srcStorages.hashes())
        val mis = srcStorages.getMergeInfo(session = response.session, infos = response.infos)
        val cis = dstStorages.merge(session = response.session, infos = mis)
        assertFiles(files = afterMerge)
        srcStorages.commit(session = response.session, infos = cis)
        assertFiles(files = afterCommit)
    }

    @Test
    fun commitFilesTest() {
        val strings = (1..5).map { number ->
            mockPayload(pointer = 10 + number)
        }
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val hashes = MockHashFunction.hashes(
            strings to "strings:1-5:hash",
            (2..5).map { number ->
                mockPayload(pointer = 10 + number)
            } to "strings:2-5:hash",
            (1..4).map { number ->
                mockPayload(pointer = 10 + number)
            } to "strings:1-4:hash",
            (2..4).map { number ->
                mockPayload(pointer = 10 + number)
            } to "strings:2-4:hash",
            (3..4).map { number ->
                mockPayload(pointer = 10 + number)
            } to "strings:3-4:hash",
            listOf(mockPayload(pointer = 14)) to "strings:4:hash",
            emptyList<Payload<String>>() to "strings:empty",
        ) + strings.map {
            StringTransformer.hashPair(it)
        }
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val tStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "t"),
                        ids = ids,
                    )
                },
            )
        val rStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "r"),
                        ids = ids,
                    )
                },
            )
        strings.forEach { payload ->
            itemId = payload.meta.id
            time = payload.meta.created
            tStorages.require<String>().add(payload.value)
        }
        assertFiles(
            files = mapOf(
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-0",
                ),
            ),
        )
        assertCommitFiles(
            dstStorages = tStorages,
            srcStorages = rStorages,
            beforeMerge = mapOf(
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-0",
                ),
            ),
            afterMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-0",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
            ),
            afterCommit = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
            ),
        )
        check(tStorages.hashes().keys.sorted() == rStorages.hashes().keys.sorted())
        tStorages.hashes().keys.forEach { storageId ->
            val tStorage = tStorages.require(storageId)
            val tItems = tStorage.items
            val rStorage = rStorages.require(storageId)
            val rItems = rStorage.items
            check(tItems.isNotEmpty())
            check(tItems.size == rItems.size)
            tItems.forEachIndexed { index, payload: Payload<out Any> ->
                check(payload == rItems[index])
            }
        }
        assertTrue(rStorages.require(mockUUID(1)).delete(mockUUID(11)))
        assertTrue(rStorages.require(mockUUID(1)).items.none { it.meta.id == mockUUID(11) })
        assertTrue(tStorages.require(mockUUID(1)).items.any { it.meta.id == mockUUID(11) })
        assertTrue(tStorages.require(mockUUID(1)).delete(mockUUID(15)))
        assertTrue(rStorages.require(mockUUID(1)).items.any { it.meta.id == mockUUID(15) })
        assertTrue(tStorages.require(mockUUID(1)).items.none { it.meta.id == mockUUID(15) })
        assertCommitFiles(
            dstStorages = tStorages,
            srcStorages = rStorages,
            beforeMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
            ),
            afterMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-1",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
            ),
            afterCommit = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
            ),
        )
        assertTrue(rStorages.require(mockUUID(1)).delete(mockUUID(12)))
        assertTrue(rStorages.require(mockUUID(1)).items.none { it.meta.id == mockUUID(12) })
        assertTrue(tStorages.require(mockUUID(1)).items.any { it.meta.id == mockUUID(12) })
        assertCommitFiles(
            dstStorages = tStorages,
            srcStorages = rStorages,
            beforeMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
            ),
            afterMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-3",
                ),
            ),
            afterCommit = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-3",
                ),
            ),
        )
        assertTrue(tStorages.require(mockUUID(1)).delete(mockUUID(13)))
        assertTrue(rStorages.require(mockUUID(1)).items.any { it.meta.id == mockUUID(13) })
        assertTrue(tStorages.require(mockUUID(1)).items.none { it.meta.id == mockUUID(13) })
        assertCommitFiles(
            dstStorages = tStorages,
            srcStorages = rStorages,
            beforeMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-3",
                ),
            ),
            afterMerge = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-2",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-4",
                ),
            ),
            afterCommit = mapOf(
                File(dir, "r/storages") to listOf(
                    "${mockUUID(1)}-3",
                ),
                File(dir, "t/storages") to listOf(
                    "${mockUUID(1)}-4",
                ),
            ),
        )
    }

    @Test
    fun commitTest() {
        val stringsFinal = listOf(
            mockPayload(13).updated(pointer = 113),
            mockPayload(14).updated(pointer = 114),
            mockPayload(15),
            mockPayload(16),
            mockPayload(17),
        )
        val intsFinal = listOf(
            mockPayload(23, 23).updated(pointer = 123, 123),
            mockPayload(24, 24).updated(pointer = 124, 124),
            mockPayload(25, 25),
            mockPayload(26, 26),
            mockPayload(27, 27),
        )
        val longs = (1..5).map { number ->
            mockPayload(pointer = 30 + number, value = number.toLong())
        }
        val foos = (1..5).map { number ->
            mockPayload(pointer = 40 + number, value = Foo(text = "foo:${40 + number}"))
        }
        val bars = (1..5).map { number ->
            mockPayload(pointer = 50 + number, value = Bar(number = 50 + number))
        }
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, dir: File ->
            assertCommit(
                dstStorages = tStorages,
                srcStorages = rStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(4) to foos,
                ),
            )
        }
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages, dir: File ->
            assertCommit(
                dstStorages = rStorages,
                srcStorages = tStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(5) to bars,
                ),
            )
            assertCommit(
                dstStorages = tStorages,
                srcStorages = rStorages,
                files = mapOf(
                    File(dir, "r/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(5)}-0",
                    ),
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-2",
                        "${mockUUID(2)}-2",
                        "${mockUUID(3)}-1",
                        "${mockUUID(4)}-0",
                    ),
                ),
                items = mapOf(
                    mockUUID(1) to stringsFinal,
                    mockUUID(2) to intsFinal,
                    mockUUID(3) to longs,
                    mockUUID(4) to foos,
                ),
            )
        }
    }

    @Test
    fun commitErrorTest() {
        val hashes = MockHashFunction.hashes(
            emptyList<Payload<String>>() to "items:empty",
        )
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val session = mockSyncSession(
            src = MockHashFunction.hashOf(id = mockUUID(1), decoded = "items:empty"),
        )
        val storages = SyncStreamsStorages.Builder(session)
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "foo"),
                        ids = ids,
                    )
                },
            )
        val id = mockUUID(2)
        val infos = mapOf(id to mockCommitInfo())
        val throwable = assertThrows(IllegalStateException::class.java) {
            storages.commit(session = session, infos = infos)
        }
        assertEquals("No storage by ID: \"$id\"!", throwable.message)
    }

    @Test
    fun commitHashErrorTest() {
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val hashes = MockHashFunction.hashes(
            emptyList<Payload<String>>() to "items:empty",
        )
        val dir = File("/tmp/storages")
        dir.deleteRecursively()
        dir.mkdirs()
        val session = mockSyncSession(
            src = MockHashFunction.hashOf(id = mockUUID(1), decoded = "items:empty"),
        )
        val storages = SyncStreamsStorages.Builder(session)
            .add(mockUUID(1), StringTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                getStreamerProvider = { ids ->
                    assertEquals(listOf(mockUUID(1)), ids.sorted())
                    FileStreamerProvider(
                        dir = File(dir, "foo"),
                        ids = ids,
                    )
                },
            )
        val throwable = assertThrows(IllegalStateException::class.java) {
            val commitInfo = mockCommitInfo()
            check(commitInfo.items.isEmpty())
            check(commitInfo.deleted.isEmpty())
            check(!commitInfo.hash.contentEquals(storages.require<String>().hash))
            val cis = mapOf(mockUUID(1) to commitInfo)
            check(cis.keys.single() == mockUUID(1))
            storages.commit(session = session, infos = cis)
        }
        assertEquals("Wrong hash!", throwable.message)
    }

    companion object {
        private fun SyncStreamsStorages.assertHashes(expected: Map<UUID, ByteArray>) {
            val actual = hashes()
            assertEquals(expected.size, actual.size, "hashes:\n$expected\n$actual\n")
            for ((id, eh) in expected) {
                val ah = actual[id] ?: error("No hash by ID: \"$id\"!")
                assertEquals(eh.toHEX(), ah.toHEX(), "id: $id")
            }
        }

        private fun SyncStreamsStorages.assertSyncInfo(hashes: Map<UUID, ByteArray>, expected: Map<UUID, SyncInfo>) {
            val response = getSyncInfo(hashes)
            // todo response.session
            val actual = response.infos
            assertEquals(expected.size, actual.size, "SyncInfo:\n$expected\n$actual\n")
            for ((storageId, value) in expected) {
                val syncInfo = actual[storageId] ?: error("No hash by ID: \"$storageId\"!")
                val message = """
                    storageId: $storageId
                    es: $value
                    as: $syncInfo
                """.trimIndent()
                assertEquals(value.deleted.size, syncInfo.deleted.size, message)
                assertEquals(value.deleted.sorted(), syncInfo.deleted.sorted())
                assertEquals(value.infos.size, syncInfo.infos.size)
                for (key in value.infos.keys) {
                    val ei = value.infos[key]
                    checkNotNull(ei)
                    val ai = syncInfo.infos[key]
                    checkNotNull(ai)
                    assertEquals(ei, ai, "storage: $storageId\nkey: $key")
                }
                assertEquals(value, syncInfo, "storage: $storageId")
            }
        }

        private fun SyncStreamsStorages.assertMergeInfo(storage: SyncStreamsStorages, expected: Map<UUID, MergeInfo>) {
            val response = storage.getSyncInfo(hashes())
            val actual = getMergeInfo(session = response.session, infos = response.infos)
            assertEquals(expected.size, actual.size, "MergeInfo:\n$expected\n$actual\n")
            for ((id, value) in expected) {
                SyncStreamsStorageTest.assert(
                    expected = value,
                    actual = actual[id] ?: error("No hash by ID: \"$id\"!"),
                )
            }
        }

        private fun Payload<String>.updated(pointer: Int): Payload<String> {
            val newValue = "$value:$pointer:updated"
            return copy(
                updated = (1_000 + pointer).milliseconds,
                hash = MockHashFunction.map(newValue),
                value = newValue,
            )
        }

        private fun Payload<Int>.updated(pointer: Int, newValue: Int): Payload<Int> {
            return copy(
                updated = (1_000 + pointer).milliseconds,
                hash = MockHashFunction.map("$newValue"),
                value = newValue,
            )
        }

        fun <T : Any> Payload<T>.map(transformer: Transformer<T>): RawPayload {
            return map(transform = transformer::encode)
        }

        fun <T : Any> Payload<T>.map(transform: (T) -> ByteArray): RawPayload {
            return RawPayload(
                meta = meta,
                bytes = transform(value),
            )
        }

        private fun RawPayload.assert(other: RawPayload) {
            assertEquals(other.meta, meta)
            assertTrue(other.bytes.contentEquals(bytes), "e: ${other.bytes.toHEX()}\na: ${bytes.toHEX()}")
            assertEquals(other, this)
        }

        private fun <T : Any> Payload<T>.assert(other: Payload<T>) {
            assertEquals(other.meta, meta)
            assertEquals(other.value, value)
            assertEquals(other, this)
        }

        private fun assertFiles(
            files: Map<File, List<String>>,
        ) {
            check(files.isNotEmpty())
            files.forEach { (dir, expected) ->
                val children = dir.listFiles()
                check(!children.isNullOrEmpty()) { "dir: $dir" }
                check(expected.isNotEmpty())
                val message = """
                    dir: ${dir.absolutePath}
                    actual: ${children.map { it.name }}
                    expected: $expected
                """.trimIndent()
                assertTrue(children.all { it.exists() && it.isFile && it.length() > 0 }, message)
                assertEquals(expected, children.map { it.name }.sorted(), "dir: $dir")
            }
        }

        private fun onSyncStreamsStorages(
            commited: Boolean = true,
            block: (t: SyncStreamsStorages, r: SyncStreamsStorages, dir: File) -> Unit,
        ) {
            val strings = (1..5).map { number ->
                mockPayload(pointer = 10 + number)
            }
            val stringTUpdated = strings[2].updated(pointer = 113)
            val stringRUpdated = strings[3].updated(pointer = 114)
            // 00 01 02 03 04 05 06 07 08 09
            // __ 12 uu 14 15 16
            val stringsTUpdated = listOf(
                mockPayload(12),
                stringTUpdated,
                mockPayload(14),
                mockPayload(15),
                mockPayload(16),
            )
            // 00 01 02 03 04 05 06 07 08 09
            // 11 __ 13 uu 15 17
            val stringsRUpdated = listOf(
                mockPayload(11),
                mockPayload(13),
                stringRUpdated,
                mockPayload(15),
                mockPayload(17),
            )
            val stringsFinal = listOf(
                stringTUpdated,
                stringRUpdated,
                mockPayload(15),
                mockPayload(16),
                mockPayload(17),
            )
            val ints = (1..5).map { number ->
                mockPayload(pointer = 20 + number, value = 20 + number)
            }
            val intTUpdated = ints[2].updated(pointer = 123, newValue = 123)
            val intRUpdated = ints[3].updated(pointer = 124, newValue = 124)
            // 00 01 02 03 04 05 06 07 08 09
            // __ 22 uu 24 25 26
            val intsTUpdated = listOf(
                mockPayload(pointer = 22, value = 22),
                intTUpdated,
                mockPayload(pointer = 24, value = 24),
                mockPayload(pointer = 25, value = 25),
                mockPayload(pointer = 26, value = 26),
            )
            // 00 01 02 03 04 05 06 07 08 09
            // 21 __ 23 uu 25 27
            val intsRUpdated = listOf(
                mockPayload(pointer = 21, value = 21),
                mockPayload(pointer = 23, value = 23),
                intRUpdated,
                mockPayload(pointer = 25, value = 25),
                mockPayload(pointer = 27, value = 27),
            )
            val intsFinal = listOf(
                intTUpdated,
                intRUpdated,
                mockPayload(25, 25),
                mockPayload(26, 26),
                mockPayload(27, 27),
            )
            val longs = (1..5).map { number ->
                mockPayload(pointer = 30 + number, value = number.toLong())
            }
            val foos = (1..5).map { number ->
                mockPayload(pointer = 40 + number, value = Foo(text = "foo:${40 + number}"))
            }
            val bars = (1..5).map { number ->
                mockPayload(pointer = 50 + number, value = Bar(number = 50 + number))
            }
            val hashes = MockHashFunction.hashes(
                strings to "strings:hash",
                emptyList<Payload<Any>>() to "empty:hash",
                ints to "ints:hash",
                longs to "longs:hash",
                foos to "foos:hash",
                bars to "bars:hash",
                stringsTUpdated to "strings:hash:t:updated",
                stringsRUpdated to "strings:hash:r:updated",
                stringsFinal to "strings:hash:final",
                intsTUpdated to "ints:hash:t:updated",
                intsRUpdated to "ints:hash:r:updated",
                intsFinal to "ints:hash:final",
            ) + strings.map {
                StringTransformer.hashPair(it)
            } + ints.map {
                IntTransformer.hashPair(it)
            } + longs.map {
                LongTransformer.hashPair(it)
            } + foos.map {
                FooTransformer.hashPair(it)
            } + bars.map {
                BarTransformer.hashPair(it)
            } + listOf(
                StringTransformer.hashPair(mockPayload(pointer = 16)),
                StringTransformer.hashPair(mockPayload(pointer = 17)),
                StringTransformer.hashPair(stringTUpdated),
                StringTransformer.hashPair(stringRUpdated),
                IntTransformer.hashPair(mockPayload(pointer = 26, value = 26)),
                IntTransformer.hashPair(mockPayload(pointer = 27, value = 27)),
                IntTransformer.hashPair(intTUpdated),
                IntTransformer.hashPair(intRUpdated),
            )
            var time = 1.minutes
            val timeProvider = MockProvider { time }
            var itemId = mockUUID()
            val uuidProvider = MockProvider { itemId }
            val dateFormat = SimpleDateFormat("yyyyMMdd")
            val dir = File("/tmp/${dateFormat.format(Date())}")
            dir.deleteRecursively()
            dir.mkdirs()
            val tStorages = SyncStreamsStorages.Builder()
                .add(mockUUID(1), StringTransformer)
                .add(mockUUID(2), IntTransformer)
                .add(mockUUID(3), LongTransformer)
                .add(mockUUID(4), FooTransformer)
                .mock(
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    getStreamerProvider = { ids ->
                        val expected = listOf(
                            mockUUID(1),
                            mockUUID(2),
                            mockUUID(3),
                            mockUUID(4),
                        )
                        assertEquals(expected, ids.sorted())
                        FileStreamerProvider(
                            dir = File(dir, "t"),
                            ids = ids,
                        )
                    },
                )
            val rStorages = SyncStreamsStorages.Builder()
                .add(mockUUID(1), StringTransformer)
                .add(mockUUID(2), IntTransformer)
                .add(mockUUID(3), LongTransformer)
                .add(mockUUID(5), BarTransformer)
                .mock(
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    getStreamerProvider = { ids ->
                        val expected = listOf(
                            mockUUID(1),
                            mockUUID(2),
                            mockUUID(3),
                            mockUUID(5),
                        )
                        assertEquals(expected, ids.sorted())
                        FileStreamerProvider(
                            dir = File(dir, "r"),
                            ids = ids,
                        )
                    },
                )
            strings.forEach { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<String>().add(payload.value)
                if (!commited) rStorages.require<String>().add(payload.value)
            }
            ints.forEach { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<Int>().add(payload.value)
                if (!commited) rStorages.require<Int>().add(payload.value)
            }
            longs.forEach { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<Long>().add(payload.value)
                if (!commited) rStorages.require<Long>().add(payload.value)
            }
            foos.forEach { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<Foo>().add(payload.value)
            }
            bars.forEach { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                rStorages.require<Bar>().add(payload.value)
            }
            //
            assertFiles(
                mapOf(
                    File(dir, "t/storages") to listOf(
                        "${mockUUID(1)}-0",
                        "${mockUUID(2)}-0",
                        "${mockUUID(3)}-0",
                        "${mockUUID(4)}-0",
                    ),
                ),
            )
            if (commited) {
                assertFiles(
                    mapOf(
                        File(dir, "r/storages") to listOf(
                            "${mockUUID(5)}-0",
                        ),
                    ),
                )
                val response = tStorages.getSyncInfo(rStorages.hashes())
                val mis = rStorages.getMergeInfo(session = response.session, infos = response.infos)
                val cis = tStorages.merge(session = response.session, infos = mis)
                rStorages.commit(session = response.session, infos = cis)
                assertFiles(
                    mapOf(
                        File(dir, "t/storages") to listOf(
                            "${mockUUID(1)}-1",
                            "${mockUUID(2)}-1",
                            "${mockUUID(3)}-1",
                            "${mockUUID(4)}-0",
                        ),
                        File(dir, "r/storages") to listOf(
                            "${mockUUID(1)}-1",
                            "${mockUUID(2)}-1",
                            "${mockUUID(3)}-1",
                            "${mockUUID(5)}-0",
                        ),
                    ),
                )
            } else {
                assertFiles(
                    mapOf(
                        File(dir, "r/storages") to listOf(
                            "${mockUUID(1)}-0",
                            "${mockUUID(2)}-0",
                            "${mockUUID(3)}-0",
                            "${mockUUID(5)}-0",
                        ),
                    ),
                )
            }
            //
            check(tStorages.hashes().keys.sorted() == listOf(mockUUID(1), mockUUID(2), mockUUID(3), mockUUID(4)))
            check(rStorages.hashes().keys.sorted() == listOf(mockUUID(1), mockUUID(2), mockUUID(3), mockUUID(5)))
            //
            mockPayload(pointer = 16).also { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<String>().add(payload.value)
            }
            mockPayload(pointer = 17).also { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                rStorages.require<String>().add(payload.value)
            }
            check(tStorages.require<String>().delete(strings[0].meta.id))
            check(rStorages.require<String>().delete(strings[1].meta.id))
            stringTUpdated.also { payload ->
                itemId = payload.meta.id
                time = payload.meta.info.updated
                val info = tStorages.require<String>().update(payload.meta.id, payload.value)
                checkNotNull(info)
            }
            stringRUpdated.also { payload ->
                itemId = payload.meta.id
                time = payload.meta.info.updated
                val info = rStorages.require<String>().update(payload.meta.id, payload.value)
                checkNotNull(info)
            }
            //
            mockPayload(pointer = 26, value = 26).also { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                tStorages.require<Int>().add(payload.value)
            }
            mockPayload(pointer = 27, value = 27).also { payload ->
                itemId = payload.meta.id
                time = payload.meta.created
                rStorages.require<Int>().add(payload.value)
            }
            check(tStorages.require<Int>().delete(ints[0].meta.id))
            check(rStorages.require<Int>().delete(ints[1].meta.id))
            intTUpdated.also { payload ->
                itemId = payload.meta.id
                time = payload.meta.info.updated
                val info = tStorages.require<Int>().update(payload.meta.id, payload.value)
                checkNotNull(info)
            }
            intRUpdated.also { payload ->
                itemId = payload.meta.id
                time = payload.meta.info.updated
                val info = rStorages.require<Int>().update(payload.meta.id, payload.value)
                checkNotNull(info)
            }
            //
            block(tStorages, rStorages, dir)
        }
    }
}
