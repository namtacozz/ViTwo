package com.vitwo.battle;

import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerBattleManager {
    private static final TowerBattleManager INSTANCE = new TowerBattleManager();
    public static TowerBattleManager getInstance() { return INSTANCE; }

    private final Set<UUID> inTowerBattlePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Random RANDOM = new Random();

    // 6-Pokemon Rosters for Floors 91-100 (Full Legendary / Mythical)
    public static final List<List<String>> LEGENDARY_ROSTERS = List.of(
            List.of("ho-oh", "lugia", "entei", "raikou", "suicune", "celebi"),
            List.of("kyogre", "groudon", "rayquaza", "latios", "latias", "jirachi"),
            List.of("rayquaza", "deoxys", "regigigas", "darkrai", "cresselia", "victini"),
            List.of("dialga", "palkia", "giratina", "heatran", "regigigas", "shaymin"),
            List.of("palkia", "dialga", "giratina", "manaphy", "phione", "darkrai"),
            List.of("giratina", "darkrai", "mewtwo", "marshadow", "necrozma", "calyrex"),
            List.of("groudon", "kyogre", "rayquaza", "zacian", "zamazenta", "eternatus"),
            List.of("kyogre", "groudon", "rayquaza", "miraidon", "koraidon", "ting-lu"),
            List.of("calyrex", "spectrier", "glastrier", "zacian", "urshifu", "arceus"),
            List.of("arceus", "mewtwo", "rayquaza", "giratina", "dialga", "palkia")
    );

    private TowerBattleManager() {}

    public TowerTeam getBossTeamForFloor(int floor) {
        return TrainerPool.getTeamForFloor(floor);
    }

    public String getBossNameForFloor(int floor) {
        return TrainerPool.getRandomTrainerName(floor);
    }

    public List<String> getBossTeamSpeciesForFloor(int floor) {
        return TrainerPool.generateDynamicTeam(floor);
    }

    public String getRandomBossSpecies(int floor) {
        List<String> roster = getBossTeamSpeciesForFloor(floor);
        return roster.get(RANDOM.nextInt(roster.size()));
    }

    public boolean isInTowerBattle(UUID playerId) {
        return inTowerBattlePlayers.contains(playerId);
    }

    public void startSoloDoubleBattle(TowerParty party, ServerPlayerEntity player, int floor) {
        inTowerBattlePlayers.add(player.getUuid());

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(floor);
        String bossName = getBossNameForFloor(floor);

        player.sendMessage(Text.literal("§b[CobbleTower] §fOpponent: §e" + bossName + " §7| §bSolo 2-Slot §7| §eCap: §aLv." + maxCap), false);
        if (floor >= 91) {
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

        if (floor >= 91) {
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
        int duration = (floor >= 91) ? 45 : ((floor % 10 == 0) ? 30 : 20);
        String bossName = getBossNameForFloor(floor);
        String bossTitle = (floor >= 91) ? "« Tower Sovereign Boss »" : ((floor % 10 == 0) ? "« Tower Milestone Boss »" : "Tower Challenger");
        List<String> oppRoster = getBossTeamSpeciesForFloor(floor);

        if (leader != null) {
            List<String> playerRoster = getPlayerPartySpecies(leader);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    leader,
                    new com.vitwo.network.s2c.OpenTeamPreviewS2CPacket(floor, duration, bossName, bossTitle, oppRoster, playerRoster)
            );
        }
        if (member != null) {
            List<String> memberRoster = getPlayerPartySpecies(member);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    member,
                    new com.vitwo.network.s2c.OpenTeamPreviewS2CPacket(floor, duration, bossName, bossTitle, oppRoster, memberRoster)
            );
        }
    }

    public void handleReadyTeamPreview(ServerPlayerEntity player, List<Integer> slotOrder) {
        if (player == null) return;
        player.sendMessage(Text.literal("§a✔ Team Preview confirmed! Entering battle..."), false);
    }

    private List<String> getPlayerPartySpecies(ServerPlayerEntity player) {
        List<String> list = new ArrayList<>();
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            java.lang.reflect.Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            for (Object pokemon : party) {
                if (pokemon == null) continue;
                java.lang.reflect.Method getSpeciesMethod = pokemon.getClass().getMethod("getSpecies");
                Object species = getSpeciesMethod.invoke(pokemon);
                java.lang.reflect.Method getNameMethod = species.getClass().getMethod("getName");
                String name = (String) getNameMethod.invoke(species);
                list.add(name);
            }
        } catch (Exception ignored) {
            list.addAll(List.of("Pokémon 1", "Pokémon 2", "Pokémon 3", "Pokémon 4", "Pokémon 5", "Pokémon 6"));
        }
        return list;
    }

    public void endBattle(TowerParty party, boolean victory, ServerPlayerEntity leader, ServerPlayerEntity member) {
        if (leader != null) {
            inTowerBattlePlayers.remove(leader.getUuid());
            TowerSpectatorManager.getInstance().restoreFromSpectator(leader);
        }
        if (member != null) {
            inTowerBattlePlayers.remove(member.getUuid());
            TowerSpectatorManager.getInstance().restoreFromSpectator(member);
        }

        if (victory) {
            TowerPartyManager.getInstance().onFloorWon(party, leader != null ? leader.getServer() : member.getServer());
        } else {
            TowerPartyManager.getInstance().onPartyDefeated(party, leader != null ? leader.getServer() : member.getServer());
        }
    }
}
