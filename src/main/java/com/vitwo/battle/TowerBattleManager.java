package com.vitwo.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerBattleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-BattleManager");
    private static final TowerBattleManager INSTANCE = new TowerBattleManager();
    public static TowerBattleManager getInstance() { return INSTANCE; }

    private final Set<UUID> inTowerBattlePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Random RANDOM = new Random();

    private TowerBattleManager() {}

    /**
     * Subscribes to Cobblemon battle end events (Victory & Fled) to automatically progress tower floors
     */
    public void registerBattleEvents() {
        try {
            CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (Function1<BattleVictoryEvent, Unit>) event -> {
                handleBattleVictory(event);
                return Unit.INSTANCE;
            });

            CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, (Function1<BattleFledEvent, Unit>) event -> {
                handleBattleFled(event);
                return Unit.INSTANCE;
            });

            LOGGER.info("[CobbleTower] Successfully registered Cobblemon BATTLE_VICTORY and BATTLE_FLED event listeners!");
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Error registering battle event listeners", t);
        }
    }

    private void handleBattleVictory(BattleVictoryEvent event) {
        if (event == null) return;

        // 1. Check if any winner is in an active Tower party
        for (BattleActor winner : event.getWinners()) {
            if (winner == null) continue;
            for (UUID playerId : winner.getPlayerUUIDs()) {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
                if (partyOpt.isPresent()) {
                    TowerParty party = partyOpt.get();
                    if (party.getState() == TowerParty.State.IN_BATTLE) {
                        for (UUID memId : party.getAllMembers()) {
                            inTowerBattlePlayers.remove(memId);
                        }

                        // Retrieve server from one of the party members
                        ServerPlayerEntity leader = null;
                        for (UUID id : party.getAllMembers()) {
                            ServerPlayerEntity p = getServerPlayer(id);
                            if (p != null) {
                                leader = p;
                                break;
                            }
                        }

                        if (leader != null && leader.getServer() != null) {
                            MinecraftServer server = leader.getServer();
                            server.execute(() -> {
                                for (UUID id : party.getAllMembers()) {
                                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                                    if (p != null) {
                                        p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                                    }
                                }
                                TowerPartyManager.getInstance().onFloorWon(party, server);
                            });
                        }
                        return;
                    }
                }
            }
        }

        // 2. Check if any loser is in an active Tower party (Loss condition)
        for (BattleActor loser : event.getLosers()) {
            if (loser == null) continue;
            for (UUID playerId : loser.getPlayerUUIDs()) {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
                if (partyOpt.isPresent()) {
                    TowerParty party = partyOpt.get();
                    if (party.getState() == TowerParty.State.IN_BATTLE) {
                        for (UUID memId : party.getAllMembers()) {
                            inTowerBattlePlayers.remove(memId);
                        }

                        ServerPlayerEntity leader = getServerPlayer(playerId);
                        if (leader != null && leader.getServer() != null) {
                            MinecraftServer server = leader.getServer();
                            server.execute(() -> {
                                for (UUID id : party.getAllMembers()) {
                                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                                    if (p != null) {
                                        p.playSound(SoundEvents.ENTITY_WITHER_DEATH, 0.7f, 0.8f);
                                    }
                                }
                                TowerPartyManager.getInstance().onPartyDefeated(party, server);
                            });
                        }
                        return;
                    }
                }
            }
        }
    }

    private void handleBattleFled(BattleFledEvent event) {
        if (event == null || event.getPlayer() == null) return;
        UUID playerId = event.getPlayer().getUuid();
        Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
        if (partyOpt.isPresent()) {
            TowerParty party = partyOpt.get();
            if (party.getState() == TowerParty.State.IN_BATTLE) {
                for (UUID memId : party.getAllMembers()) {
                    inTowerBattlePlayers.remove(memId);
                }
                ServerPlayerEntity player = getServerPlayer(playerId);
                if (player != null && player.getServer() != null) {
                    MinecraftServer server = player.getServer();
                    server.execute(() -> {
                        player.sendMessage(Text.literal("§c[CobbleTower] You fled from the battle! Tower run ended."), false);
                        TowerPartyManager.getInstance().onPartyDefeated(party, server);
                    });
                }
            }
        }
    }

    private ServerPlayerEntity getServerPlayer(UUID uuid) {
        MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();
        if (server != null) {
            return server.getPlayerManager().getPlayer(uuid);
        }
        return null;
    }

    public TowerTeam getBossTeamForFloor(int floor) {
        return TrainerPool.getTeamForFloor(floor);
    }

    public String getBossNameForFloor(int floor) {
        return TrainerPool.getTrainerDisplayName(floor);
    }

    public List<String> getBossTeamSpeciesForFloor(int floor) {
        return TrainerPool.generateDynamicTeam(floor);
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

    public void sendTeamPreview(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member, int floor) {
        // Disabled per request for pure mystery battle flow
    }

    public void handleReadyTeamPreview(ServerPlayerEntity player, List<Integer> slotOrder) {
        if (player == null) return;
        player.sendMessage(Text.literal("§a✔ Ready for battle!"), false);
    }
}
