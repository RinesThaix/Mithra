package sexy.kostya.mithra.engine.blocks

import it.unimi.dsi.fastutil.objects.Object2IntMap
import sexy.kostya.mithra.bridge.Block
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightModification
import sexy.kostya.mithra.engine.LightProcessor
import sexy.kostya.mithra.engine.RecursiveLightProcessor
import java.util.*
import kotlin.math.max

@Deprecated(message = "Slow and probably not working correctly")
class BlocksRecursiveLightProcessor : RecursiveLightProcessor() {

    override fun getLightLevel(location: BlockLocation) = location.getBlockLightLevel()

    override fun setLightLevel(location: BlockLocation, level: Int) = location.setBlockLightLevel(level)

    override fun spread(from: BlockLocation, level: Int, except: BlockLocation?): List<LightChangeEntry> {
        val result = ArrayList<LightChangeEntry>(6)
        val nextLevel = max(0, level - 1)
        fun BlockLocation?.handle() {
            if (this != null && this != except) {
                result.add(LightChangeEntry(from, this, nextLevel))
            }
        }
        for (delta in LightProcessor.LightSpreadingOffsets) {
            from.relative(delta, 0, 0).handle()
            from.relative(0, delta, 0).handle()
            from.relative(0, 0, delta).handle()
        }
        return result
    }

    override fun handleIncreasingLevel(
        modification: LightModification,
        stack: Stack<LightChangeEntry>,
        cache: Object2IntMap<BlockLocation>,
        from: BlockLocation,
        to: BlockLocation,
        previousLevel: Int,
        level: Int
    ) {
        val lightEmission = to.block.lightEmission
        if (lightEmission == previousLevel) {
            cache[to] = lightEmission
            if (previousLevel != level + 1 && !from.block.opaque) {
                stack.clear()
                stack.add(LightChangeEntry(to, from, previousLevel - 1))
            }
            return
        }
        check(!to.block.opaque)
        modification.add(to)
        if (lightEmission > level) {
            setLightLevel(to, lightEmission)
            cache[to] = lightEmission
            spread(to, lightEmission)
        } else {
            setLightLevel(to, level)
            cache[to] = level
            spread(to, level, from)
        }
    }

    override fun handleUpdate(location: BlockLocation, previous: Block, new: Block): LightModification? {
        val previousLevel = getLightLevel(location)
        val level = new.lightEmission
        if (previous.opaque == new.opaque) {
            if (previousLevel == level) {
                return null
            }
            if (!new.opaque && previousLevel > level) {
                if (previousLevel != previous.lightEmission) {
                    return null
                }
                if (previousLevel == getBlockLightFromNeighbors(location)) {
                    return null
                }
            }
        }
        val modification = LightModification()
        initializeLightSpreading(location, modification, previousLevel, level)
        return if (modification.isEmpty()) null else modification
    }

    private fun getBlockLightFromNeighbors(location: BlockLocation): Int {
        var level = 0
        fun BlockLocation?.handle() {
            if (this != null) {
                level = max(level, getLightLevel(this) - 1)
            }
        }
        for (delta in LightProcessor.LightSpreadingOffsets) {
            location.relative(delta, 0, 0).handle()
            location.relative(0, delta, 0).handle()
            location.relative(0, 0, delta).handle()
        }
        return level
    }
}