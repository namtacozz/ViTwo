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
     * Subscribes to Cobblemon battle events to scale NPC teams to 6x max level cap and handle victory/defeat/fled outcomes.
     */
    public void registerBattleEvents() {
        try {
            subscribeObservable(CobblemonEvents.BATTLE_STARTED_POST, this::handleBattleStarted);
            subscribeObservable(CobblemonEvents.BATTLE_VICTORY, this::handleBattleVictory);
            subscribeObservable(CobblemonEvents.BATTLE_FLED, this::handleBattleFled);
            LOGGER.info("[CobbleTower] Successfully registered Cobblemon BATTLE_STARTED_POST, BATTLE_VICTORY, and BATTLE_FLED listeners!");
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
                if (party.getState() == TowerParty.State.PREPARING || party.getState() == TowerParty.State.IN_BATTLE) {
                    party.setState(TowerParty.State.IN_BATTLE);
                    partyFloor = party.getCurrentFloor();
                    MinecraftServer srv = resolveServer(battle, party);
                    if (srv != null) {
                        TowerPartyManager.getInstance().syncParty(party, srv);
                    }
                    break;
                }
            }
        }

        if (partyFloor <= 0) return;
        LOGGER.info("[CobbleTower] Double Battle started successfully for Floor {}", partyFloor);
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
                    if (party.getState() == TowerParty.State.IN_BATTLE || party.getState() == TowerParty.State.PREPARING) {
                        for (UUID memId : party.getAllMembers()) {
                            inTowerBattlePlayers.remove(memId);
                        }

                        MinecraftServer server = resolveServer(event.getBattle(), party);
                        if (server != null) {
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
                    if (party.getState() == TowerParty.State.IN_BATTLE || party.getState() == TowerParty.State.PREPARING) {
                        for (UUID memId : party.getAllMembers()) {
                            inTowerBattlePlayers.remove(memId);
                        }

                        MinecraftServer server = resolveServer(event.getBattle(), party);
                        if (server != null) {
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
            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerId);
            if (partyOpt.isPresent()) {
                TowerParty party = partyOpt.get();
                if (party.getState() == TowerParty.State.IN_BATTLE || party.getState() == TowerParty.State.PREPARING) {
                    for (UUID memId : party.getAllMembers()) {
                        inTowerBattlePlayers.remove(memId);
                    }

                    MinecraftServer server = resolveServer(event.getBattle(), party);
                    if (server != null) {
                        server.execute(() -> {
                            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                            if (player != null) {
                                player.sendMessage(Text.literal("§c[CobbleTower] You fled from the battle! Tower run ended."), false);
                            }
                            TowerPartyManager.getInstance().onPartyDefeated(party, server);
                        });
                    }
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
}
