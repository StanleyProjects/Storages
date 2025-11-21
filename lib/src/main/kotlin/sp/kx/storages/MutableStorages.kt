package sp.kx.storages

import java.util.UUID

interface MutableStorages : Storages {
    class Transaction private constructor(val operations: List<Operation>) {
        sealed interface Operation {
            class Add<T : Any>(val key: Storage.Key<T>, val value: T) : Operation
            class Delete<T : Any>(val key: Storage.Key<T>, val id: UUID) : Operation
        }

        class Builder {
            private val operations = mutableListOf<Operation>()

            fun <T : Any> add(key: Storage.Key<T>, value: T): Builder {
                operations.add(Operation.Add(key = key, value = value))
                return this
            }

            fun <T : Any> delete(key: Storage.Key<T>, id: UUID): Builder {
                operations.add(Operation.Delete(key = key, id = id))
                return this
            }

            fun build(): Transaction {
                return Transaction(operations = ArrayList(operations))
            }
        }
    }

    override operator fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>?

    fun commit(transaction: Transaction)
}
