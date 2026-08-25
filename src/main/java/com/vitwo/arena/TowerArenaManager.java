package com.vitwo.arena;

import com.vitwo.battle.TrainerPool;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TowerArenaManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-ArenaManager");
    private static final TowerArenaManager INSTANCE = new TowerArenaManager();
    public static TowerArenaManager getInstance() { return INSTANCE; }

    public static final RegistryKey<World> TOWER_DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("vitwo", "tower_dimension")
    );

    // Arena Pooling Variables
    private static final Set<Integer> activeSlots = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> builtSlots = ConcurrentHashMap.newKeySet();

    private TowerArenaManager() {}

    public static int getSlotSpacing() {
        return com.vitwo.config.TowerConfig.getInstance().arena.sectorSpacing;
    }

    public static int getGroundY() {
        return com.vitwo.config.TowerConfig.getInstance().arena.groundY;
    }

    public static int getBattlefieldHeightOffset() {
        return com.vitwo.config.TowerConfig.getInstance().arena.battlefieldHeightOffset;
    }

    public static int allocateSlot() {
        int maxSlots = com.vitwo.config.TowerConfig.getInstance().arena.maxArenaSlots;
        for (int i = 0; i < maxSlots; i++) {
            if (!activeSlots.contains(i)) {
                activeSlots.add(i);
                return i;
            }
        }
        return -1; // Capacity full
    }

    public static void freeSlot(int slot) {
        if (slot >= 0) {
            activeSlots.remove(slot);
        }
    }

    public static int getSectorX(int slot) {
        return (slot + 1) * getSlotSpacing();
    }

    public static int getSectorZ(int slot) {
        return 0; // All slots arranged in a line along the X axis
    }

    /**
     * Instantly places the unified tower arena structure and teleports the party inside with exact coordinates.
     */
    public void teleportPartyToArena(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member) {
        if (leader == null || leader.getServer() == null) return;
        MinecraftServer server = leader.getServer();

        ServerWorld arenaWorld = server.getWorld(TOWER_DIMENSION_KEY);
        if (arenaWorld == null) {
            arenaWorld = leader.getServerWorld();
        }

        int slot = party.getArenaSlot();
        if (slot < 0) {
            slot = allocateSlot();
            if (slot < 0) {
                leader.sendMessage(Text.literal("§c[CobbleTower] Server arena capacity is full (64/64)! Please wait for an arena to free up."), false);
                if (member != null) {
                    member.sendMessage(Text.literal("§c[CobbleTower] Server arena capacity is full (64/64)! Please wait for an arena to free up."), false);
                }
                return;
            }
            party.setArenaSlot(slot);
        }

        int floor = party.getCurrentFloor();
        int sectorX = getSectorX(slot);
        int sectorZ = getSectorZ(slot);

        int centerX = sectorX + 20;
        int centerZ = 5;
        double battlefieldY = 83.0;

        // Build structure once per arena slot
        if (!builtSlots.contains(slot)) {
            placeSingleArenaStructure(arenaWorld, centerX, getGroundY(), centerZ);
            builtSlots.add(slot);
        }

        // Clear any old entities in this sector
        clearOldNpcsInSector(arenaWorld, sectorX, sectorZ);

        // General Coordinate System matching tower_arena.nbt:
        // Structure size: 157x77x135 placed at origin (sectorX - 58, 64, -62)
        // Pokéball arena platform surface is at Y = 83.0, Z = -6.0, Center X = sectorX + 24.0
        // Player standing pad (White half) at X = sectorX + 18.0, Y = 83.0, Z = -6.0, facing +X (Yaw = -90.0)
        // NPC trainer standing pad (Red half) at X = sectorX + 30.0, Y = 83.0, Z = -6.0, facing -X (Yaw = 90.0)
        double playerX = sectorX + 18.0;
        double playerY = battlefieldY;
        double playerZ = -6.0;
        float playerYaw = -90.0f;

        double trainerX = sectorX + 30.0;
        double trainerY = battlefieldY;
        double trainerZ = -6.0;
        float trainerYaw = 90.0f;

        // Teleport players instantly
        if (party.isSolo() || member == null) {
            leader.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, playerX, playerY, playerZ, playerYaw, 0.0f);
        } else {
            leader.changeGameMode(GameMode.ADVENTURE);
            member.changeGameMode(GameMode.ADVENTURE);
            leader.teleport(arenaWorld, playerX, playerY, playerZ - 2.0, playerYaw, 0.0f);
            member.teleport(arenaWorld, playerX, playerY, playerZ + 2.0, playerYaw, 0.0f);
        }

        // Spawn Single Deterministic Trainer NPC for all floors 1-100
        spawnTowerTrainerNpc(arenaWorld, party, floor, trainerX, trainerY, trainerZ, trainerYaw);
    }

    /**
     * Loads and places the unified tower_arena structure template
     */
    private StructureTemplate loadStructureTemplate(ServerWorld world, String name) {
        StructureTemplateManager templateManager = world.getStructureTemplateManager();

        // 1. Try template manager lookup
        Optional<StructureTemplate> templateOpt = templateManager.getTemplate(Identifier.of("vitwo", name));
        if (templateOpt.isPresent()) return templateOpt.get();

        templateOpt = templateManager.getTemplate(Identifier.of("vitwo", "structure/" + name));
        if (templateOpt.isPresent()) return templateOpt.get();

        templateOpt = templateManager.getTemplate(Identifier.of("vitwo", "structures/" + name));
        if (templateOpt.isPresent()) return templateOpt.get();

        // 2. Direct ClassLoader JAR resource fallback
        String[] candidatePaths = {
                "/data/vitwo/structure/" + name + ".nbt",
                "/data/vitwo/structures/" + name + ".nbt"
        };
        for (String path : candidatePaths) {
            try (java.io.InputStream is = TowerArenaManager.class.getResourceAsStream(path)) {
                if (is != null) {
                    net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.NbtIo.readCompressed(is, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                    StructureTemplate template = new StructureTemplate();
                    template.readNbt(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt);
                    LOGGER.info("[CobbleTower] Successfully loaded structure '{}' from JAR resource: {}", name, path);
                    return template;
                }
            } catch (Exception e) {
                LOGGER.warn("[CobbleTower] Failed loading structure from resource {}: {}", path, e.getMessage());
            }
        }
        return null;
    }

    private void placeSingleArenaStructure(ServerWorld world, int centerX, int groundY, int centerZ) {
        try {
            StructureTemplate template = loadStructureTemplate(world, "tower_arena");
            if (template != null) {
                Vec3i size = template.getSize();
                StructurePlacementData placementSettings = new StructurePlacementData()
                        .setIgnoreEntities(true)
                        .setUpdateNeighbors(false);

                int originX = centerX - (size.getX() / 2);
                int originZ = centerZ - (size.getZ() / 2);
                BlockPos originPos = new BlockPos(originX, groundY, originZ);

                template.place(world, originPos, originPos, placementSettings, world.getRandom(), Block.NOTIFY_LISTENERS);
                LOGGER.info("[CobbleTower] Successfully placed unified structure for slot at ({}, {}, {})", originX, groundY, originZ);
            } else {
                LOGGER.warn("[CobbleTower] Structure template 'tower_arena' not found! Building fallback platform.");
                buildFallbackPlatform(world, centerX, groundY, centerZ);
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to place unified structure template", e);
            buildFallbackPlatform(world, centerX, groundY, centerZ);
        }
    }

    private void buildFallbackPlatform(ServerWorld world, int centerX, int groundY, int centerZ) {
        int radius = 14;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.setBlockState(new BlockPos(centerX + dx, groundY - 1, centerZ + dz), Blocks.SMOOTH_QUARTZ.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    public static int findTopWalkableY(ServerWorld world, double x, double z, int startY, int maxSearchY) {
        int checkX = (int) Math.floor(x);
        int checkZ = (int) Math.floor(z);
        for (int y = maxSearchY; y >= startY; y--) {
            BlockPos pos = new BlockPos(checkX, y, checkZ);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getBlock() != Blocks.BARRIER && state.getFluidState().isEmpty()) {
                BlockPos above1 = pos.up();
                BlockPos above2 = pos.up(2);
                boolean spaceAbove = (world.getBlockState(above1).isAir() || world.getBlockState(above1).getBlock() == Blocks.BARRIER)
                        && (world.getBlockState(above2).isAir() || world.getBlockState(above2).getBlock() == Blocks.BARRIER);
                if (spaceAbove) {
                    return y + 1;
                }
            }
        }
        return startY;
    }

    public void purgeSectorAndBlocks(ServerWorld world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        try {
            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
            List<Entity> entities = world.getOtherEntities(null, box);
            for (Entity e : entities) {
                if (!(e instanceof ServerPlayerEntity)) {
                    e.discard();
                }
            }

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!world.getBlockState(p).isAir()) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void clearOldNpcsInSector(ServerWorld world, int sectorX, int sectorZ) {
        try {
            Box box = new Box(sectorX - 80, 40, sectorZ - 80, sectorX + 180, 150, sectorZ + 180);
            List<Entity> entities = world.getOtherEntities(null, box);
            for (Entity e : entities) {
                if (e instanceof ServerPlayerEntity) continue;
                e.discard();
            }
        } catch (Exception ignored) {}
    }

    private void spawnTowerTrainerNpc(ServerWorld world, TowerParty party, int floor, double x, double y, double z, float yaw) {
        try {
            world.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
            EntityType<?> trainerType = Registries.ENTITY_TYPE.get(Identifier.of("rctmod", "trainer"));
            if (trainerType != null) {
                Entity npc = trainerType.create(world);
                if (npc != null) {
                    npc.refreshPositionAndAngles(x, y, z, yaw, 0.0f);
                    String chosenTrainerId = party != null ? party.getCurrentTrainerId() : TrainerPool.getRctTrainerIdForFloor(floor);
                    String displayName = party != null ? party.getCurrentBossName() : TrainerPool.getTrainerDisplayName(floor, chosenTrainerId);

                    try {
                        Method setTrainerIdMethod = npc.getClass().getMethod("setTrainerId", String.class);
                        setTrainerIdMethod.invoke(npc, chosenTrainerId);

                        Method setPersistentMethod = npc.getClass().getMethod("setPersistent", boolean.class);
                        setPersistentMethod.invoke(npc, true);

                        Method setHomePosMethod = npc.getClass().getMethod("setHomePos", BlockPos.class);
                        setHomePosMethod.invoke(npc, new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));

                        npc.setCustomName(Text.literal(displayName));
                        if (npc instanceof net.minecraft.entity.mob.MobEntity mob) {
                            mob.setPersistent();
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("[CobbleTower] Failed to configure RCT TrainerMob properties: {}", ex.getMessage());
                    }

                    world.spawnEntity(npc);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Error spawning Tower Trainer NPC", e);
        }
    }

    public void cleanupFloorArena(TowerParty party, MinecraftServer server) {
        if (party == null || server == null) return;
        ServerWorld arenaWorld = server.getWorld(TOWER_DIMENSION_KEY);
        if (arenaWorld == null) return;

        int slot = party.getArenaSlot();
        if (slot >= 0) {
            int sectorX = getSectorX(slot);
            int sectorZ = getSectorZ(slot);
            clearOldNpcsInSector(arenaWorld, sectorX, sectorZ);
            freeSlot(slot);
            party.setArenaSlot(-1);
        }
    }

    public void returnPlayerToOriginalPos(ServerPlayerEntity player, BlockPos fallbackPos) {
        if (player == null || player.getServer() == null) return;
        MinecraftServer server = player.getServer();

        Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
        String targetDim = "minecraft:overworld";
        double targetX = 0, targetY = 64, targetZ = 0;
        float targetYaw = 0, targetPitch = 0;
        boolean hasExact = false;

        if (partyOpt.isPresent()) {
            TowerParty party = partyOpt.get();
            if (player.getUuid().equals(party.getLeaderId())) {
                targetDim = party.getOriginalLeaderDim();
                targetX = party.getOriginalLeaderX();
                targetY = party.getOriginalLeaderY();
                targetZ = party.getOriginalLeaderZ();
                targetYaw = party.getOriginalLeaderYaw();
                targetPitch = party.getOriginalLeaderPitch();
                hasExact = (targetY > -60);
            } else if (party.getMemberId() != null && player.getUuid().equals(party.getMemberId())) {
                targetDim = party.getOriginalMemberDim();
                targetX = party.getOriginalMemberX();
                targetY = party.getOriginalMemberY();
                targetZ = party.getOriginalMemberZ();
                targetYaw = party.getOriginalMemberYaw();
                targetPitch = party.getOriginalMemberPitch();
                hasExact = (targetY > -60);
            }
        }

        if (!hasExact && fallbackPos != null) {
            targetX = fallbackPos.getX() + 0.5;
            targetY = fallbackPos.getY();
            targetZ = fallbackPos.getZ() + 0.5;
            hasExact = true;
        }

        ServerWorld targetWorld = null;
        if (targetDim != null) {
            Identifier dimId = Identifier.tryParse(targetDim);
            if (dimId != null) {
                targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
            }
        }
        if (targetWorld == null) {
            targetWorld = server.getOverworld();
        }

        if (!hasExact) {
            BlockPos spawn = targetWorld.getSpawnPos();
            targetX = spawn.getX() + 0.5;
            targetY = spawn.getY();
            targetZ = spawn.getZ() + 0.5;
        }

        BlockPos targetPos = new BlockPos((int) Math.floor(targetX), (int) Math.floor(targetY), (int) Math.floor(targetZ));
        BlockPos safePos = findSafeTeleportTarget(targetWorld, targetPos);

        player.changeGameMode(GameMode.SURVIVAL);
        player.teleport(targetWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, targetYaw, targetPitch);
    }

    public static BlockPos findSafeTeleportTarget(ServerWorld world, BlockPos original) {
        int x = original.getX();
        int z = original.getZ();
        int maxY = world.getTopY();
        int minY = world.getBottomY();

        // 1. Scan downwards from original Y down to minY + 1
        int startY = Math.min(original.getY(), maxY - 2);
        for (int y = startY; y >= minY + 1; y--) {
            BlockPos feetPos = new BlockPos(x, y, z);
            if (isSafeLocation(world, feetPos)) {
                return feetPos;
            }
        }

        // 2. Scan upwards from original Y up to maxY - 2
        for (int y = Math.max(original.getY(), minY + 1); y <= maxY - 2; y++) {
            BlockPos feetPos = new BlockPos(x, y, z);
            if (isSafeLocation(world, feetPos)) {
                return feetPos;
            }
        }

        // 3. Fallback: world spawn point
        return world.getSpawnPos();
    }

    private static boolean isSafeLocation(ServerWorld world, BlockPos feetPos) {
        BlockPos headPos = feetPos.up();
        BlockPos groundPos = feetPos.down();

        BlockState feetState = world.getBlockState(feetPos);
        BlockState headState = world.getBlockState(headPos);
        BlockState groundState = world.getBlockState(groundPos);

        boolean feetAir = feetState.getCollisionShape(world, feetPos).isEmpty();
        boolean headAir = headState.getCollisionShape(world, headPos).isEmpty();
        boolean groundSolid = !groundState.isAir() && !groundState.getCollisionShape(world, groundPos).isEmpty();

        return feetAir && headAir && groundSolid;
    }
}
