package com.reginaediscordia.softcombatduels;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SoftCombatDuels.MODID);

    private static final Identifier REACH_ID = Identifier.fromNamespaceAndPath(SoftCombatDuels.MODID, "boffer_reach");
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(SoftCombatDuels.MODID, "boffer_attack_speed");

    // ---- Boffer ----
    public static final Map<BofferType, DeferredItem<Item>> BOFFERS = new EnumMap<>(BofferType.class);

    // ---- Escudo redondo (bloquea como el escudo vanilla: 100% del daño frontal) ----
    public static final DeferredItem<Item> ROUND_SHIELD = ITEMS.registerItem("round_shield",
            props -> new Item(shieldProps(props)));

    // ---- Variantes de color del escudo (como los tabardos) ----
    public static final String[] SHIELD_VARIANTS = {"reginaediscordia", "rn", "templario", "vn", "bn", "ironheim"};
    public static final Map<String, DeferredItem<Item>> SHIELDS = new LinkedHashMap<>();

    private static Item.Properties shieldProps(Item.Properties props) {
        return props.durability(336)
                .component(DataComponents.BLOCKS_ATTACKS, Items.SHIELD.components().get(DataComponents.BLOCKS_ATTACKS));
    }

    // ---- Tabardos (pecho, +1 de armadura) ----
    public static final String[] TABARD_IDS = {
            "tabard_reginaediscordia", "tabard_rn", "tabard_templario", "tabard_vn", "tabard_bn", "tabard_ironheim"};
    public static final Map<String, DeferredItem<Item>> TABARDS = new LinkedHashMap<>();

    static {
        for (BofferType t : BofferType.values()) {
            BOFFERS.put(t, ITEMS.registerItem(t.id, props -> new BofferItem(boffer(props, t), t)));
        }
        for (String v : SHIELD_VARIANTS) {
            SHIELDS.put(v, ITEMS.registerItem("round_shield_" + v, props -> new Item(shieldProps(props))));
        }
        for (String id : TABARD_IDS) {
            TABARDS.put(id, ITEMS.registerItem(id, props -> new Item(tabard(props, id))));
        }
    }

    private static Item.Properties boffer(Item.Properties props, BofferType t) {
        ItemAttributeModifiers mods = ItemAttributeModifiers.builder()
                // alcance propio de cada Boffer (se sincroniza con el cliente)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(REACH_ID, t.reach - BofferType.DEFAULT_REACH, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                // mismo cooldown de ataque para todos (el actual de las espadas)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(SPEED_ID, -2.4D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
        return props.stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, mods)
                // bloqueo copiado del escudo vanilla: 100% del daño frontal
                .component(DataComponents.BLOCKS_ATTACKS, Items.SHIELD.components().get(DataComponents.BLOCKS_ATTACKS));
    }

    private static Item.Properties tabard(Item.Properties props, String id) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(SoftCombatDuels.MODID, id));
        Equippable equippable = Equippable.builder(EquipmentSlot.CHEST).setAsset(asset).build();
        ItemAttributeModifiers mods = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(SoftCombatDuels.MODID, id + "_armor"),
                                1.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.CHEST)
                .build();
        return props.stacksTo(1)
                .component(DataComponents.EQUIPPABLE, equippable)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, mods);
    }

    /** Tipo de Boffer de un item, o null si no es un Boffer. */
    public static BofferType typeOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Map.Entry<BofferType, DeferredItem<Item>> e : BOFFERS.entrySet()) {
            if (stack.is(e.getValue().get())) return e.getKey();
        }
        return null;
    }

    /** Escudo (o cualquier objeto que bloquea) que no sea un Boffer. */
    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && typeOf(stack) == null && stack.has(DataComponents.BLOCKS_ATTACKS);
    }

    /**
     * Reglas de manos:
     * - daga / espada corta: solas, con otra daga/espada corta, o con escudo;
     * - lanza: una mano, con escudo permitido, sin otro Boffer;
     * - bastarda: una mano, sin otro Boffer ni escudo;
     * - mandoble y Q-Tip: una sola arma, con la mano secundaria libre (configurable).
     */
    public static boolean handsAllowed(ItemStack main, ItemStack off) {
        BofferType m = typeOf(main), o = typeOf(off);
        if (m == null && o == null) return true;
        if (o != null && !o.dualWield && !main.isEmpty()) return false; // las armas largas no van en la secundaria
        if (m == null) return true;
        if (m.twoHanded && Config.GREATSWORD_NEEDS_FREE_OFFHAND.get()) return off.isEmpty();
        if (isShield(off)) return m.shield && Config.ALLOW_SHIELD.get();
        if (!m.dualWield) return o == null;
        return o == null || o.dualWield;
    }
}
