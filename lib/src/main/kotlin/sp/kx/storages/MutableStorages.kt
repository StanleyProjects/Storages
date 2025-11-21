package sp.kx.storages

interface MutableStorages : Storages {
    class Transaction {
        internal sealed interface Operation {
            class Add<T : Any>(val key: Storage.Key<T>, value: T) : Operation
        }

        private val operations = mutableListOf<Operation>()

        fun <T : Any> add(key: Storage.Key<T>, value: T): Transaction {
            operations.add(Operation.Add(key = key, value = value))
            return this
        }
    }

    override operator fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>?

    fun commit(transaction: Transaction)
}
