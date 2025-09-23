package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

data class ValueInfo(
    val id: UUID,
    val created: Duration,
)
