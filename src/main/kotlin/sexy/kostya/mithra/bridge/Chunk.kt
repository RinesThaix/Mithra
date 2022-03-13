package sexy.kostya.mithra.bridge

interface Chunk {

    val world: World
    val x: Int
    val z: Int
    val id: Long
        get() = x.toLong().shl(32) or z.toLong().and(0xFFFFFFFFL)

    val minSection: Int // inclusive
    val maxSection: Int // exclusive

    fun getSection(index: Int): ChunkSection

    fun getBlock(x: Int, y: Int, z: Int): Block

}