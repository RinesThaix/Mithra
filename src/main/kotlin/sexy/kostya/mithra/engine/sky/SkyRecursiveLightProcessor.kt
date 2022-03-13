package sexy.kostya.mithra.engine.sky

import it.unimi.dsi.fastutil.objects.Object2IntMap
import sexy.kostya.mithra.bridge.Block
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightModification
import sexy.kostya.mithra.engine.LightProcessor
import sexy.kostya.mithra.engine.RecursiveLightProcessor
import java.util.*
import kotlin.math.max

@Deprecated(message = "Slow and probably not working correctly")
class SkyRecursiveLightProcessor : RecursiveLightProcessor() {

    override fun getLightLevel(location: BlockLocation) = location.getSkyLightLevel()

    override fun setLightLevel(location: BlockLocation, level: Int) = location.setSkyLightLevel(level)

    override fun spread(from: BlockLocation, level: Int, except: BlockLocation?): List<LightChangeEntry> {
        val result = ArrayList<LightChangeEntry>(6)
        val block = from.block
        val propagatesSkylightDown = block.propagatesSkylightDown
        val sideLevel = max(0, level - 1)
        val bottomLevel = if (propagatesSkylightDown && level == LightProcessor.MaxLightLevel) {
            LightProcessor.MaxLightLevel
        } else {
            sideLevel
        }

        fun BlockLocation?.handle(bottom: Boolean) {
            if (this != null && this != except) {
                result.add(LightChangeEntry(from, this, if (bottom) bottomLevel else sideLevel))
            }
        }
        for (delta in LightProcessor.LightSpreadingOffsets) {
            from.relative(delta, 0, 0).handle(false)
            from.relative(0, delta, 0).handle(delta == -1)
            from.relative(0, 0, delta).handle(false)
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
        val fromTop = from.x == to.x && from.y == to.y + 1 && from.z == to.z
        if (!fromTop && previousLevel == LightProcessor.MaxLightLevel) {
            val top = to.relative(0, 1, 0)
            if (top == null || cache.getInt(top) == LightProcessor.MaxLightLevel) {
                cache[to] = LightProcessor.MaxLightLevel
                if (!from.block.opaque) {
                    val fromBottom = from.x == to.x && from.y == to.y - 1 && from.z == to.z
                    stack.clear()
                    stack.add(
                        LightChangeEntry(
                            to,
                            from,
                            if (fromBottom && to.block.propagatesSkylightDown)
                                LightProcessor.MaxLightLevel
                            else
                                LightProcessor.MaxLightLevel - 1
                        )
                    )
                }
                return
            }
        }
        setLightLevel(to, level)
        modification.add(to)
        cache[to] = level
        spread(to, level, from)
    }

    override fun handleUpdate(location: BlockLocation, previous: Block, new: Block): LightModification? {
        if (previous.opaque == new.opaque) {
            return null
        }
        val modification = LightModification()
        val level = if (new.opaque) 0 else getSkyLightFromNeighbors(location)
        initializeLightSpreading(location, modification, getLightLevel(location), level)
        return if (modification.isEmpty()) null else modification
    }

    private fun getSkyLightFromNeighbors(location: BlockLocation): Int {
        var level = 0
        fun BlockLocation?.handle(bottom: Boolean) {
            if (this == null) {
                return
            }
            val thisLevel = getLightLevel(this)
            val relativeLevel = if (bottom && thisLevel == LightProcessor.MaxLightLevel && block.propagatesSkylightDown) {
                LightProcessor.MaxLightLevel
            } else {
                thisLevel - 1
            }
            level = max(0, relativeLevel)
        }
        for (delta in LightProcessor.LightSpreadingOffsets) {
            location.relative(delta, 0, 0).handle(false)
            location.relative(0, delta, 0).handle(delta == -1)
            location.relative(0, 0, delta).handle(false)
        }
        return level
    }
}