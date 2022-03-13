package sexy.kostya.mithra.bridge

import java.util.BitSet

data class ChunkLightUpdate(
    val chunk: Chunk,
    val skyLightMask: BitSet,
    val emptySkyLightMask: BitSet,
    val blockLightMask: BitSet,
    val emptyBlockLightMask: BitSet,
    val skyLights: List<ByteArray>,
    val blockLights: List<ByteArray>
)