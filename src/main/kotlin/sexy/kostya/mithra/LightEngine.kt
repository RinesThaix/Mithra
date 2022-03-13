package sexy.kostya.mithra

import sexy.kostya.mithra.bridge.*
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightModification
import sexy.kostya.mithra.engine.LightProcessor
import sexy.kostya.mithra.engine.blocks.BlocksGraphBasedLightProcessor
import sexy.kostya.mithra.engine.sky.SkyGraphBasedLightProcessor

class LightEngine(
    val world: World,
    val skyProcessor: LightProcessor? = SkyGraphBasedLightProcessor(world.maxY),
    val blocksProcessor: LightProcessor? = BlocksGraphBasedLightProcessor()
) {

    private val worldMinSection = world.minY.toChunkCoordinate()

    /**
     * Must be called whenever block is being updated.
     *
     * @param expectedChunk mostly a tip for the engine to reduce amount of chunk retrievals. Even if specified,
     * engine will validate whether given block coords are within that chunk.
     * @param x             block x coordinate.
     * @param y             block y coordinate.
     * @param z             block z coordinate.
     * @param previous      block previous state.
     * @param new           block new stats.
     * @return list of light updates for every affected chunk. May be empty.
     */
    fun blockChanged(expectedChunk: Chunk? = null, x: Int, y: Int, z: Int, previous: Block, new: Block): List<ChunkLightUpdate> {
        val chunk = expectedChunk ?: world.getChunkAt(x, z) ?: throw IllegalStateException("could not handle block change within not loaded chunk")
        require(chunk.world == world) { "chunk's world must be the same as engine's world" }
        val location = BlockLocation.of(chunk, x, y, z) ?: throw IllegalStateException("could not handle block change within not loaded chunk")
        return LightModification.compileUpdate(
            world,
            worldMinSection,
            skyProcessor?.handleUpdate(location, previous, new),
            blocksProcessor?.handleUpdate(location, previous, new)
        )
    }

    fun chunkLoaded(chunk: Chunk) {
        skyProcessor?.handleChunkLoad(chunk)
        blocksProcessor?.handleChunkLoad(chunk)
    }

    fun chunkUnloaded(chunk: Chunk) {
        skyProcessor?.handleChunkUnload(chunk)
        blocksProcessor?.handleChunkUnload(chunk)
    }

}