package com.vitwo.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TowerBattleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-BattleManager");
    private static final TowerBattleManager INSTANCE = new TowerBattleManager();
    public static TowerBattleManager getInstance() { return INSTANCE; }

    private final Set<UUID> inTowerBattlePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Random RANDOM = new Random();

    private TowerBattleManager() {}

    /**
     * Subscribes to Cobblemon battle events to handle victory/defeat/fled outcomes.
     */
    public void registerBattleEvents() {
        try {
            CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.NORMAL, (Consumer<BattleStartedEvent.Post>) this::handleBattleStarted);
            CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (Consumer<BattleVictoryEvent>) this::handleBattleVictory);
            CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, (Consumer<BattleFledEvent>) this::handleBattleFled);
            LOGGER.info("[CobbleTower] Successfully registered Cobblemon BATTLE_STARTED_POST, BATTLE_VICTORY, and BATTLE_FLED listeners natively!");
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Error registering battle event listeners natively: {}", t.getMessage(), t);
        }
    }

    /**
     * Dynamically scales the NPC trainer team to FULL 6 POKÉMON all at MAX LEVEL CAP when battle starts
     */
    private void handleBattleStarted(BattleStartedEvent.Post event) {
        if (event == null || event.getBattle() == null) return;
        PokemonBattle battle = event.getBattle();

        // 1. Find if any battle participant is in a Tower Party
        int partyFloor = -1;
        for (UUID playerId : battle.getPlayerUUIDs()) {
            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
            if (partyOpt.isPresent()) {
                TowerParty party = partyOpt.get();
                ServerPlayerEntity p = getServerPlayer(playerId);
                if (p == null || p.getServerWorld() == null || !p.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                    continue;
                }
                party.setState(TowerParty.State.IN_BATTLE);
                partyFloor = party.getCurrentFloor();
                for (UUID memId : party.getAllMembers()) {
                    inTowerBattlePlayers.add(memId);
                }
                MinecraftServer srv = (p.getServer() != null) ? p.getServer() : resolveServer(battle, party);
                if (srv != null) {
                    TowerPartyManager.getInstance().syncParty(party, srv);
                }
                break;
            }
        }

        if (partyFloor <= 0) return;
        LOGGER.info("[CobbleTower] Double Battle started successfully for Floor {}", partyFloor);
    }

    private void handleBattleVictory(BattleVictoryEvent event) {
        if (event == null) return;
        LOGGER.info("[CobbleTower] handleBattleVictory fired! Winners: {}, Losers: {}", event.getWinners(), event.getLosers());

        // 1. Check if any winner is in an active Tower party
        for (BattleActor winner : event.getWinners()) {
            if (winner == null) continue;

            ServerPlayerEntity winningPlayer = null;
            if (winner instanceof PlayerBattleActor pba) {
                winningPlayer = pba.getEntity();
            }

            for (UUID playerId : winner.getPlayerUUIDs()) {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
                if (partyOpt.isEmpty()) continue;

                TowerParty party = partyOpt.get();
                ServerPlayerEntity p = (winningPlayer != null && winningPlayer.getUuid().equals(playerId))
                        ? winningPlayer
                        : (winningPlayer != null && winningPlayer.getServer() != null ? winningPlayer.getServer().getPlayerManager().getPlayer(playerId) : getServerPlayer(playerId));

                // Ensure player is in the tower dimension before handling victory
                if (p != null && p.getServerWorld() != null && !p.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                    continue;
                }

                LOGGER.info("[CobbleTower] Floor {} VICTORY confirmed for Party {}!", party.getCurrentFloor(), party.getLeaderId());

                // Immediately transition out of IN_BATTLE to prevent tick() from triggering false defeat
                party.setState(TowerParty.State.PREPARING);
                party.resetNoBattleTicks();
                party.resetStallTicks();
                party.resetHardStallTicks();

                for (UUID memId : party.getAllMembers()) {
                    inTowerBattlePlayers.remove(memId);
                }

                MinecraftServer server = (p != null && p.getServer() != null) ? p.getServer() : resolveServer(event.getBattle(), party);
                if (server != null) {
                    server.execute(() -> {
                        for (UUID id : party.getAllMembers()) {
                            ServerPlayerEntity memberEntity = server.getPlayerManager().getPlayer(id);
                            if (memberEntity != null) {
                                memberEntity.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            }
                        }
                        TowerPartyManager.getInstance().onFloorWon(party, server);
                    });
                }
                return;
            }
        }

        // 2. Check if any loser is in an active Tower party (Loss condition)
        for (BattleActor loser : event.getLosers()) {
            if (loser == null) continue;

            ServerPlayerEntity losingPlayer = null;
            if (loser instanceof PlayerBattleActor pba) {
                losingPlayer = pba.getEntity();
            }

            for (UUID playerId : loser.getPlayerUUIDs()) {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
                if (partyOpt.isEmpty()) continue;

                TowerParty party = partyOpt.get();
                ServerPlayerEntity p = (losingPlayer != null && losingPlayer.getUuid().equals(playerId))
                        ? losingPlayer
                        : (losingPlayer != null && losingPlayer.getServer() != null ? losingPlayer.getServer().getPlayerManager().getPlayer(playerId) : getServerPlayer(playerId));

                // Ensure player is in the tower dimension before handling defeat
                if (p != null && p.getServerWorld() != null && !p.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                    continue;
                }

                LOGGER.info("[CobbleTower] Defeat on Floor {} confirmed for Party {}!", party.getCurrentFloor(), party.getLeaderId());

                party.setState(TowerParty.State.PREPARING);
                for (UUID memId : party.getAllMembers()) {
                    inTowerBattlePlayers.remove(memId);
                }

                MinecraftServer server = (p != null && p.getServer() != null) ? p.getServer() : resolveServer(event.getBattle(), party);
                if (server != null) {
                    server.execute(() -> {
                        for (UUID id : party.getAllMembers()) {
                            ServerPlayerEntity memberEntity = server.getPlayerManager().getPlayer(id);
                            if (memberEntity != null) {
                                memberEntity.playSound(SoundEvents.ENTITY_WITHER_DEATH, 0.7f, 0.8f);
                            }
                        }
                        TowerPartyManager.getInstance().onPartyDefeated(party, server);
                    });
                }
                return;
            }
        }
    }

    /**
     * Handles fleeing/running from battle: immediately triggers defeat, shows defeat screen, and returns player to Hub.
     */
    private void handleBattleFled(BattleFledEvent event) {
        if (event == null) return;

        List<UUID> playerIds = new ArrayList<>();
        if (event.getPlayer() != null) {
            try {
                Object pObj = event.getPlayer();
                if (pObj instanceof PlayerBattleActor pba) {
                    playerIds.add(pba.getUuid());
                } else {
                    for (Method m : pObj.getClass().getMethods()) {
                        if ("getUuid".equals(m.getName()) && m.getParameterCount() == 0) {
                            Object res = m.invoke(pObj);
                            if (res instanceof UUID u) playerIds.add(u);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (event.getBattle() != null) {
            for (UUID id : event.getBattle().getPlayerUUIDs()) {
                if (!playerIds.contains(id)) playerIds.add(id);
            }
        }

        for (UUID playerId : playerIds) {
            // Ensure player is in the tower dimension before handling tower fled defeat
            ServerPlayerEntity p = getServerPlayer(playerId);
            if (p == null || p.getServerWorld() == null || !p.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                continue;
            }

            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
            MinecraftServer server = resolveServer(event.getBattle(), partyOpt.orElse(null));
            if (partyOpt.isPresent()) {
                TowerParty party = partyOpt.get();
                if (party.isSolo()) {
                    for (UUID memId : party.getAllMembers()) {
                        inTowerBattlePlayers.remove(memId);
                    }

                    if (server != null) {
                        server.execute(() -> {
                            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                            if (player != null) {
                                player.sendMessage(Text.literal("§c[CobbleTower] You fled from battle! Tower run ended in defeat."), false);
                            }
                            TowerPartyManager.getInstance().onPartyDefeated(party, server);
                        });
                    }
                    return;
                } else {
                    // Duo Co-op Mode: Cannot forfeit unilaterally via flee. Must vote in Menu Y!
                    if (server != null) {
                        server.execute(() -> {
                            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                            if (player != null) {
                                player.sendMessage(Text.literal("§e[CobbleTower] §cIn Duo Co-op, you cannot flee unilaterally! Please open [Y] Hub to vote for Forfeit."), false);
                            }
                        });
                    }
                    return;
                }
            } else if (server != null) {
                // If player was in tower dimension without a party
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player != null && player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                    server.execute(() -> {
                        TowerPartyManager.terminateActiveBattleForPlayer(player);
                        com.vitwo.arena.TowerArenaManager.getInstance().returnPlayerToOriginalPos(player, null);
                        player.sendMessage(Text.literal("§c[CobbleTower] You fled from battle! Returned safely to the Overworld."), false);
                    });
                    return;
                }
            }
        }
    }

    private MinecraftServer resolveServer(PokemonBattle battle, TowerParty party) {
        if (party != null) {
            for (UUID id : party.getAllMembers()) {
                ServerPlayerEntity p = getServerPlayer(id);
                if (p != null && p.getServer() != null) return p.getServer();
            }
        }
        if (battle != null) {
            for (UUID id : battle.getPlayerUUIDs()) {
                ServerPlayerEntity p = getServerPlayer(id);
                if (p != null && p.getServer() != null) return p.getServer();
            }
        }
        return TowerPartyManager.getInstance().getCurrentServer();
    }

    private ServerPlayerEntity getServerPlayer(UUID uuid) {
        MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();
        if (server != null) {
            return server.getPlayerManager().getPlayer(uuid);
        }
        return null;
    }

    public String getBossNameForFloor(int floor) {
        return TrainerPool.getTrainerDisplayName(floor);
    }

    public List<String> getBossTeamSpeciesForFloor(int floor) {
        String trainerId = TrainerPool.getRctTrainerIdForFloor(floor);
        var team = HellModeTeamLoader.createTeamFromTrainerId(trainerId, 50);
        if (team != null && !team.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (var mon : team) {
                if (mon != null && mon.getSpecies() != null) {
                    list.add(mon.getSpecies().getName().toLowerCase(Locale.ROOT));
                }
            }
            return list;
        }
        return List.of("pikachu", "charizard", "blastoise", "gengar", "snorlax", "dragonite");
    }

    public String getRandomBossSpecies(int floor) {
        List<String> species = getBossTeamSpeciesForFloor(floor);
        if (species != null && !species.isEmpty()) {
            return species.get(RANDOM.nextInt(species.size()));
        }
        return "pikachu";
    }

    public boolean isInTowerBattle(UUID playerId) {
        return inTowerBattlePlayers.contains(playerId);
    }

    public void startSoloDoubleBattle(TowerParty party, ServerPlayerEntity player, int floor) {
        inTowerBattlePlayers.add(player.getUuid());

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(floor);
        String bossName = getBossNameForFloor(floor);

        player.sendMessage(Text.literal("§b[CobbleTower] §fOpponent: §e" + bossName + " §7| §bSolo 6v6 §7| §eCap: §aLv." + maxCap), false);
        if (floor >= 100) {
            player.sendMessage(Text.literal("§4👑 FINAL BATTLE: Confront the Genesis Arceus Sovereign!"), false);
        } else if (floor >= 91) {
            player.sendMessage(Text.literal("§4⚠ WARNING: Sovereign commands a Full Legendary roster with a Shiny Ace!"), false);
        } else if (hasShiny) {
            player.sendMessage(Text.literal("§d✨ Opponent commands 1 Shiny Pokémon with Perfect 6x31 IVs & Mega/Z/Dyna/Tera!"), false);
        }

        String battleRules = GimmickController.getFloorBattleRulesDescription(floor);
        player.sendMessage(Text.literal(battleRules), false);
        player.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);
    }

    public boolean triggerDuoBattle(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member, net.minecraft.entity.Entity npcEntity, int floor) {
        try {
            // Apply level caps before starting
            LevelCapManager.applyLevelCapToPlayer(leader, floor, party);
            LevelCapManager.applyLevelCapToPlayer(member, floor, party);

            startDuoDoubleBattle(party, leader, member, floor);

            return RCTModAdapter.startBattle(npcEntity, leader);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Error in triggerDuoBattle: {}", e.getMessage(), e);
            return false;
        }
    }

    public void startDuoDoubleBattle(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member, int floor) {
        inTowerBattlePlayers.add(leader.getUuid());
        inTowerBattlePlayers.add(member.getUuid());

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(floor);
        String bossName = getBossNameForFloor(floor);

        String header = "§b[CobbleTower] §fOpponent: §e" + bossName + " §7| §dCo-op Duo §7| §eCap: §aLv." + maxCap;
        leader.sendMessage(Text.literal(header), false);
        member.sendMessage(Text.literal(header), false);

        if (floor >= 100) {
            leader.sendMessage(Text.literal("§4👑 FINAL BATTLE: Confront the Genesis Arceus Sovereign!"), false);
            member.sendMessage(Text.literal("§4👑 FINAL BATTLE: Confront the Genesis Arceus Sovereign!"), false);
        } else if (floor >= 91) {
            leader.sendMessage(Text.literal("§4⚠ WARNING: Sovereign commands a Full Legendary roster with a Shiny Ace!"), false);
            member.sendMessage(Text.literal("§4⚠ WARNING: Sovereign commands a Full Legendary roster with a Shiny Ace!"), false);
        } else if (hasShiny) {
            leader.sendMessage(Text.literal("§d✨ Opponent commands 1 Shiny Pokémon with Perfect 6x31 IVs & Mega/Z/Dyna/Tera!"), false);
            member.sendMessage(Text.literal("§d✨ Opponent commands 1 Shiny Pokémon with Perfect 6x31 IVs & Mega/Z/Dyna/Tera!"), false);
        }

        String battleRules = GimmickController.getFloorBattleRulesDescription(floor);
        leader.sendMessage(Text.literal(battleRules), false);
        member.sendMessage(Text.literal(battleRules), false);

        leader.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);
        member.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);
    }
}
