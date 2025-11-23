package sp.kx.storages

import java.util.UUID

class Transaction private constructor(val operations: List<Operation>) {
    sealed interface Operation {
        class Add<T : Any>(val key: Storage.Key<T>, val value: T) : Operation
        class Delete<T : Any>(val key: Storage.Key<T>, val id: UUID) : Operation
        class DeleteFirst<T : Any>(val key: Storage.Key<T>, val condition: (Payload<T>) -> Boolean) : Operation
        class Update<T : Any>(val key: Storage.Key<T>, val id: UUID, val value: T) : Operation
        class UpdateFirst<T : Any>(val key: Storage.Key<T>, val value: T, val condition: (Payload<T>) -> Boolean) : Operation
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

        fun <T : Any> deleteFirst(key: Storage.Key<T>, condition: (Payload<T>) -> Boolean): Builder {
            operations.add(Operation.DeleteFirst(key = key, condition = condition))
            return this
        }

        fun <T : Any> update(key: Storage.Key<T>, id: UUID, value: T): Builder {
            operations.add(Operation.Update(key = key, id = id, value = value))
            return this
        }

        fun <T : Any> updateFirst(key: Storage.Key<T>, value: T, condition: (Payload<T>) -> Boolean): Builder {
            operations.add(Operation.UpdateFirst(key = key, value = value, condition = condition))
            return this
        }

        fun build(): Transaction {
            return Transaction(operations = ArrayList(operations))
        }
    }
}
