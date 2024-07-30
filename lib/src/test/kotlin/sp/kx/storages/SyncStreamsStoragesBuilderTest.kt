package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStreamsStoragesBuilderTest {
    @Test
    fun buildTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val hashes = listOf(
            ByteArray(0) to MockHashFunction.map("foo"),
        )
        val storages = SyncStreamsStorages.Builder()
            .add(id = id, StringTransformer)
            .build(
                hf = MockHashFunction(hashes = hashes),
                env = object : SyncStreamsStorage.Environment {
                    override fun now(): Duration {
                        return 1.milliseconds
                    }

                    override fun randomUUID(): UUID {
                        return mockUUID(42)
                    }
                },
                dir = dir,
            )
        assertEquals(id, storages.hashes().keys.single())
        val streamerProvider = FileStreamerProvider(dir = dir, ids = setOf(id))
        assertEquals(0, streamerProvider.getPointer(id))
    }

    @Test
    fun buildErrorTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val builder = SyncStreamsStorages.Builder()
        assertThrows(IllegalStateException::class.java) {
            builder.build(
                hf = MockHashFunction(),
                env = object : SyncStreamsStorage.Environment {
                    override fun now(): Duration {
                        return 1.milliseconds
                    }

                    override fun randomUUID(): UUID {
                        return mockUUID(42)
                    }
                },
                dir = dir,
            )
        }
    }
}
