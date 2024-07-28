package sp.kx.storages

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal class FileStreamerProvider(
    private val root: File,
) : SyncStreamsStorages.StreamerProvider {
    init {
        root.mkdirs()
    }

    override fun get(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        return object : Streamer {
            override fun inputStream(): InputStream {
                val file = File(root, "$id-$inputPointer")
                if (!file.exists() || file.length() == 0L) {
                    file.writeBytes(ByteArray(8))
                }
                return file.inputStream()
            }

            override fun outputStream(): OutputStream {
                return File(root, "$id-$outputPointer").outputStream()
            }
        }
    }
}
