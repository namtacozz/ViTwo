package com.vitwo.arena;

import com.vitwo.battle.TrainerPool;
import com.vitwo.party.TowerParty;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class TowerArenaManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-ArenaManager");
    private static final TowerArenaManager INSTANCE = new TowerArenaManager();
    public static TowerArenaManager getInstance() { return INSTANCE; }

    public static final RegistryKey<World> TOWER_DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("vitwo", "tower_dimension")
    );

    private TowerArenaManager() {}

    /**
     * Teleports party members directly to the interior ground floor inside the Gym Arena,
     * ensuring zero collision, clear headroom, barrier perimeter enclosure, and seamless entry.
     */
    public void teleportPartyToArena(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member) {
        if (leader == null || leader.getServer() == null) return;

        // Obtain Tower Dimension world (or fallback to leader's current world)
        ServerWorld arenaWorld = leader.getServer().getWorld(TOWER_DIMENSION_KEY);
        if (arenaWorld == null) {
            arenaWorld = leader.getServerWorld();
        }

        int floor = party.getCurrentFloor();

        // Calculate isolated sector coordinates per party and floor tier
        int partyHash = Math.abs(party.getLeaderId().hashCode() % 1000);
        int sectorX = (partyHash % 40) * 800;
        int sectorZ = (partyHash / 40) * 800 + ((floor % 5) * 150);

        // Base origin placed at Y=63 so relative layer Y=1 of structure aligns flush at Y=64 ground level
        BlockPos arenaOrigin = new BlockPos(sectorX, 63, sectorZ);

        // Clear any old entities/blocks in this sector box before pasting new structure
        purgeSector(arenaWorld, sectorX, sectorZ, 80, 80);

        // Load and place NBT structure template
        String structurePath = ArenaTemplatePool.getStructureForFloor(floor);
        Vec3i arenaSize = placeStructureSafely(arenaWorld, arenaOrigin, structurePath);

        // Calculate center coordinates
        int centerX = arenaOrigin.getX() + (arenaSize.getX() / 2);
        int centerZ = arenaOrigin.getZ() + (arenaSize.getZ() / 2);
        int playerZ = centerZ - 6;
        int trainerZ = centerZ + 6;

        // Find true solid ground heights dynamically
        int playerGroundY = findSafeGroundY(arenaWorld, centerX, playerZ, 64);
        int trainerGroundY = findSafeGroundY(arenaWorld, centerX, trainerZ, playerGroundY);

        double spawnY = playerGroundY + 1.0;

        // Clear headroom in spawn zone
        clearSpawnHeadroom(arenaWorld, centerX, playerGroundY + 1, playerZ);

        // Build Barrier Ring around the active battle area (keeps player and trainer focused in ring)
        if (floor % 5 != 0 || floor == 100) {
            buildArenaBarrierBox(arenaWorld, centerX, playerGroundY, centerZ, 14, 14, 8);
        }

        // Teleport players in Adventure mode (Anti-grief)
        if (party.isSolo() || member == null) {
            leader.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, centerX + 0.5, spawnY, playerZ + 0.5, 0.0f, 0.0f);
        } else {
            leader.changeGameMode(GameMode.ADVENTURE);
            member.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, centerX - 1.5, spawnY, playerZ + 0.5, 0.0f, 0.0f);
            member.teleport(arenaWorld, centerX + 1.5, spawnY, playerZ + 0.5, 0.0f, 0.0f);
        }

        // Spawn Trainer NPC entity for all battle floors (including Floor 100 Boss)
        if (floor % 5 != 0 || floor == 100) {
            clearOldNpcsInSector(arenaWorld, sectorX, sectorZ, arenaSize);
            spawnTowerTrainerNpc(arenaWorld, floor, centerX + 0.5, trainerGroundY + 1.0, trainerZ + 0.5);
        }
    }

    /**
     * Constructs a transparent Barrier safety box around the fighting stage so players cannot escape or wander
     */
    private void buildArenaBarrierBox(ServerWorld world, int centerX, int groundY, int centerZ, int radiusX, int radiusZ, int height) {
        int minX = centerX - radiusX;
        int maxX = centerX + radiusX;
        int minZ = centerZ - radiusZ;
        int maxZ = centerZ + radiusZ;
        int topY = groundY + height;

        // Perimeter Walls
        for (int y = groundY + 1; y <= topY; y++) {
            for (int x = minX; x <= maxX; x++) {
                setBarrierIfEmptyOrWeak(world, new BlockPos(x, y, minZ));
                setBarrierIfEmptyOrWeak(world, new BlockPos(x, y, maxZ));
            }
            for (int z = minZ; z <= maxZ; z++) {
                setBarrierIfEmptyOrWeak(world, new BlockPos(minX, y, z));
                setBarrierIfEmptyOrWeak(world, new BlockPos(maxX, y, z));
            }
        }

        // Ceiling
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setBarrierIfEmptyOrWeak(world, new BlockPos(x, topY, z));
            }
        }
    }

    private void setBarrierIfEmptyOrWeak(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isAir() || world.getBlockState(pos).getBlock() == Blocks.STRUCTURE_VOID) {
            world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * Purges previous entities and structures in the sector box
     */
    private void purgeSector(ServerWorld world, int sectorX, int sectorZ, int sizeX, int sizeZ) {
        try {
            Box box = new Box(sectorX - 20, 50, sectorZ - 20, sectorX + sizeX + 20, 130, sectorZ + sizeZ + 20);
            List<Entity> entities = world.getOtherEntities(null, box);
            for (Entity e : entities) {
                if (!(e instanceof ServerPlayerEntity)) {
                    e.discard();
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Purges previous NPC entities in the sector before placing a new one
     */
    private void clearOldNpcsInSector(ServerWorld world, int sectorX, int sectorZ, Vec3i size) {
        try {
            Box box = new Box(sectorX - 20, 50, sectorZ - 20, sectorX + size.getX() + 20, 130, sectorZ + size.getZ() + 20);
            List<Entity> entities = world.getOtherEntities(null, box);
            for (Entity e : entities) {
                if (e instanceof ServerPlayerEntity) continue;
                e.discard(); // Purge all non-player entities from the arena sector
            }
        } catch (Exception ignored) {}
    }

    /**
     * Spawns an RCT Trainer NPC entity with chosen trainerId matching the floor stage
     */
    private void spawnTowerTrainerNpc(ServerWorld world, int floor, double x, double y, double z) {
        try {
            EntityType<?> trainerType = Registries.ENTITY_TYPE.get(Identifier.of("rctmod", "trainer"));
            if (trainerType != null) {
                Entity npc = trainerType.create(world);
                if (npc != null) {
                    npc.refreshPositionAndAngles(x, y, z, 180.0f, 0.0f);
                    String chosenTrainerId = TrainerPool.getRctTrainerIdForFloor(floor);
                    String displayName = TrainerPool.getTrainerDisplayName(floor);

                    // Set Trainer ID and persistence via reflection
                    try {
                        Method setTrainerIdMethod = npc.getClass().getMethod("setTrainerId", String.class);
                        setTrainerIdMethod.invoke(npc, chosenTrainerId);

                        Method setPersistentMethod = npc.getClass().getMethod("setPersistent", boolean.class);
                        setPersistentMethod.invoke(npc, true);

                        Method setHomePosMethod = npc.getClass().getMethod("setHomePos", BlockPos.class);
                        setHomePosMethod.invoke(npc, new BlockPos((int) x, (int) y, (int) z));

                        // Set custom name directly matching avatar
                        npc.setCustomName(Text.literal(displayName));
                        npc.setCustomNameVisible(true);
                    } catch (Exception ex) {
                        LOGGER.warn("[CobbleTower] Failed to configure RCT TrainerMob properties: {}", ex.getMessage());
                    }

                    world.spawnEntity(npc);
                    LOGGER.info("[CobbleTower] Successfully spawned Trainer NPC '{}' ({}) for Floor {} at ({}, {}, {})",
                            displayName, chosenTrainerId, floor, x, y, z);
                }
            } else {
                LOGGER.warn("[CobbleTower] EntityType 'rctmod:trainer' not found in registry!");
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Error spawning Tower Trainer NPC", e);
        }
    }

    /**
     * Dynamically finds the true solid ground surface height
     */
    private int findSafeGroundY(ServerWorld world, int x, int z, int defaultY) {
        for (int y = 95; y >= 60; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos upPos = pos.up();
            BlockPos up2Pos = pos.up(2);
            if (!world.getBlockState(pos).isAir() && world.getBlockState(pos).getBlock() != Blocks.BARRIER
                    && world.getBlockState(upPos).isAir() && world.getBlockState(up2Pos).isAir()) {
                return y;
            }
        }
        return defaultY;
    }

    /**
     * Clears a safe pocket of air at the spawn foyer so player never suffocates or gets pushed up to the roof
     */
    private void clearSpawnHeadroom(ServerWorld world, int centerX, int floorY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // Ensure solid floor underneath if empty
                BlockPos floorPos = new BlockPos(centerX + dx, floorY - 1, centerZ + dz);
                if (world.getBlockState(floorPos).isAir()) {
                    world.setBlockState(floorPos, Blocks.POLISHED_DEEPSLATE.getDefaultState(), Block.NOTIFY_LISTENERS);
                }

                // Clear 3 blocks of air above the floor
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos airPos = new BlockPos(centerX + dx, floorY + dy, centerZ + dz);
                    if (!world.getBlockState(airPos).isAir() && world.getBlockState(airPos).getBlock() != Blocks.BARRIER) {
                        world.setBlockState(airPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    /**
     * Places the NBT structure safely and cleans up all command blocks, structure blocks, portals, and summoning shrines.
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

        // Purge ALL Command Blocks, Structure Blocks, Portals, and Summon Shrines across the entire arena volume
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 55; y <= 125; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    Block b = world.getBlockState(p).getBlock();
                    Identifier bId = Registries.BLOCK.getId(b);
                    String path = bId.getPath().toLowerCase();

                    boolean isRestricted = b instanceof CommandBlock || b instanceof StructureBlock
                            || path.contains("command_block") || path.contains("jigsaw")
                            || path.contains("portal") || path.contains("shrine")
                            || path.contains("altar") || path.contains("monument")
                            || path.contains("chalice") || path.contains("summon")
                            || b == Blocks.NETHER_PORTAL || b == Blocks.END_PORTAL || b == Blocks.END_GATEWAY;

                    if (isRestricted) {
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

            // Find safe ground surface Y so player never gets stuck underground
            int safeY = findSafeGroundY(overworld, targetPos.getX(), targetPos.getZ(), targetPos.getY());
            double targetY = (safeY > 0) ? (safeY + 1.0) : (targetPos.getY() + 1.0);

            player.changeGameMode(GameMode.SURVIVAL);
            player.teleport(overworld, targetPos.getX() + 0.5, targetY, targetPos.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
    }
}
