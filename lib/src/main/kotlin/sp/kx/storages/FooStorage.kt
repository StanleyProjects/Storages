package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private class FooStorage(
    override val id: UUID,
) : MutableStorage<String> {
    private var bytes: ByteArray = """
        0
        0
        0
    """.trimIndent().toByteArray()

    override val hash: String
        get() = TODO("FooStorage:hash")
    override val list: List<Described<String>>
        get() = TODO("Not yet implemented")
    override val deleted: Set<UUID>
        get() = TODO("Not yet implemented")

    private fun read(): Items<String> {
        TODO()
    }

    private fun write(
        list: List<Described<String>>,
        deleted: Set<UUID>,
    ) {
        // todo hash
        TODO()
    }

    override fun delete(id: UUID) {
        val items = read()
        for (index in items.list.indices) {
            val it = items.list[index]
            if (it.id != id) continue
            val list = items.list.toMutableList()
            list.removeAt(index)
            write(
                deleted = items.deleted + id,
                list = list,
            )
            return
        }
        TODO("FooStorage:delete($id)")
    }

    private fun hash(item: String): String {
        return item.hashCode().toString()
    }

    override fun update(id: UUID, item: String) {
        val items = read()
        for (index in items.list.indices) {
            val it = items.list[index]
            if (it.id != id) continue
            val list = items.list.toMutableList()
            list.removeAt(index)
            list.add(
                it.copy(
                    updated = System.currentTimeMillis().milliseconds, // todo
                    hash = hash(item), // todo
                    item = item,
                ),
            )
            write(
                deleted = items.deleted + id,
                list = list,
            )
            return
        }
        TODO("FooStorage:update($id)")
    }

    override fun add(item: String) {
        val items = read()
        val list = items.list.toMutableList()
        val created = System.currentTimeMillis().milliseconds // todo
        list.add(
            Described(
                id = UUID.randomUUID(), // todo
                info = ItemInfo(
                    created = created,
                    updated = created,
                    hash = hash(item), // todo
                ),
                item = item,
            ),
        )
        write(
            deleted = items.deleted,
            list = list,
        )
    }
}
