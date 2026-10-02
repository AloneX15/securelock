package com.takumistudios.securemod.registry;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.item.AdminToolItem;
import com.takumistudios.securemod.item.CardWriterItem;
import com.takumistudios.securemod.item.KeycardItem;
import com.takumistudios.securemod.item.PadlockItem;
import com.takumistudios.securemod.item.UniversalBlockRemoverItem;
import com.takumistudios.securemod.item.UniversalModifierItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ModItems {
    private static final List<Item> TAB_ORDER = new ArrayList<>();

    // --- Bloques ---
    public static final Item REINFORCED_IRON_DOOR = block(ModBlocks.REINFORCED_IRON_DOOR, DoubleHighBlockItem::new);
    public static final Item REINFORCED_OAK_DOOR = block(ModBlocks.REINFORCED_OAK_DOOR, DoubleHighBlockItem::new);
    public static final Item REINFORCED_IRON_TRAPDOOR = block(ModBlocks.REINFORCED_IRON_TRAPDOOR, BlockItem::new);
    public static final Item REINFORCED_OAK_FENCE_GATE = block(ModBlocks.REINFORCED_OAK_FENCE_GATE, BlockItem::new);
    public static final Item PASSCODE_CHEST = block(ModBlocks.PASSCODE_CHEST, BlockItem::new);
    public static final Item PASSCODE_BARREL = block(ModBlocks.PASSCODE_BARREL, BlockItem::new);
    public static final Item KEYPAD = block(ModBlocks.KEYPAD, BlockItem::new);
    public static final Item CARD_READER = block(ModBlocks.CARD_READER, BlockItem::new);
    public static final Item BIOMETRIC_SCANNER = block(ModBlocks.BIOMETRIC_SCANNER, BlockItem::new);

    // --- Herramientas ---
    public static final Item PADLOCK = item("padlock", PadlockItem::new, new Item.Properties().stacksTo(16));
    public static final Item UNIVERSAL_MODIFIER = item("universal_modifier", UniversalModifierItem::new, new Item.Properties().stacksTo(1));
    public static final Item UNIVERSAL_BLOCK_REMOVER = item("universal_block_remover", UniversalBlockRemoverItem::new, new Item.Properties().durability(64));
    public static final Item CARD_WRITER = item("card_writer", CardWriterItem::new, new Item.Properties().stacksTo(1));
    public static final Item KEYCARD_LV1 = keycard(1);
    public static final Item KEYCARD_LV2 = keycard(2);
    public static final Item KEYCARD_LV3 = keycard(3);
    public static final Item KEYCARD_LV4 = keycard(4);
    public static final Item KEYCARD_LV5 = keycard(5);
    public static final Item ADMIN_TOOL = item("admin_tool", AdminToolItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, "main"));

    private ModItems() {
    }

    private static Item block(Block block, BiFunction<Block, Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item item = factory.apply(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
        TAB_ORDER.add(item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, name));
        Item item = factory.apply(properties.setId(key));
        TAB_ORDER.add(item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static Item keycard(int level) {
        Rarity rarity = level >= 5 ? Rarity.EPIC : level >= 3 ? Rarity.RARE : Rarity.UNCOMMON;
        return item("keycard_lv" + level, p -> new KeycardItem(level, p), new Item.Properties().stacksTo(1).rarity(rarity));
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.securemod.main"))
                .icon(() -> new ItemStack(KEYPAD))
                .displayItems((parameters, output) -> TAB_ORDER.forEach(output::accept))
                .build());
    }
}
