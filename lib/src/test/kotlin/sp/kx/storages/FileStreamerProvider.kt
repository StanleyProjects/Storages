package sp.kx.storages

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal class FileStreamerProvider(
    private val dir: File,
    pointers: Map<UUID, Int> = emptyMap(),
) : SyncStreamsStorages.StreamerProvider {
    private val values = pointers.toMutableMap()

    init {
        dir.mkdirs()
    }

    override fun getStreamer(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        return object : Streamer {
            override fun inputStream(): InputStream {
                val file = File(dir, "$id-$inputPointer")
                if (!file.exists() || file.length() == 0L) {
                    file.writeBytes(ByteArray(8))
                }
                return file.inputStream()
            }

            override fun outputStream(): OutputStream {
                return File(dir, "$id-$outputPointer").outputStream()
            }
        }
    }

    override fun getPointer(id: UUID): Int {
        return values[id] ?: TODO()
    }

    override fun putPointers(values: Map<UUID, Int>) {
        this.values.putAll(values)
        val files = dir.listFiles()!!
        for (file in files) {
            if (file.isDirectory) continue
            val exists = this.values.any { (id, pointer) -> file.name == "$id-$pointer"}
            if (!exists) file.delete()
        }
    }
}
