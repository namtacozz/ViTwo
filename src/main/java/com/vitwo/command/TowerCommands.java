package com.vitwo.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.vitwo.config.TowerConfig;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import com.vitwo.party.TowerRunPersistenceManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

                // /tower stats
                .then(CommandManager.literal("stats")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            var profile = TowerPlayerDataManager.getInstance().getProfile(player.getUuid());
                            player.sendMessage(Text.literal("§6§l❖ COBBLETOWER PLAYER PROFILE ❖"), false);
                            player.sendMessage(Text.literal("§e● Player: §f" + player.getName().getString()), false);
                            player.sendMessage(Text.literal("§e● Prestige Level: §b" + profile.prestigeLevel + " " + (profile.prestigeLevel >= 5 ? "👑" : (profile.prestigeLevel >= 3 ? "🌟" : "⭐"))), false);
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
        );
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
