package com.vitwo.block;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ViTwoBlocks {
    public static final Block TOWER_GATEWAY = new TowerGatewayBlock(
            AbstractBlock.Settings.create()
                    .strength(3.5f, 6.0f)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                    .luminance(state -> 12)
    );

    public static void registerBlocks() {
        Identifier id = Identifier.of(ViTwoPackets.MOD_ID, "tower_gateway");
        Registry.register(Registries.BLOCK, id, TOWER_GATEWAY);
        Registry.register(Registries.ITEM, id, new BlockItem(TOWER_GATEWAY, new Item.Settings()));
    }
}
