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
            val text = ids.joinToString(separator = ",") { id ->
                "$id:0"
            }
            File(dir, "pointers").writeText(text)
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
        val text = File(dir, "pointers").readText()
        check(text.isNotEmpty())
        return text.split(",").associate {
            val (_id, _pointer) = it.split(":")
            UUID.fromString(_id) to _pointer.toInt()
        }
    }

    override fun getPointer(id: UUID): Int {
        return getValues()[id] ?: error("No pointer by ID: \"$id\"!")
    }

    override fun putPointers(values: Map<UUID, Int>) {
        val newValues = getValues() + values
        val text = newValues.entries.joinToString(separator = ",") { (id, pointer) ->
            "$id:$pointer"
        }
        File(dir, "pointers").writeText(text)
        for (file in File(dir, "storages").listFiles()!!) {
            if (file.isDirectory) continue
            val exists = newValues.any { (id, pointer) -> file.name == "$id-$pointer"}
            if (!exists) file.delete()
        }
    }
}
