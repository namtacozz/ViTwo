package com.vitwo.arena;

import com.vitwo.party.TowerParty;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Optional;

public class TowerArenaManager {
    private static final TowerArenaManager INSTANCE = new TowerArenaManager();
    public static TowerArenaManager getInstance() { return INSTANCE; }

    public static final RegistryKey<World> TOWER_DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("vitwo", "tower_dimension")
    );

    private TowerArenaManager() {}

    /**
     * Teleports party members directly to the interior ground floor inside the Gym Arena,
     * ensuring zero collision, clear headroom, and seamless entry.
     */
    public void teleportPartyToArena(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member) {
        if (leader == null || leader.getServer() == null) return;

        // Obtain Tower Dimension world (or fallback to leader's current world)
        ServerWorld arenaWorld = leader.getServer().getWorld(TOWER_DIMENSION_KEY);
        if (arenaWorld == null) {
            arenaWorld = leader.getServerWorld();
        }

        int floor = party.getCurrentFloor();

        // Calculate isolated sector coordinates
        int sectorX = ((Math.abs(party.getLeaderId().hashCode()) % 500) + 1) * 350;
        int sectorZ = ((floor % 50) + 1) * 350;
        
        // Base origin placed at Y=63 so relative layer Y=1 of structure aligns flush at Y=64 ground level
        BlockPos arenaOrigin = new BlockPos(sectorX, 63, sectorZ);

        // Load and place NBT structure template
        String structurePath = ArenaTemplatePool.getStructureForFloor(floor);
        Vec3i arenaSize = placeStructureSafely(arenaWorld, arenaOrigin, structurePath);

        // Calculate interior ground coordinates
        int centerX = arenaOrigin.getX() + (arenaSize.getX() / 2);
        double spawnY = 64.0;
        double spawnZ = arenaOrigin.getZ() + 6.0;

        // Clear headroom in spawn zone to prevent Minecraft suffocation anti-cheat from pushing players to roof
        clearSpawnHeadroom(arenaWorld, centerX, 64, (int) spawnZ);

        // Teleport players in Adventure mode (Anti-grief)
        if (party.isSolo() || member == null) {
            leader.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, centerX + 0.5, spawnY, spawnZ, 0.0f, 0.0f);
        } else {
            leader.changeGameMode(GameMode.ADVENTURE);
            member.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, centerX - 1.5, spawnY, spawnZ, 0.0f, 0.0f);
            member.teleport(arenaWorld, centerX + 1.5, spawnY, spawnZ, 0.0f, 0.0f);
        }
    }

    /**
     * Clears a safe 5x3x5 pocket of air at the spawn foyer so player never suffocates or gets pushed up to the roof
     */
    private void clearSpawnHeadroom(ServerWorld world, int centerX, int floorY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // Ensure solid floor underneath
                BlockPos floorPos = new BlockPos(centerX + dx, floorY - 1, centerZ + dz);
                if (world.getBlockState(floorPos).isAir()) {
                    world.setBlockState(floorPos, Blocks.POLISHED_DEEPSLATE.getDefaultState(), Block.NOTIFY_LISTENERS);
                }

                // Clear 3 blocks of air above the floor
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos airPos = new BlockPos(centerX + dx, floorY + dy, centerZ + dz);
                    if (!world.getBlockState(airPos).isAir()) {
                        world.setBlockState(airPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    /**
     * Places the NBT structure safely and cleans up all command blocks & structure blocks.
     * Generates a flush bedrock foundation and ground platform around the entire facility.
     */
    private Vec3i placeStructureSafely(ServerWorld world, BlockPos origin, String structureIdentifier) {
        Vec3i size = new Vec3i(36, 18, 36);

        try {
            String[] parts = structureIdentifier.split(":");
            if (parts.length == 2) {
                Identifier id = Identifier.of(parts[0], parts[1]);
                StructureTemplateManager templateManager = world.getStructureTemplateManager();
                Optional<StructureTemplate> templateOpt = templateManager.getTemplate(id);

                if (templateOpt.isPresent()) {
                    StructureTemplate template = templateOpt.get();
                    size = template.getSize();
                    StructurePlacementData placementData = new StructurePlacementData();
                    template.place(world, origin, origin, placementData, world.getRandom(), Block.NOTIFY_LISTENERS);
                }
            }
        } catch (Exception ignored) {}

        // Construct a safety bedrock platform at Y=61 and smooth deepslate ground at Y=62 & Y=63
        int minX = origin.getX() - 10;
        int maxX = origin.getX() + size.getX() + 10;
        int minZ = origin.getZ() - 10;
        int maxZ = origin.getZ() + size.getZ() + 10;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, 61, z), Blocks.BEDROCK.getDefaultState(), Block.NOTIFY_LISTENERS);
                world.setBlockState(new BlockPos(x, 62, z), Blocks.POLISHED_DEEPSLATE.getDefaultState(), Block.NOTIFY_LISTENERS);
                world.setBlockState(new BlockPos(x, 63, z), Blocks.POLISHED_DEEPSLATE.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }

        // Purge ALL Command Blocks, Structure Blocks, Jigsaws, Barriers across the entire arena volume
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 55; y <= 120; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    Block b = world.getBlockState(p).getBlock();
                    if (b instanceof CommandBlock || b instanceof StructureBlock || b == Blocks.BARRIER) {
                        if (y <= 63) {
                            world.setBlockState(p, Blocks.POLISHED_DEEPSLATE.getDefaultState(), Block.NOTIFY_LISTENERS);
                        } else {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }

        return size;
    }

    public void returnPlayerToOriginalPos(ServerPlayerEntity player, BlockPos originalPos) {
        if (player != null && player.getServer() != null) {
            ServerWorld overworld = player.getServer().getOverworld();
            BlockPos targetPos = originalPos != null ? originalPos : overworld.getSpawnPos();
            player.changeGameMode(GameMode.SURVIVAL);
            player.teleport(overworld, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
    }
}
