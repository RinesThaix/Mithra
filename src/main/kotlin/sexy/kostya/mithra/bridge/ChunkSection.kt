package sexy.kostya.mithra.bridge

class ChunkSection(
    skyLight: ByteArray,
    blockLight: ByteArray
) {

    companion object {
        internal const val Size = 16
        internal val SizeRange = 0 until Size

        private val EmptyArray = ByteArray(0)
        private val DefaultSkyLight: ByteArray
            get() = ByteArray(2048).apply { fill(0xFF.toByte()) }

        internal fun index(x: Int, y: Int, z: Int) =
            ((y and 0xF) shl 8) or ((z and 0xF) shl 4) or (x and 0xF)

        private fun getLightLevel(light: ByteArray, index: Int) = when {
            light.isEmpty() -> 0
            index and 1 == 0 -> light[index shr 1].toInt() and 0xF
            else -> light[index shr 1].toInt().shr(4) and 0xF
        }

        private fun getLightLevel(light: ByteArray, x: Int, y: Int, z: Int) = getLightLevel(light, index(x, y, z))

        private fun countNonZeroBlocks(light: ByteArray): Int {
            var result = 0
            for (x in SizeRange) {
                for (y in SizeRange) {
                    for (z in SizeRange) {
                        if (getLightLevel(light, x, y, z) > 0) {
                            ++result
                        }
                    }
                }
            }
            return result
        }
    }

    constructor() : this(DefaultSkyLight, EmptyArray)

    var skyLight = skyLight
        private set
    var blockLight = blockLight
        private set
    var skyLightEntries = 0
        private set
    var blockLightEntries = 0
        private set

    init {
        if (skyLight.isNotEmpty()) {
            skyLightEntries = countNonZeroBlocks(skyLight)
            if (skyLightEntries == 0) {
                this.skyLight = EmptyArray
            }
        }
        if (blockLight.isNotEmpty()) {
            blockLightEntries = countNonZeroBlocks(blockLight)
            if (blockLightEntries == 0) {
                this.blockLight = EmptyArray
            }
        }
    }

    fun updateSkyLight(skyLight: ByteArray) {
        this.skyLight = skyLight
        if (skyLight.isEmpty()) {
            skyLightEntries = 0
        } else {
            skyLightEntries = countNonZeroBlocks(skyLight)
            if (skyLightEntries == 0) {
                this.skyLight = EmptyArray
            }
        }
    }

    fun updateBlockLight(blockLight: ByteArray) {
        this.blockLight = blockLight
        if (blockLight.isEmpty()) {
            blockLightEntries = 0
        } else {
            blockLightEntries = countNonZeroBlocks(blockLight)
            if (blockLightEntries == 0) {
                this.blockLight = EmptyArray
            }
        }
    }

    fun clear() {
        skyLight = DefaultSkyLight
        skyLightEntries = Size * Size * Size
        blockLight = EmptyArray
        blockLightEntries = 0
    }

    fun getSkyLightLevel(x: Int, y: Int, z: Int) = getLightLevel(skyLight, x, y, z)

    fun setSkyLightLevel(x: Int, y: Int, z: Int, level: Int) = setLightLevel(true, x, y, z, level)

    fun getBlockLightLevel(x: Int, y: Int, z: Int) = getLightLevel(blockLight, x, y, z)

    fun setBlockLightLevel(x: Int, y: Int, z: Int, level: Int) = setLightLevel(false, x, y, z, level)

    private fun setLightLevel(sky: Boolean, x: Int, y: Int, z: Int, level: Int): Boolean {
        require(level in 0..15) { "Light level must be within [0; 15] bounds" }
        val index = index(x, y, z)
        var light = if (sky) skyLight else blockLight
        val previousLevel = getLightLevel(light, index)
        if (previousLevel == level) {
            return false
        }
        if (light.isEmpty()) {
            light = ByteArray(2048)
            if (sky) skyLight = light else blockLight = light
        }
        val value = light[index shr 1]
        light[index shr 1] = if (index and 1 == 0) {
            value.toInt().and(0xF0).or(level).toByte()
        } else {
            value.toInt().and(0x0F).or(level shl 4).toByte()
        }
        if (previousLevel == 0) {
            if (sky) ++skyLightEntries else ++blockLightEntries
        } else if (level == 0) {
            if (sky && --skyLightEntries == 0) {
                skyLight = EmptyArray
            } else if (!sky && --blockLightEntries == 0) {
                blockLight = EmptyArray
            }
        }
        return true
    }

}