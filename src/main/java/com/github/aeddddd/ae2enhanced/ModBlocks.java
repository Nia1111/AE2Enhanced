package com.github.aeddddd.ae2enhanced;

import com.github.aeddddd.ae2enhanced.block.*;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyMeInterface;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public class ModBlocks {

    public static BlockAssemblyController ASSEMBLY_CONTROLLER;
    public static BlockAssemblyMeInterface ASSEMBLY_ME_INTERFACE;
    public static BlockAssemblyCasing ASSEMBLY_CASING;
    public static BlockAssemblyInnerWall ASSEMBLY_INNER_WALL;
    public static BlockAssemblyStabilizer ASSEMBLY_STABILIZER;

    public static void init() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
            ASSEMBLY_CONTROLLER = new BlockAssemblyController(),
            ASSEMBLY_ME_INTERFACE = new BlockAssemblyMeInterface(),
            ASSEMBLY_CASING = new BlockAssemblyCasing(),
            ASSEMBLY_INNER_WALL = new BlockAssemblyInnerWall(),
            ASSEMBLY_STABILIZER = new BlockAssemblyStabilizer()
        );

        GameRegistry.registerTileEntity(TileAssemblyController.class, AE2Enhanced.MOD_ID + ":assembly_controller");
        GameRegistry.registerTileEntity(TileAssemblyMeInterface.class, AE2Enhanced.MOD_ID + ":assembly_me_interface");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            new ItemBlock(ASSEMBLY_CONTROLLER).setRegistryName(ASSEMBLY_CONTROLLER.getRegistryName()),
            new ItemBlock(ASSEMBLY_ME_INTERFACE).setRegistryName(ASSEMBLY_ME_INTERFACE.getRegistryName()),
            new ItemBlock(ASSEMBLY_CASING).setRegistryName(ASSEMBLY_CASING.getRegistryName()),
            new ItemBlock(ASSEMBLY_INNER_WALL).setRegistryName(ASSEMBLY_INNER_WALL.getRegistryName()),
            new ItemBlock(ASSEMBLY_STABILIZER).setRegistryName(ASSEMBLY_STABILIZER.getRegistryName())
        );
    }
}
