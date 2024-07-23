package sp.kx.storages

import java.util.UUID

abstract class SyncStreamer(
    val id: UUID,
    val inputPointer: Long,
    val outputPointer: Long,
) : Streamer
