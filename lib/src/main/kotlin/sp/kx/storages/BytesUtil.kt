package sp.kx.storages

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import java.io.InputStream
import java.io.OutputStream
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal fun OutputStream.writeBytes(value: SyncInfo) {
    writeBytes(value = value.infos.size)
    value.infos.forEach { (id, itemInfo) ->
        writeBytes(value = id)
        writeBytes(value = itemInfo)
    }
    writeBytes(value = value.deleted.size)
    value.deleted.forEach { id ->
        writeBytes(value = id)
    }
}

internal fun OutputStream.writeBytes(value: ItemInfo) {
    writeBytes(value = value.created.inWholeMilliseconds)
    writeBytes(value = value.updated.inWholeMilliseconds)
    write(value.hash)
}

internal fun InputStream.readSyncInfo(hf: HashFunction): SyncInfo {
    val itemInfoSize = readInt()
    val infos = HashMap<UUID, ItemInfo>(itemInfoSize)
    for (i in 0 until itemInfoSize) {
        infos[readUUID()] = readItemInfo(hf = hf)
    }
    val deletedSize = readInt()
    val deleted = HashSet<UUID>(deletedSize)
    for (i in 0 until deletedSize) {
        deleted.add(readUUID())
    }
    return SyncInfo(
        infos = infos,
        deleted = deleted,
    )
}

internal fun InputStream.readItemInfo(hf: HashFunction): ItemInfo {
    return ItemInfo(
        created = readLong().milliseconds,
        updated = readLong().milliseconds,
        hash = readBytes(hf.size),
    )
}
