package sp.kx.storages

import java.util.UUID

/**
 * Data changing tool.
 *
 * Usage:
 * ```
 * val storage: MutableStorage<String> = ...
 *
 * val deleted = storage.delete(UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80101"))
 * if (deleted) {
 *     println("item deleted")
 * } else {
 *     println("item not deleted")
 * }
 *
 * val described = storage.add("foo1")
 * println("new item: ${described.id}")
 *
 * val info = storage.update(
 *     id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80102"),
 *     item = "foo2",
 * )
 * if (info == null) {
 *    println("item not updated")
 * } else {
 *    println("item updated: ${info.updated}")
 * }
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
interface MutableStorage<T : Any> : Storage<T> {
    fun delete(id: UUID): Boolean
    fun add(item: T): Described<T>
    fun update(id: UUID, item: T): ItemInfo?
}
