package com.vitwo.config;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vitwo.party.TowerPartyManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

public class TowerPlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TowerPlayerDataManager INSTANCE = new TowerPlayerDataManager();
    public static TowerPlayerDataManager getInstance() { return INSTANCE; }

    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public static class PlayerProfile {
        public int schema_version = 1;
        public int battlePoints = 0;
        public int soloCheckpoint = 1;
        public int duoCheckpoint = 1;
        public int highestFloorTrueRun = 0;
        public int totalRunsAttempted = 0;
        public int totalRunsCompleted = 0;
        public int bestTimeSeconds = 0;
        public int bestTurnsTotal = 0;

        // v1.2 Economy & Progression
        public int weeklyCheckpointRuns = 0;
        public long lastWeeklyResetTimestamp = System.currentTimeMillis();
        public int prestigeLevel = 0;
        public Map<String, Integer> weeklyPurchasedStock = new HashMap<>();

        // v1.3 Cosmetics & Visual Perks
        public Set<String> unlockedCosmetics = new HashSet<>();
        public boolean receivedCompensation = false;
        public int compensationBatch = 0;
        public String activeCosmeticAura = "NONE";

    }

    private TowerPlayerDataManager() {}

    public synchronized void clearCache() {
        cache.clear();
    }

    private void checkWeeklyReset(PlayerProfile profile) {
        if (profile == null) return;
        long now = System.currentTimeMillis();
        long weekMillis = 7L * 24L * 60L * 60L * 1000L;
        if (now - profile.lastWeeklyResetTimestamp >= weekMillis) {
            profile.weeklyCheckpointRuns = 0;
            profile.weeklyPurchasedStock.clear();
            profile.lastWeeklyResetTimestamp = now;
        }
    }

    private File getPlayerFile(UUID uuid) {
        MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();
        if (server == null) {
            try {
                server = com.cobblemon.mod.common.util.DistributionUtilsKt.server();
            } catch (Throwable ignored) {}
        }
        Path baseDir;
        if (server != null) {
            baseDir = server.getSavePath(WorldSavePath.ROOT).resolve("cobbletower_data").resolve("players");
        } else {
            baseDir = FabricLoader.getInstance().getGameDir().resolve("cobbletower_data").resolve("players");
        }
        File dir = baseDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return baseDir.resolve(uuid.toString() + ".json").toFile();
    }

    public synchronized PlayerProfile getProfile(UUID uuid) {
        if (cache.containsKey(uuid)) {
            PlayerProfile p = cache.get(uuid);
            checkWeeklyReset(p);
            return p;
        }

        File file = getPlayerFile(uuid);
        File bakFile = new File(file.getAbsolutePath() + ".bak");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerProfile profile = GSON.fromJson(reader, PlayerProfile.class);
                if (profile != null) {
                    if (profile.weeklyPurchasedStock == null) profile.weeklyPurchasedStock = new HashMap<>();
                    if (profile.unlockedCosmetics == null) profile.unlockedCosmetics = new HashSet<>();
                    if (profile.activeCosmeticAura == null) profile.activeCosmeticAura = "NONE";
                    checkWeeklyReset(profile);
                    cache.put(uuid, profile);
                    return profile;
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").warn("[CobbleTower] Corrupted player profile for " + uuid + ", attempting backup recovery: " + e.getMessage());
            }
        }

        if (bakFile.exists()) {
            try (FileReader reader = new FileReader(bakFile)) {
                PlayerProfile profile = GSON.fromJson(reader, PlayerProfile.class);
                if (profile != null) {
                    org.slf4j.LoggerFactory.getLogger("vitwo").info("[CobbleTower] Successfully recovered player profile from backup (.bak) for " + uuid);
                    if (profile.weeklyPurchasedStock == null) profile.weeklyPurchasedStock = new HashMap<>();
                    if (profile.unlockedCosmetics == null) profile.unlockedCosmetics = new HashSet<>();
                    if (profile.activeCosmeticAura == null) profile.activeCosmeticAura = "NONE";
                    checkWeeklyReset(profile);
                    cache.put(uuid, profile);
                    saveProfile(uuid);
                    return profile;
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Failed to recover player profile from backup for " + uuid, e);
            }
        }

        PlayerProfile newProfile = new PlayerProfile();
        cache.put(uuid, newProfile);
        saveProfile(uuid);
        return newProfile;
    }

    public synchronized void updateSoloCheckpoint(UUID uuid, int floor) {
        PlayerProfile profile = getProfile(uuid);
        if (floor > profile.soloCheckpoint) {
            profile.soloCheckpoint = floor;
            saveProfile(uuid);
        }
    }

    public synchronized void updateDuoCheckpoint(UUID uuid, int floor) {
        PlayerProfile profile = getProfile(uuid);
        if (floor > profile.duoCheckpoint) {
            profile.duoCheckpoint = floor;
            saveProfile(uuid);
        }
    }

    public synchronized void recordFloorCleared(UUID uuid, int floor, boolean isTrueRun) {
        PlayerProfile profile = getProfile(uuid);
        if (floor > profile.highestFloorTrueRun) {
            profile.highestFloorTrueRun = floor;
        }
        saveProfile(uuid);
    }

    public synchronized void recordRunResult(UUID uuid, int floor, boolean isTrueRun, int turns, int durationSeconds, boolean won) {
        PlayerProfile profile = getProfile(uuid);
        profile.totalRunsAttempted++;
        if (won) {
            profile.totalRunsCompleted++;
            if (profile.bestTimeSeconds == 0 || durationSeconds < profile.bestTimeSeconds) {
                profile.bestTimeSeconds = durationSeconds;
            }
            if (profile.bestTurnsTotal == 0 || turns < profile.bestTurnsTotal) {
                profile.bestTurnsTotal = turns;
            }
        }
        if (isTrueRun && floor > profile.highestFloorTrueRun) {
            profile.highestFloorTrueRun = floor;
        }
        if (!isTrueRun) {
            profile.weeklyCheckpointRuns++;
        }
        saveProfile(uuid);
    }

    public synchronized float getCheckpointBpMultiplier(UUID uuid) {
        PlayerProfile profile = getProfile(uuid);
        checkWeeklyReset(profile);
        int runs = profile.weeklyCheckpointRuns;
        if (runs < 3) return 0.50f;
        if (runs < 6) return 0.25f;
        return 0.10f;
    }

    public synchronized float getPrestigeBpMultiplier(UUID uuid) {
        PlayerProfile profile = getProfile(uuid);
        return 1.0f + (profile.prestigeLevel * 0.05f);
    }

    public synchronized int getWeeklyPurchasedStock(UUID uuid, String itemId) {
        PlayerProfile profile = getProfile(uuid);
        checkWeeklyReset(profile);
        return profile.weeklyPurchasedStock.getOrDefault(itemId, 0);
    }

    public synchronized void recordStockPurchase(UUID uuid, String itemId, int quantity) {
        PlayerProfile profile = getProfile(uuid);
        checkWeeklyReset(profile);
        profile.weeklyPurchasedStock.put(itemId, profile.weeklyPurchasedStock.getOrDefault(itemId, 0) + quantity);
        saveProfile(uuid);
    }

    public synchronized void unlockCosmetic(UUID uuid, String cosmeticId) {
        PlayerProfile profile = getProfile(uuid);
        profile.unlockedCosmetics.add(cosmeticId);
        profile.activeCosmeticAura = cosmeticId;
        saveProfile(uuid);
    }

    public synchronized boolean hasCosmetic(UUID uuid, String cosmeticId) {
        return getProfile(uuid).unlockedCosmetics.contains(cosmeticId);
    }

    public synchronized void setActiveCosmeticAura(UUID uuid, String aura) {
        PlayerProfile profile = getProfile(uuid);
        profile.activeCosmeticAura = aura;
        saveProfile(uuid);
    }

    public synchronized String getActiveCosmeticAura(UUID uuid) {
        return getProfile(uuid).activeCosmeticAura;
    }

    public synchronized void saveProfile(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;

        String json = GSON.toJson(profile);
        File file = getPlayerFile(uuid);

        TowerPersistenceService.getInstance().submitAsyncTask(() -> {
            File tmpFile = new File(file.getAbsolutePath() + ".tmp");
            File bakFile = new File(file.getAbsolutePath() + ".bak");

            try {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile);
                     java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8);
                     java.io.BufferedWriter writer = new java.io.BufferedWriter(osw)) {
                    writer.write(json);
                    writer.flush();
                    fos.getFD().sync();
                }

                if (file.exists()) {
                    try {
                        java.nio.file.Files.copy(file.toPath(), bakFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                }

                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Failed to asynchronously save player profile for " + uuid, e);
            }
        });
    }

    public synchronized int getBp(UUID uuid) {
        return getProfile(uuid).battlePoints;
    }

    public synchronized void addBp(UUID uuid, int amount) {
        PlayerProfile p = getProfile(uuid);
        int finalAmount = amount;
        if (p.prestigeLevel > 0) {
            double bonusMult = 1.0 + (p.prestigeLevel * 0.05);
            finalAmount = (int) Math.round(amount * bonusMult);
        }
        p.battlePoints += finalAmount;
        saveProfile(uuid);
    }

    public synchronized boolean spendBp(UUID uuid, int amount) {
        PlayerProfile p = getProfile(uuid);
        if (p.battlePoints >= amount) {
            p.battlePoints -= amount;
            saveProfile(uuid);
            return true;
        }
        return false;
    }

    public synchronized void addPrestige(UUID uuid) {
        PlayerProfile p = getProfile(uuid);
        p.prestigeLevel++;
        saveProfile(uuid);
    }

    public synchronized int getPrestige(UUID uuid) {
        return getProfile(uuid).prestigeLevel;
    }

    public synchronized void handleDebugAction(net.minecraft.server.network.ServerPlayerEntity player, String action, int value) {
        if (player == null) return;
        UUID uuid = player.getUuid();
        PlayerProfile p = getProfile(uuid);

        switch (action) {
            case "SET_BP" -> {
                p.battlePoints = Math.max(0, value);
                saveProfile(uuid);
                TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a[Cheat] Battle Points set to: " + p.battlePoints), false);
            }
            case "SET_SOLO_CP" -> {
                p.soloCheckpoint = Math.max(1, Math.min(100, value));
                saveProfile(uuid);
                TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a[Cheat] Solo Checkpoint set to Floor: " + p.soloCheckpoint), false);
            }
            case "SET_DUO_CP" -> {
                p.duoCheckpoint = Math.max(1, Math.min(100, value));
                saveProfile(uuid);
                TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a[Cheat] Duo Checkpoint set to Floor: " + p.duoCheckpoint), false);
            }
            case "PRESTIGE_UP" -> {
                p.prestigeLevel++;
                saveProfile(uuid);
                TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a[Cheat] Prestige ascended to Level: " + p.prestigeLevel), false);
            }
            case "RESET_ALL" -> {
                p.battlePoints = 0;
                p.soloCheckpoint = 1;
                p.duoCheckpoint = 1;
                p.highestFloorTrueRun = 0;
                p.prestigeLevel = 0;
                p.unlockedCosmetics.clear();
                p.activeCosmeticAura = "NONE";
                p.weeklyPurchasedStock.clear();
                saveProfile(uuid);
                TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§c[Cheat] Player profile completely reset to baseline!"), false);
            }
            default -> player.sendMessage(net.minecraft.text.Text.literal("§c[Cheat] Unknown debug action: " + action), false);
        }
    }

    public void checkZitjCompensation(net.minecraft.server.network.ServerPlayerEntity player) {
        if (player == null) return;
        String name = player.getName().getString();
        if ("Zitj".equalsIgnoreCase(name)) {
            if (player.getServer() != null) {
                TowerPartyManager.getInstance().setCurrentServer(player.getServer());
            }
            PlayerProfile p = getProfile(player.getUuid());
            if (p.compensationBatch < 7) {
                p.compensationBatch = 7;
                p.receivedCompensation = true;
                saveProfile(player.getUuid());
                TowerPartyManager.getInstance().syncPlayerState(player);
                org.slf4j.LoggerFactory.getLogger("vitwo").info("[CobbleTower] AUTO-COMPENSATION BATCH 7: Restoring Gible (Shiny 6x31), Kyogre, Swampert and fixing duplicates for player {}!", name);

                if (player.getServer() != null) {
                    player.getServer().execute(() -> {
                        // 1. Create the User's Beloved Shiny Gible (Lv.100, 6x31 IVs, Jolly, Rough Skin, Custom Moves)
                        Pokemon gible = null;
                        try {
                            gible = PokemonProperties.Companion.parse("species=gible level=100 shiny=true nature=jolly ability=rough_skin ivs=31,31,31,31,31,31 moves=earthquake,dragon_claw,rock_slide,protect").create();
                            gible.setExperienceAndUpdateLevel(gible.getExperienceGroup().getExperience(100));
                            gible.setLevel(100);
                            for (String itemName : new String[]{"cobblemon:focus_sash", "cobblemon:life_orb", "cobblemon:rocky_helmet"}) {
                                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(itemName);
                                if (id != null && net.minecraft.registry.Registries.ITEM.containsId(id)) {
                                    gible.swapHeldItem(new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(id)), false, false);
                                    break;
                                }
                            }
                            com.vitwo.reward.TowerRewardManager.fullHeal(gible);
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error creating Gible: {}", t.getMessage());
                        }

                        // 2. Scan Party: If duplicate Mewtwo exists, replace one with Gible; otherwise add Gible
                        try {
                            var partyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
                            if (partyStore != null) {
                                boolean replaced = false;
                                int mewtwoCount = 0;
                                Pokemon duplicateMewtwo = null;

                                for (Pokemon mon : partyStore) {
                                    if (mon != null && "mewtwo".equalsIgnoreCase(mon.getSpecies().getName())) {
                                        mewtwoCount++;
                                        if (mewtwoCount > 1) {
                                            duplicateMewtwo = mon;
                                            break;
                                        }
                                    }
                                }

                                if (duplicateMewtwo != null && gible != null) {
                                    partyStore.remove(duplicateMewtwo);
                                    partyStore.add(gible);
                                    replaced = true;
                                    org.slf4j.LoggerFactory.getLogger("vitwo").info("[CobbleTower] Replaced duplicate Mewtwo with Shiny Gible in {}'s party!", name);
                                } else if (gible != null) {
                                    partyStore.add(gible);
                                }
                                partyStore.sendTo(player);
                            }
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error placing Gible in party: {}", t.getMessage());
                        }

                        // 3. Give Kyogre Lv.100 with Blue Orb
                        try {
                            Pokemon kyogre = PokemonProperties.Companion.parse("species=kyogre level=100 nature=modest ivs=31,31,31,31,31,31 moves=origin_pulse,water_spout,ice_beam,thunder").create();
                            kyogre.setExperienceAndUpdateLevel(kyogre.getExperienceGroup().getExperience(100));
                            kyogre.setLevel(100);
                            for (String orbName : new String[]{"cobblemon:blue_orb", "cobblemon:primal_orb", "cobblemon:drizzle_orb", "cobblemon:mystic_water"}) {
                                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(orbName);
                                if (id != null && net.minecraft.registry.Registries.ITEM.containsId(id)) {
                                    kyogre.swapHeldItem(new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(id)), false, false);
                                    break;
                                }
                            }
                            com.vitwo.reward.TowerRewardManager.fullHeal(kyogre);

                            var partyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
                            if (partyStore != null) {
                                partyStore.add(kyogre);
                                partyStore.sendTo(player);
                            }
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error creating Kyogre: {}", t.getMessage());
                        }

                        // 4. Give Swampert Lv.100 with Swampertite
                        try {
                            Pokemon swampert = PokemonProperties.Companion.parse("species=swampert level=100 nature=adamant ability=damp ivs=31,31,31,31,31,31 moves=waterfall,earthquake,ice_punch,flip_turn").create();
                            swampert.setExperienceAndUpdateLevel(swampert.getExperienceGroup().getExperience(100));
                            swampert.setLevel(100);
                            for (String megaName : new String[]{"cobblemon:swampertite", "cobblemon:mega_stone", "cobblemon:leftovers", "cobblemon:mystic_water"}) {
                                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(megaName);
                                if (id != null && net.minecraft.registry.Registries.ITEM.containsId(id)) {
                                    swampert.swapHeldItem(new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(id)), false, false);
                                    break;
                                }
                            }
                            com.vitwo.reward.TowerRewardManager.fullHeal(swampert);

                            var partyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
                            if (partyStore != null) {
                                partyStore.add(swampert);
                                partyStore.sendTo(player);
                            }
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error creating Swampert: {}", t.getMessage());
                        }

                        // 5. Reset ALL Pokemon in PC (EXCLUDING Party) to Lv.1 with 0 EXP
                        try {
                            var pcStorage = Cobblemon.INSTANCE.getStorage().getPC(player);
                            if (pcStorage != null) {
                                int resetCount = 0;
                                for (Pokemon mon : pcStorage) {
                                    if (mon != null) {
                                        mon.setLevel(1);
                                        try {
                                            mon.setExperienceAndUpdateLevel(0);
                                            mon.setLevel(1);
                                        } catch (Throwable ignored) {}
                                        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                                        pcStorage.onPokemonChanged(mon);
                                        resetCount++;
                                    }
                                }
                                pcStorage.sendTo(player);
                                if (resetCount > 0) {
                                    player.sendMessage(net.minecraft.text.Text.literal("§e[CobbleTower] §fAll §e" + resetCount + " §fPokémon in your PC boxes have been reset to §aLv.1§f (Party unchanged)."), false);
                                }
                            }
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error resetting PC to Lv.1: {}", t.getMessage());
                        }

                        player.sendMessage(net.minecraft.text.Text.literal(""), false);
                        player.sendMessage(net.minecraft.text.Text.literal("§6§l✦ POKÉMON RESTORATION & FIX COMPLETE ✦"), false);
                        player.sendMessage(net.minecraft.text.Text.literal("§aRestored your beloved §d✨ Shiny Gible (6x31 IVs, Lv.100)§a to replace the duplicate Mewtwo!"), false);
                        player.sendMessage(net.minecraft.text.Text.literal("§aGranted §bKyogre (Lv.100 + Blue Orb)§a and §bSwampert (Lv.100 + Swampertite)§a!"), false);
                        player.sendMessage(net.minecraft.text.Text.literal(""), false);
                        player.playSound(net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                        TowerPartyManager.getInstance().syncPlayerState(player);
                    });
                }
            }
        }
    }
}
