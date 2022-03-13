package sexy.kostya.mithra.bridge

fun Int.toChunkCoordinate() = this shr 4

fun Int.fromChunkCoordinate() = this shl 4

fun Long.toChunkX() = this.shr(32).toInt()

fun Long.toChunkZ() = toInt()