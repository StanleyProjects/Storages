package sp.kx.storages

import kotlin.time.Duration

class ValueState(
    val updated: Duration,
    val hash: ByteArray,
)
