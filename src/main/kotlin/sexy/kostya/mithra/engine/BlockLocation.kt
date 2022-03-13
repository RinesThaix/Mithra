package sexy.kostya.mithra.engine

import sexy.kostya.mithra.bridge.*

data class BlockLocation(
    val chunk: Chunk,
    val sectionIndex: Int,
    val x: Int,
    val y: Int,
    val z: Int
) {

    companion object {

        fun of(expectedChunk: Chunk, x: Int, y: Int, z: Int): BlockLocation? {
            var chunk = expectedChunk
            val cx = x.toChunkCoordinate()
            val cz = z.toChunkCoordinate()
            if (cx != expectedChunk.x || z.toChunkCoordinate() != cz) {
                chunk = expectedChunk.world.getChunk(cx, cz) ?: return null
            }
            val sectionIndex = y.toChunkCoordinate()
            if (sectionIndex !in chunk.minSection until chunk.maxSection) {
                return null
            }
            return BlockLocation(chunk, sectionIndex, x, y, z)
        }

    }

    val block: Block
        get() = chunk.getBlock(x, y, z)

    val section: ChunkSection
        get() = chunk.getSection(sectionIndex)

    fun getSkyLightLevel() = section.getSkyLightLevel(x, y, z)

    fun setSkyLightLevel(level: Int) = section.setSkyLightLevel(x, y, z, level)

    fun getBlockLightLevel() = section.getBlockLightLevel(x, y, z)

    fun setBlockLightLevel(level: Int) = section.setBlockLightLevel(x, y, z, level)

    fun relative(dx: Int, dy: Int, dz: Int): BlockLocation? {
        val x = x + dx
        val y = y + dy
        val z = z + dz
        val cx = x.toChunkCoordinate()
        val cz = z.toChunkCoordinate()
        var chunk = chunk
        if (cx != chunk.x || cz != chunk.z) {
            if (chunk.world.getChunk(cx, cz) == null) {
                println("chunk is null: $x $y $z $cx $cz")
            }
            chunk = chunk.world.getChunk(cx, cz) ?: return null
        }
        val sectionIndex = y.toChunkCoordinate()
        if (sectionIndex !in chunk.minSection until chunk.maxSection) {
            println("sectionIndex is not within bounds: $y $sectionIndex ${chunk.minSection} ${chunk.maxSection}")
            return null
        }
        return BlockLocation(chunk, sectionIndex, x, y, z)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockLocation

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun toString() = "BlockLocation(x=$x, y=$y, z=$z)"

}