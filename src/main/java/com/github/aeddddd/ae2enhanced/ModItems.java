package com.github.aeddddd.ae2enhanced;

import com.github.aeddddd.ae2enhanced.item.ItemUpgradeCard;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public class ModItems {

    public static ItemUpgradeCard UPGRADE_CARD;

    public static void init() {
        UPGRADE_CARD = new ItemUpgradeCard();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(UPGRADE_CARD);
    }
}
