package com.reginaediscordia.softcombatduels;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SoftCombatDuels.MODID);

    /** Pestaña "SoftCombat Duels" del inventario creativo: Boffer, escudo redondo y tabardos. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SoftCombatDuels.MODID))
                    .icon(() -> new ItemStack(ModItems.BOFFERS.get(BofferType.SHORT_SWORD).get()))
                    .displayItems((parameters, output) -> {
                        for (DeferredItem<Item> item : ModItems.BOFFERS.values()) output.accept(item);
                        output.accept(ModItems.ROUND_SHIELD);
                        for (DeferredItem<Item> item : ModItems.SHIELDS.values()) output.accept(item);
                        for (DeferredItem<Item> item : ModItems.TABARDS.values()) output.accept(item);
                    })
                    .build());
}
