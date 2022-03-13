package sexy.kostya.mithra.bridge

interface World {

    val minY: Int
    val maxY: Int

    fun getChunk(x: Int, z: Int): Chunk?

    fun getChunkAt(x: Int, z: Int) = getChunk(x.toChunkCoordinate(), z.toChunkCoordinate())

}