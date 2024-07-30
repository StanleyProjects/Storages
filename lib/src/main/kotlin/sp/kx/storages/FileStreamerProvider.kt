package sp.kx.storages

import java.io.File
import java.util.UUID

internal class FileStreamerProvider(
    private val dir: File,
    ids: Set<UUID>,
) : SyncStreamsStorages.StreamerProvider {
    init {
        File(dir, "storages").mkdirs()
        val pointers = File(dir, "pointers")
        if (!pointers.exists() || pointers.length() == 0L) {
            pointers.writeBytes(toBytes(ids.associateWith { 0 }))
        }
    }

    override fun getStreamer(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        return FileStreamer(
            dir = File(dir, "storages"),
            id = id,
            inputPointer = inputPointer,
            outputPointer = outputPointer,
        )
    }

    private fun getValues(): Map<UUID, Int> {
        return fromBytes(File(dir, "pointers").readBytes())
    }

    override fun getPointer(id: UUID): Int {
        return getValues()[id] ?: error("No pointer by ID: \"$id\"!")
    }

    override fun putPointers(values: Map<UUID, Int>) {
        val newValues = getValues() + values
        File(dir, "pointers").writeBytes(toBytes(newValues))
        for (file in File(dir, "storages").listFiles()!!) {
            if (file.isDirectory) continue
            if (!file.isFile) continue
            for ((id, pointer) in newValues) {
                if (file.name.startsWith(id.toString()) && file.name != "$id-$pointer") {
                    file.delete()
                    break
                }
            }
        }
    }

    companion object {
        private fun toBytes(values: Map<UUID, Int>): ByteArray {
            val entries = values.entries
            val size = entries.size
            val bytes = ByteArray(4 + size * 20)
            BytesUtil.writeBytes(bytes = bytes, index = 0, value = size)
            entries.forEachIndexed { index, (id, pointer) ->
                BytesUtil.writeBytes(bytes = bytes, index = 4 + index * 20, value = id)
                BytesUtil.writeBytes(bytes = bytes, index = 4 + index * 20 + 16, value = pointer)
            }
            return bytes
        }

        private fun fromBytes(bytes: ByteArray): Map<UUID, Int> {
            val values = mutableMapOf<UUID, Int>()
            val size = BytesUtil.readInt(bytes = bytes, index = 0)
            for (index in 0 until size) {
                val id = BytesUtil.readUUID(bytes = bytes, index = 4 + index * 20)
                val pointer = BytesUtil.readInt(bytes = bytes, index = 4 + index * 20 + 16)
                values[id] = pointer
            }
            return values
        }
    }
}
