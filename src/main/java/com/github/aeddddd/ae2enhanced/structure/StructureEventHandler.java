package com.github.aeddddd.ae2enhanced.structure;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.ModBlocks;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public class StructureEventHandler {

    // 按维度分离的待验证控制器位置 -> 剩余 tick
    private static final Map<Integer, Map<BlockPos, Integer>> pendingChecks = new HashMap<>();

    @SubscribeEvent
    public static void onNeighborNotify(net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        checkSurroundingControllers(world, pos);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        // 如果是控制器本身被破坏，立即解散（不用等 tick）
        if (world.getBlockState(pos).getBlock() == ModBlocks.ASSEMBLY_CONTROLLER) {
            AssemblyStructure.disassemble(world, pos);
        }
        checkSurroundingControllers(world, pos);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

        World world = event.world;
        int dimId = world.provider.getDimension();
        Map<BlockPos, Integer> dimChecks = pendingChecks.get(dimId);
        if (dimChecks == null) return;

        dimChecks.entrySet().removeIf(entry -> {
            BlockPos controllerPos = entry.getKey();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                validateAndUpdate(world, controllerPos);
                return true;
            }
            entry.setValue(ticks);
            return false;
        });
    }

    private static void checkSurroundingControllers(World world, BlockPos changedPos) {
        ControllerIndex index = ControllerIndex.get(world);
        if (index == null) return;

        Set<BlockPos> controllers = index.getAll();
        for (BlockPos controllerPos : controllers) {
            BlockPos origin = AssemblyStructure.getOriginFromController(controllerPos);
            BlockPos rel = changedPos.subtract(origin);
            if (AssemblyStructure.ALL_SET.contains(rel)) {
                scheduleCheck(world.provider.getDimension(), controllerPos);
            }
        }
    }

    private static void scheduleCheck(int dimId, BlockPos controllerPos) {
        pendingChecks.computeIfAbsent(dimId, k -> new HashMap<>()).put(controllerPos, 20);
    }

    private static void validateAndUpdate(World world, BlockPos controllerPos) {
        if (world.getBlockState(controllerPos).getBlock() != ModBlocks.ASSEMBLY_CONTROLLER) {
            return;
        }

        boolean valid = AssemblyStructure.validate(world, controllerPos);
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof TileAssemblyController) {
            TileAssemblyController tile = (TileAssemblyController) te;
            if (valid && !tile.isFormed()) {
                AssemblyStructure.assemble(world, controllerPos);
            } else if (!valid && tile.isFormed()) {
                AssemblyStructure.disassemble(world, controllerPos);
            }
        }
    }
}
