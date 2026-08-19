package com.vitwo.event;

import com.vitwo.arena.TowerArenaManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class TowerBlockInteractionHandler {
    public static void register() {
        // 1. Intercept Block Use (Beds, Machines, Placing Blocks, Containers)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.getRegistryKey().equals(TowerArenaManager.TOWER_DIMENSION_KEY)) {
                BlockState state = world.getBlockState(hitResult.getBlockPos());
                Block block = state.getBlock();
                Identifier blockId = Registries.BLOCK.getId(block);
                String idPath = blockId.getPath().toLowerCase();

                // A. Prevent placing ANY blocks in Tower Dimension
                if (player.getStackInHand(hand).getItem() instanceof BlockItem) {
                    if (!world.isClient()) {
                        player.sendMessage(Text.literal("§c[CobbleTower] Placing blocks is disabled in the Arena!"), true);
                    }
                    return ActionResult.FAIL;
                }

                // B. Seal Beds (Prevent Sleep & Bed Explosions)
                if (state.isIn(BlockTags.BEDS) || block instanceof BedBlock || idPath.contains("bed")) {
                    if (!world.isClient()) {
                        player.sendMessage(Text.literal("§c[CobbleTower] Sleeping is disabled in the Tower."), true);
                    }
                    return ActionResult.FAIL;
                }

                // C. Seal Cobblemon Healing Machines & PC
                if (idPath.contains("healing_machine") || idPath.contains("healer") || idPath.contains("healing") || idPath.contains("pc") || idPath.contains("pokebox") || idPath.contains("pasture")) {
                    if (!world.isClient()) {
                        player.sendMessage(Text.literal("§c[CobbleTower] Healing Machines & PC are sealed! Rest at Floor 5, 10, 15... Rest Stations."), true);
                    }
                    return ActionResult.FAIL;
                }

                // D. Seal Containers & Command Blocks
                if (block instanceof ChestBlock || block instanceof ShulkerBoxBlock || block instanceof BarrelBlock
                        || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof HopperBlock
                        || block instanceof CommandBlock || idPath.contains("command_block")) {
                    if (!world.isClient()) {
                        player.sendMessage(Text.literal("§c[CobbleTower] Structure storage and devices are locked!"), true);
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // 2. Prevent Breaking Blocks in Tower Dimension
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.getRegistryKey().equals(TowerArenaManager.TOWER_DIMENSION_KEY)) {
                if (!world.isClient()) {
                    player.sendMessage(Text.literal("§c[CobbleTower] Breaking blocks is disabled in the Arena!"), true);
                }
                return false; // Cancels block break
            }
            return true;
        });

        // 3. Prevent Attacking Blocks in Tower Dimension
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.getRegistryKey().equals(TowerArenaManager.TOWER_DIMENSION_KEY)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // 4. Immunity to Bed / Dimension Explosions in Tower Dimension
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (player.getServerWorld().getRegistryKey().equals(TowerArenaManager.TOWER_DIMENSION_KEY)) {
                    if (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.BAD_RESPAWN_POINT)) {
                        return false; // Complete explosion immunity
                    }
                }
            }
            return true;
        });
    }
}
