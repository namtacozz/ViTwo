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

    private final String[] EARLY_BOSSES = {
            "Youngster Joey", "Bug Catcher Sammy", "Lass Carrie", "Camper Todd", "Picnicker Diana",
            "School Kid Billy", "Bird Keeper Toby", "Hiker Clark", "Fisherman Jonah", "Gym Leader Brock"
    };

    private final String[] MID_BOSSES = {
            "Ace Trainer Aaron", "Veteran Brian", "Cool Trainer Maya", "Dragon Tamer Nicholas",
            "Black Belt Koji", "Hex Maniac Sabrina", "Psychic Eugene", "Gym Leader Erika",
            "Gym Leader Sabrina", "Gym Leader Giovanni"
    };

    private final String[] HIGH_BOSSES = {
            "Elite Four Bruno", "Elite Four Karen", "Elite Four Koga", "Elite Four Will",
            "Elite Four Lorelei", "Elite Four Agatha", "Elite Four Drake", "Champion Lance",
            "Champion Steven", "Champion Cynthia"
    };

    private final String[] MASTER_BOSSES = {
            "Frontier Brain Brandon", "Frontier Brain Anabel", "Legendary Trainer Red",
            "Legendary Trainer Blue", "Ancient Hero Volo", "Distortion Sovereign Cyrus",
            "Champion Leon", "Champion Geeta", "Paradox Master Sada", "Paradox Master Turo"
    };

    private TowerBattleManager() {}

    public String getBossNameForFloor(int floor) {
        if (floor <= 25) {
            int idx = Math.min((floor - 1) % EARLY_BOSSES.length, EARLY_BOSSES.length - 1);
            return EARLY_BOSSES[idx];
        } else if (floor <= 50) {
            int idx = Math.min((floor - 26) % MID_BOSSES.length, MID_BOSSES.length - 1);
            return MID_BOSSES[idx];
        } else if (floor <= 75) {
            int idx = Math.min((floor - 51) % HIGH_BOSSES.length, HIGH_BOSSES.length - 1);
            return HIGH_BOSSES[idx];
        } else if (floor < 100) {
            int idx = Math.min((floor - 76) % MASTER_BOSSES.length, MASTER_BOSSES.length - 1);
            return MASTER_BOSSES[idx];
        } else {
            return "§4§lULTIMATE BOSS: ARCEUS AVATAR";
        }
    }

    public boolean isInTowerBattle(UUID playerId) {
        return inTowerBattlePlayers.contains(playerId);
    }

    /**
     * Start Solo Mode: Double Battle where 1 player commands both active battle slots
     */
    public void startSoloDoubleBattle(TowerParty party, ServerPlayerEntity player, int floor) {
        inTowerBattlePlayers.add(player.getUuid());

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(floor);

        player.sendMessage(Text.literal("§6[CobbleTower] §eHình thức: §fĐấu Đôi Solo (2 Slot) §7| §eGiới Hạn Cấp: §aMax Lv." + maxCap), false);
        if (hasShiny) {
            player.sendMessage(Text.literal("§d✨ Boss sở hữu 1 Pokemon Shiny với Full 6x31 IVs hoàn hảo!"), false);
        } else {
            player.sendMessage(Text.literal("§7Boss được tăng cường trạng thái hoàn hảo (Full 6x31 IVs)."), false);
        }

        String battleRules = GimmickController.getFloorBattleRulesDescription(floor);
        player.sendMessage(Text.literal(battleRules), false);
        player.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);

        // Cobblemon Double Battle Initiation (Player controls slot 1 & 2)
    }

    /**
     * Start Duo Mode: Double Battle where 2 players cooperate (1 slot each)
     */
    public void startDuoDoubleBattle(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member, int floor) {
        inTowerBattlePlayers.add(leader.getUuid());
        inTowerBattlePlayers.add(member.getUuid());

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(floor);

        String header = "§6[CobbleTower] §eHình thức: §fĐấu Đôi Co-op (2v1) §7| §eGiới Hạn Cấp: §aMax Lv." + maxCap;
        leader.sendMessage(Text.literal(header), false);
        member.sendMessage(Text.literal(header), false);

        if (hasShiny) {
            leader.sendMessage(Text.literal("§d✨ Boss sở hữu 1 Pokemon Shiny với Full 6x31 IVs hoàn hảo!"), false);
            member.sendMessage(Text.literal("§d✨ Boss sở hữu 1 Pokemon Shiny với Full 6x31 IVs hoàn hảo!"), false);
        }

        String battleRules = GimmickController.getFloorBattleRulesDescription(floor);
        leader.sendMessage(Text.literal(battleRules), false);
        member.sendMessage(Text.literal(battleRules), false);

        leader.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);
        member.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), false);

        // Cobblemon 2v1 Double Battle Initiation
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
