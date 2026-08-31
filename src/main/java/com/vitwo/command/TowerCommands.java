package com.vitwo.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.vitwo.config.TowerConfig;
import com.vitwo.config.TowerLeaderboardManager;
import com.vitwo.config.TowerLeaderboardManager.LeaderboardEntry;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import com.vitwo.party.TowerRunPersistenceManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class TowerCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tower")
                // /tower start or /tower solo (True Run Floor 1)
                .then(CommandManager.literal("start")
                        .executes(ctx -> startSolo(ctx.getSource(), 1)))
                .then(CommandManager.literal("solo")
                        .executes(ctx -> startSolo(ctx.getSource(), 1)))

                // /tower checkpoint <floor>
                .then(CommandManager.literal("checkpoint")
                        .then(CommandManager.argument("floor", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    return startSolo(ctx.getSource(), floor);
                                })))

                // /tower duo <player>
                .then(CommandManager.literal("duo")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity sender = ctx.getSource().getPlayerOrThrow();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                    TowerPartyManager.getInstance().invitePlayer(sender, target);
                                    return 1;
                                }))
                        .then(CommandManager.literal("accept")
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    TowerPartyManager.getInstance().acceptInvite(player);
                                    return 1;
                                }))
                        .then(CommandManager.literal("decline")
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    TowerPartyManager.getInstance().respondInvite(player, false);
                                    return 1;
                                })))

                // /tower pause
                .then(CommandManager.literal("pause")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                            if (partyOpt.isEmpty()) {
                                player.sendMessage(Text.literal("§c[CobbleTower] You are not currently in an active Tower run."), false);
                                return 0;
                            }
                            TowerRunPersistenceManager.getInstance().saveRun(partyOpt.get());
                            player.sendMessage(Text.literal("§e[CobbleTower] Run state safely saved and paused! Use §b/tower resume §eto continue."), false);
                            return 1;
                        }))

                // /tower resume
                .then(CommandManager.literal("resume")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            boolean restored = TowerRunPersistenceManager.getInstance().restoreRun(player, player.getServer());
                            if (!restored) {
                                player.sendMessage(Text.literal("§c[CobbleTower] No paused run save found for your profile."), false);
                            }
                            return restored ? 1 : 0;
                        }))

                // /tower forfeit
                .then(CommandManager.literal("forfeit")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            TowerPartyManager.getInstance().handleForfeitVote(player);
                            return 1;
                        }))
                // /tower admin addbp <player> <amount>
                .then(CommandManager.literal("admin")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("addbp")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    TowerPlayerDataManager.getInstance().addBp(target.getUuid(), amount);
                                                    TowerPartyManager.getInstance().syncPlayerState(target);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("§aAdded " + amount + " BP to " + target.getName().getString() + " (Total: " + TowerPlayerDataManager.getInstance().getBp(target.getUuid()) + " BP)."), false);
                                                    target.sendMessage(Text.literal("§6[CobbleTower] §aYou received " + amount + " BP from an Admin!"), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(CommandManager.literal("setbp")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    TowerPlayerDataManager.PlayerProfile prof = TowerPlayerDataManager.getInstance().getProfile(target.getUuid());
                                                    prof.battlePoints = amount;
                                                    TowerPlayerDataManager.getInstance().saveProfile(target.getUuid());
                                                    TowerPartyManager.getInstance().syncPlayerState(target);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("§aSet BP of " + target.getName().getString() + " to " + amount + " BP."), false);
                                                    target.sendMessage(Text.literal("§6[CobbleTower] §aYour BP was set to " + amount + " BP by an Admin!"), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(CommandManager.literal("setlevel")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                                    var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(target);
                                                    int count = 0;
                                                    if (party != null) {
                                                        for (com.cobblemon.mod.common.pokemon.Pokemon mon : party) {
                                                            if (mon != null) {
                                                                mon.setLevel(lvl);
                                                                try {
                                                                    int targetExp = mon.getExperienceGroup().getExperience(lvl);
                                                                    mon.setExperienceAndUpdateLevel(targetExp);
                                                                    if (mon.getLevel() < lvl) mon.setLevel(lvl);
                                                                } catch (Throwable ignored) {}
                                                                com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                                                                party.onPokemonChanged(mon);
                                                                count++;
                                                            }
                                                        }
                                                        party.sendTo(target);
                                                    }
                                                    final int finalCount = count;
                                                    ctx.getSource().sendFeedback(() -> Text.literal("§aSuccessfully set all " + finalCount + " Pokémon of " + target.getName().getString() + " to Lv." + lvl + "!"), false);
                                                    target.sendMessage(Text.literal("§6[CobbleTower] §aYour entire party has been set by Admin to §eLv." + lvl + "§a (Full EXP & PP)!"), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(CommandManager.literal("recover")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            int partyRestored = 0;
                                            var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(target);
                                            if (party != null) {
                                                for (com.cobblemon.mod.common.pokemon.Pokemon mon : party) {
                                                    if (mon != null && mon.getLevel() < 100) {
                                                        mon.setLevel(100);
                                                        try {
                                                            int targetExp = mon.getExperienceGroup().getExperience(100);
                                                            mon.setExperienceAndUpdateLevel(targetExp);
                                                            if (mon.getLevel() < 100) mon.setLevel(100);
                                                        } catch (Throwable ignored) {}
                                                        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                                                        party.onPokemonChanged(mon);
                                                        partyRestored++;
                                                    }
                                                }
                                                party.sendTo(target);
                                            }
                                            final int pCount = partyRestored;
                                            ctx.getSource().sendFeedback(() -> Text.literal("§aSuccessfully restored " + pCount + " Party Pokémon of " + target.getName().getString() + " to Lv.100!"), false);
                                            target.sendMessage(Text.literal("§6[CobbleTower] §aRestored §e" + pCount + " §aParty Pokémon back to §6Lv.100 (Max EXP & Full PP)§a!"), false);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("fixlevels")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(target);
                                            int count = 0;
                                            if (party != null) {
                                                for (com.cobblemon.mod.common.pokemon.Pokemon mon : party) {
                                                    if (mon != null && mon.getLevel() < 100) {
                                                        mon.setLevel(100);
                                                        try {
                                                            int targetExp = mon.getExperienceGroup().getExperience(100);
                                                            mon.setExperienceAndUpdateLevel(targetExp);
                                                            if (mon.getLevel() < 100) mon.setLevel(100);
                                                        } catch (Throwable ignored) {}
                                                        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                                                        party.onPokemonChanged(mon);
                                                        count++;
                                                    }
                                                }
                                                party.sendTo(target);
                                            }
                                            final int finalCount = count;
                                            ctx.getSource().sendFeedback(() -> Text.literal("§aRestored " + finalCount + " Pokémon of " + target.getName().getString() + " to Lv.100!"), false);
                                            target.sendMessage(Text.literal("§6[CobbleTower] §aAutomatically restored " + finalCount + " Pokémon to §6Lv.100§a!"), false);
                                            return 1;
                                        })
                                )
                        )
                )

                // /tower recover (for self)
                .then(CommandManager.literal("recover")
                        .executes(ctx -> {
                            ServerPlayerEntity target = ctx.getSource().getPlayerOrThrow();
                            int partyRestored = 0;
                            var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(target);
                            if (party != null) {
                                for (com.cobblemon.mod.common.pokemon.Pokemon mon : party) {
                                    if (mon != null && mon.getLevel() < 100) {
                                        mon.setLevel(100);
                                        try {
                                            int targetExp = mon.getExperienceGroup().getExperience(100);
                                            mon.setExperienceAndUpdateLevel(targetExp);
                                            if (mon.getLevel() < 100) mon.setLevel(100);
                                        } catch (Throwable ignored) {}
                                        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                                        party.onPokemonChanged(mon);
                                        partyRestored++;
                                    }
                                }
                                party.sendTo(target);
                            }
                            final int pCount = partyRestored;
                            target.sendMessage(Text.literal("§6[CobbleTower] §aParty recovery complete! Restored §e" + pCount + " §aParty Pokémon back to §6Lv.100 (Max EXP & Full PP)§a!"), false);
                            return 1;
                        }))


                // /tower leaderboard
                .then(CommandManager.literal("leaderboard")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            List<LeaderboardEntry> top = TowerLeaderboardManager.getInstance().getTopEntries();
                            player.sendMessage(Text.literal("§6§l❖ TRUE RUN HALL OF FAME ❖"), false);
                            if (top.isEmpty()) {
                                player.sendMessage(Text.literal("§7No Floor 100 completions yet."), false);
                            } else {
                                for (LeaderboardEntry e : top) {
                                    int min = e.durationSeconds() / 60;
                                    int sec = e.durationSeconds() % 60;
                                    String rankStr = switch (e.rank()) {
                                        case 1 -> "§6🥇 1st";
                                        case 2 -> "§f🥈 2nd";
                                        case 3 -> "§c🥉 3rd";
                                        default -> "§7#" + e.rank();
                                    };
                                    player.sendMessage(Text.literal(rankStr + " §e" + e.playerNames() + " §7— ⏱ " + String.format("%02d:%02d", min, sec) + " §7(Turns: " + e.totalTurns() + ", Faints: " + e.faints() + ")"), false);
                                }
                            }
                            return 1;
                        }))

                // /tower prestige
                .then(CommandManager.literal("prestige")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            var profile = TowerPlayerDataManager.getInstance().getProfile(player.getUuid());
                            if (profile.highestFloorTrueRun < 100) {
                                player.sendMessage(Text.literal("§c[CobbleTower] You must conquer Floor 100 in a True Run before advancing your Prestige!"), false);
                                return 0;
                            }
                            if (profile.prestigeLevel >= 10) {
                                player.sendMessage(Text.literal("§6[CobbleTower] You have reached the Maximum Prestige Level (★ Paragon X)!"), false);
                                return 0;
                            }

                            TowerPlayerDataManager.getInstance().addPrestige(player.getUuid());
                            profile.soloCheckpoint = 1;
                            profile.duoCheckpoint = 1;
                            TowerPlayerDataManager.getInstance().saveProfile(player.getUuid());
                            TowerPartyManager.getInstance().syncPlayerState(player);

                            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            player.sendMessage(Text.literal("§6§l★ PRESTIGE UPGRADE! §fYou ascended to §bPrestige Level " + profile.prestigeLevel + " ⭐§f! (+5% permanent BP gain). Checkpoints reset to F1."), false);
                            return 1;
                        }))

                // /tower cosmetic <id>
                .then(CommandManager.literal("cosmetic")
                        .then(CommandManager.argument("aura", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    String aura = StringArgumentType.getString(ctx, "aura");
                                    if ("none".equalsIgnoreCase(aura) || "off".equalsIgnoreCase(aura)) {
                                        TowerPlayerDataManager.getInstance().setActiveCosmeticAura(player.getUuid(), "NONE");
                                        player.sendMessage(Text.literal("§7[CobbleTower] Disabled active particle cosmetic."), false);
                                        return 1;
                                    }
                                    if (!TowerPlayerDataManager.getInstance().hasCosmetic(player.getUuid(), aura)) {
                                        player.sendMessage(Text.literal("§c[CobbleTower] You do not own this cosmetic perk yet! Purchase it from the BP Shop."), false);
                                        return 0;
                                    }
                                    TowerPlayerDataManager.getInstance().setActiveCosmeticAura(player.getUuid(), aura);
                                    player.sendMessage(Text.literal("§a[CobbleTower] Activated cosmetic aura: §e" + aura), false);
                                    return 1;
                                })))

                // /tower stats
                .then(CommandManager.literal("stats")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            var profile = TowerPlayerDataManager.getInstance().getProfile(player.getUuid());
                            player.sendMessage(Text.literal("§6§l❖ COBBLETOWER PLAYER PROFILE ❖"), false);
                            player.sendMessage(Text.literal("§e● Player: §f" + player.getName().getString()), false);
                            player.sendMessage(Text.literal("§e● Prestige Level: §b" + profile.prestigeLevel + " " + (profile.prestigeLevel >= 5 ? "👑" : (profile.prestigeLevel >= 3 ? "🌟" : "⭐")) + " §7(+" + (profile.prestigeLevel * 5) + "% BP Gain)"), false);
                            player.sendMessage(Text.literal("§e● Active Cosmetic: §d" + profile.activeCosmeticAura), false);
                            player.sendMessage(Text.literal("§e● Battle Points: §a" + profile.battlePoints + " BP"), false);
                            player.sendMessage(Text.literal("§e● Highest Floor (True Run): §6" + profile.highestFloorTrueRun + "/100"), false);
                            player.sendMessage(Text.literal("§e● Solo Checkpoint: §fFloor " + profile.soloCheckpoint), false);
                            player.sendMessage(Text.literal("§e● Duo Checkpoint: §fFloor " + profile.duoCheckpoint), false);
                            return 1;
                        }))

                // /tower reload (admin permission)
                .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            TowerConfig.loadConfig();
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[CobbleTower] Config reloaded successfully!"), true);
                            return 1;
                        }))

                // /tower admin compensate <player> <level> — Restore all Pokémon to target level (Admin only)
                .then(CommandManager.literal("admin")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("compensate")
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                                    int targetLevel = IntegerArgumentType.getInteger(ctx, "level");
                                                    return compensatePlayer(ctx.getSource(), target, targetLevel);
                                                })))))
        );
    }

    private static int compensatePlayer(ServerCommandSource source, ServerPlayerEntity target, int targetLevel) {
        try {
            var cobblemonParty = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(target);
            if (cobblemonParty == null) {
                source.sendError(Text.literal("§c[CobbleTower] Failed to access Cobblemon party for " + target.getName().getString()));
                return 0;
            }

            int restoredCount = 0;
            StringBuilder details = new StringBuilder();
            for (com.cobblemon.mod.common.pokemon.Pokemon mon : cobblemonParty) {
                if (mon == null) continue;
                int oldLevel = mon.getLevel();
                if (oldLevel < targetLevel) {
                    mon.setLevel(targetLevel);
                    com.vitwo.reward.TowerRewardManager.fullHeal(mon);
                    details.append(String.format("  %s: Lv.%d → Lv.%d\n", mon.getSpecies().getName(), oldLevel, targetLevel));
                    restoredCount++;
                }
            }

            final int finalRestoredCount = restoredCount;
            final String detailsStr = details.toString();

            if (finalRestoredCount > 0) {
                source.sendFeedback(() -> Text.literal("§a[CobbleTower] Compensated " + target.getName().getString() + 
                        ": " + finalRestoredCount + " Pokémon restored to Lv." + targetLevel + "\n" + detailsStr), true);
                target.sendMessage(Text.literal("§a[CobbleTower] §fAdmin compensation applied: §a" + finalRestoredCount + 
                        " §fPokémon upgraded to §aLv." + targetLevel + "§f. Apologies for any inconvenience!"), false);
                target.sendMessage(Text.literal("§6[CobbleTower] §fDetails:\n" + detailsStr), false);
            } else {
                source.sendFeedback(() -> Text.literal("§7[CobbleTower] No Pokémon needed compensation — all are already at or above Lv." + targetLevel), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§c[CobbleTower] Compensation failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int startSolo(ServerCommandSource source, int checkpointFloor) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            TowerParty soloParty = TowerPartyManager.getInstance().createSoloParty(player, checkpointFloor);
            TowerPartyManager.getInstance().startTowerSession(soloParty, true, checkpointFloor, source.getServer());
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§cFailed to start Tower session: " + e.getMessage()));
            return 0;
        }
    }
}
