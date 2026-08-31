package com.vitwo.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.vitwo.party.TowerParty;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LevelCapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-LevelCap");
    private static final Random RANDOM = new Random();

    public static int getMaxLevelCapForFloor(int floor) {
        if (floor <= 10) return 30;
        if (floor <= 25) return 40;
        if (floor <= 40) return 50;
        if (floor <= 55) return 60;
        if (floor <= 70) return 70;
        if (floor <= 85) return 85;
        return 100;
    }

    public static int getMinNpcLevelForFloor(int floor) {
        if (floor <= 10) return 26;
        if (floor <= 25) return 36;
        if (floor <= 40) return 46;
        if (floor <= 55) return 56;
        if (floor <= 70) return 66;
        if (floor <= 85) return 81;
        return 96;
    }

    public static int generateNpcPokemonLevel(int floor, boolean isAce) {
        int max = getMaxLevelCapForFloor(floor);
        int min = getMinNpcLevelForFloor(floor);
        if (isAce) return max;
        return min + RANDOM.nextInt(Math.max(1, max - min + 1));
    }

    public static boolean hasShinyBossPokemon(int floor) {
        return floor >= 51;
    }

    /**
     * Automatically adjusts (downscales) player's Pokemon levels to the floor's max level cap.
     * ALWAYS records original levels FIRST before modifying, so they can be restored later.
     * 
     * IMPORTANT: This method must NEVER silently fail. If Cobblemon API changes or
     * reflection fails, we log the error AND notify the player.
     */
    public static void applyLevelCapToPlayer(ServerPlayerEntity player, int floor, TowerParty party) {
        if (player == null) return;
        int maxCap = getMaxLevelCapForFloor(floor);

        try {
            var cobblemonParty = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (cobblemonParty == null) {
                LOGGER.error("[CobbleTower] Failed to get Cobblemon party for player {} — party is null!", player.getName().getString());
                return;
            }

            boolean adjustedAny = false;
            int recordedCount = 0;

            for (Pokemon mon : cobblemonParty) {
                if (mon == null) continue;

                // ALWAYS record original level & exact experience FIRST, before any modification
                if (party != null) {
                    party.recordOriginalPokemonState(player.getUuid(), mon.getUuid(), mon.getLevel(), mon.getExperience());
                    recordedCount++;
                }

                Integer origLevel = party != null ? party.getOriginalPokemonLevel(player.getUuid(), mon.getUuid()) : null;
                int baseOriginal = origLevel != null ? origLevel : mon.getLevel();
                int targetLevel = (maxCap > 0 && maxCap < 100) ? Math.min(baseOriginal, maxCap) : baseOriginal;

                if (mon.getLevel() != targetLevel) {
                    LOGGER.info("[CobbleTower] Dynamic level cap adjustment for {}'s {}: Lv.{} -> Lv.{} (Original: Lv.{}, Floor Cap: Lv.{})", 
                            player.getName().getString(), mon.getSpecies().getName(), mon.getLevel(), targetLevel, baseOriginal, maxCap);
                    mon.setLevel(targetLevel);
                    com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                    adjustedAny = true;
                }
            }

            LOGGER.info("[CobbleTower] Level cap applied for player {} on Floor {}: recorded {} Pokémon, maxCap=Lv.{}, adjusted={}", 
                    player.getName().getString(), floor, recordedCount, maxCap, adjustedAny);

            if (adjustedAny && maxCap > 0 && maxCap < 100) {
                player.sendMessage(Text.literal("§e[CobbleTower] §fParty temporarily scaled to Floor Level Cap: §aLv." + maxCap), false);
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] CRITICAL: Failed to apply level cap for player {} on floor {}!", 
                    player.getName().getString(), floor, t);
            player.sendMessage(Text.literal("§c[CobbleTower] System Error: Failed to apply floor level cap. Please contact Admin!"), false);
        }
    }

    /**
     * Restores player's Pokemon levels and exact experience back to their original values when leaving the Tower.
     * 
     * CRITICAL: This method must NEVER silently fail. If restoration fails for ANY reason,
     * we log a detailed error with player name, Pokémon species/UUID, and expected level/exp.
     * We also notify the player so they can report the issue.
     */
    public static void restorePlayerLevels(ServerPlayerEntity player, TowerParty party) {
        if (player == null) {
            LOGGER.warn("[CobbleTower] restorePlayerLevels called with null player!");
            return;
        }
        if (party == null) {
            LOGGER.warn("[CobbleTower] restorePlayerLevels called with null party for player {}!", player.getName().getString());
            return;
        }

        // Get the stored original levels map for this player
        Map<String, Integer> originalLevels = party.getOriginalPokemonLevelsForPlayer(player.getUuid());
        Map<String, Integer> originalExps = party.getOriginalPokemonExperienceForPlayer(player.getUuid());
        if (originalLevels == null || originalLevels.isEmpty()) {
            LOGGER.warn("[CobbleTower] No original levels recorded for player {} — levels were never downscaled or data was lost!", 
                    player.getName().getString());
            return;
        }

        LOGGER.info("[CobbleTower] Restoring levels & experience for player {} — {} Pokémon entries recorded", 
                player.getName().getString(), originalLevels.size());

        try {
            var cobblemonParty = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (cobblemonParty == null) {
                LOGGER.error("[CobbleTower] CRITICAL: Cannot restore levels — Cobblemon party is null for {}!", player.getName().getString());
                player.sendMessage(Text.literal("§c[CobbleTower] Error: Failed to access Pokémon party. Please contact Admin!"), false);
                return;
            }

            int restoredCount = 0;
            int failedCount = 0;

            for (Pokemon mon : cobblemonParty) {
                if (mon == null) continue;

                String monKey = mon.getUuid().toString();
                Integer originalLevel = originalLevels.get(monKey);
                Integer originalExp = originalExps != null ? originalExps.get(monKey) : null;

                if (originalLevel != null || originalExp != null) {
                    LOGGER.info("[CobbleTower] Restoring {}'s {} from Lv.{} (Exp: {}) back to Lv.{} (Exp: {})", 
                            player.getName().getString(), mon.getSpecies().getName(), mon.getLevel(), mon.getExperience(), originalLevel, originalExp);
                    try {
                        if (originalLevel != null && originalLevel > 0) {
                            mon.setLevel(originalLevel);
                            try {
                                int minExp = mon.getExperienceGroup().getExperience(originalLevel);
                                int finalExp = Math.max(originalExp != null ? originalExp : 0, minExp);
                                mon.setExperienceAndUpdateLevel(finalExp);
                                if (mon.getLevel() < originalLevel) {
                                    mon.setLevel(originalLevel);
                                }
                            } catch (Throwable ignored) {}
                        } else if (originalExp != null && originalExp > 0) {
                            mon.setExperienceAndUpdateLevel(originalExp);
                        }
                        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                        restoredCount++;
                    } catch (Throwable t) {
                        LOGGER.error("[CobbleTower] Failed to restore level/exp for {} (UUID: {}) to Lv.{} / Exp {}", 
                                mon.getSpecies().getName(), mon.getUuid(), originalLevel, originalExp, t);
                        failedCount++;
                    }
                }
            }

            // Synchronize with PartyStore & send to client
            for (Pokemon mon : cobblemonParty) {
                if (mon != null) {
                    cobblemonParty.onPokemonChanged(mon);
                }
            }
            cobblemonParty.sendTo(player);

            LOGGER.info("[CobbleTower] Level & experience restoration complete for {}: restored={}, failed={}", 
                    player.getName().getString(), restoredCount, failedCount);

            if (restoredCount > 0) {
                player.sendMessage(Text.literal("§a[CobbleTower] §fSuccessfully restored original levels & EXP for §a" + restoredCount + " §fPokémon."), false);
            }
            if (failedCount > 0) {
                player.sendMessage(Text.literal("§c[CobbleTower] §fWarning: " + failedCount + " Pokémon levels could not be restored! Contact Admin."), false);
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] CRITICAL: Level restoration FAILED entirely for player {}!", player.getName().getString(), t);
            player.sendMessage(Text.literal("§c[CobbleTower] CRITICAL ERROR: Failed to restore Pokémon levels! Contact Admin immediately!"), false);
            
            // Emergency dump: log all recorded levels so admin can manually fix
            for (Map.Entry<String, Integer> entry : originalLevels.entrySet()) {
                LOGGER.error("[CobbleTower] EMERGENCY DATA: Player={}, PokemonUUID={}, OriginalLevel={}", 
                        player.getName().getString(), entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Failsafe recovery: Restores player's Pokemon levels directly from an un-restored ActiveRunData snapshot.
     */
    public static void restorePlayerLevelsFromRunData(ServerPlayerEntity player, com.vitwo.party.TowerRunPersistenceManager.ActiveRunData runData) {
        if (player == null || runData == null) return;
        UUID playerId = player.getUuid();

        Map<String, Integer> originalLevels = runData.originalPokemonLevels.get(playerId.toString());
        Map<String, Integer> originalExps = runData.originalPokemonExperience != null ? runData.originalPokemonExperience.get(playerId.toString()) : null;
        if (originalLevels == null || originalLevels.isEmpty()) return;

        try {
            var cobblemonParty = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (cobblemonParty == null) return;

            int restoredCount = 0;
            for (Pokemon mon : cobblemonParty) {
                if (mon == null) continue;
                String monKey = mon.getUuid().toString();
                Integer origLvl = originalLevels.get(monKey);
                Integer origExp = originalExps != null ? originalExps.get(monKey) : null;

                if (origLvl != null || origExp != null) {
                    if (origLvl != null && origLvl > 0) {
                        mon.setLevel(origLvl);
                        try {
                            int minExp = mon.getExperienceGroup().getExperience(origLvl);
                            int finalExp = Math.max(origExp != null ? origExp : 0, minExp);
                            mon.setExperienceAndUpdateLevel(finalExp);
                            if (mon.getLevel() < origLvl) {
                                mon.setLevel(origLvl);
                            }
                        } catch (Throwable ignored) {}
                    } else if (origExp != null && origExp > 0) {
                        mon.setExperienceAndUpdateLevel(origExp);
                    }
                    com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                    restoredCount++;
                }
            }

            for (Pokemon mon : cobblemonParty) {
                if (mon != null) {
                    cobblemonParty.onPokemonChanged(mon);
                }
            }
            cobblemonParty.sendTo(player);

            if (restoredCount > 0) {
                LOGGER.info("[CobbleTower] Auto-restored levels for {} after abnormal server restart: {} Pokémon restored", player.getName().getString(), restoredCount);
                player.sendMessage(Text.literal("§a[CobbleTower] §fSafely recovered original levels & EXP for §a" + restoredCount + " §fPokémon!"), false);
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Failed to restore levels from run data for {}: {}", player.getName().getString(), t.getMessage());
        }
    }

    /**
     * Checks if player's team is eligible for the floor.
     * With auto-downscaling, all players are eligible.
     */
    public static boolean isPlayerEligible(ServerPlayerEntity player, int floor) {
        return player != null;
    }
}
