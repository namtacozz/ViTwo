package com.vitwo.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
     * Subscribes to Cobblemon battle end events (Victory & Fled) to automatically progress tower floors
     */
    public void registerBattleEvents() {
        try {
            subscribeObservable(CobblemonEvents.BATTLE_VICTORY, this::handleBattleVictory);
            subscribeObservable(CobblemonEvents.BATTLE_FLED, this::handleBattleFled);
            LOGGER.info("[CobbleTower] Successfully registered Cobblemon BATTLE_VICTORY and BATTLE_FLED event listeners!");
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Error registering battle event listeners: {}", t.getMessage());
        }
    }

    private <T> void subscribeObservable(Object observable, Consumer<T> consumer) {
        if (observable == null) return;
        try {
            Class<?> function1Class = Class.forName("kotlin.jvm.functions.Function1");
            Class<?> unitClass = Class.forName("kotlin.Unit");
            Object unitInstance = unitClass.getField("INSTANCE").get(null);

            Object handlerProxy = Proxy.newProxyInstance(
                    function1Class.getClassLoader(),
                    new Class<?>[]{function1Class},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName()) && args != null && args.length == 1) {
                            try {
                                @SuppressWarnings("unchecked")
                                T event = (T) args[0];
                                consumer.accept(event);
                            } catch (Throwable t) {
                                LOGGER.error("[CobbleTower] Error in battle event consumer", t);
                            }
                            return unitInstance;
                        }
                        return null;
                    }
            );

            // Find subscribe(Priority, Function1) or subscribe(Function1)
            for (Method m : observable.getClass().getMethods()) {
                if ("subscribe".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(observable, Priority.NORMAL, handlerProxy);
                    return;
                }
            }
            for (Method m : observable.getClass().getMethods()) {
                if ("subscribe".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.invoke(observable, handlerProxy);
                    return;
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[CobbleTower] Failed to subscribe to observable {}: {}", observable, t.getMessage());
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
