package sexy.kostya.mithra.engine

import sexy.kostya.mithra.bridge.Block
import sexy.kostya.mithra.bridge.Chunk

interface LightProcessor {

    companion object {
        const val MaxLightLevel = 15
        val LightSpreadingOffsets = intArrayOf(-1, 1)
    }

    fun handleUpdate(location: BlockLocation, previous: Block, new: Block): LightModification?

    fun handleChunkLoad(chunk: Chunk)

    fun handleChunkUnload(chunk: Chunk)

}