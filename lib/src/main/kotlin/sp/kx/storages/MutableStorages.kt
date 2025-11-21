package sp.kx.storages

interface MutableStorages : Storages {
    class Transaction private constructor(val operations: List<Operation>) {
        sealed interface Operation {
            class Add<T : Any>(val key: Storage.Key<T>, val value: T) : Operation
        }

        class Builder {
            private val operations = mutableListOf<Operation>()

            fun <T : Any> add(key: Storage.Key<T>, value: T): Builder {
                operations.add(Operation.Add(key = key, value = value))
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
