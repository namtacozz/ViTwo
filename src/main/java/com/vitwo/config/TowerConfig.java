package com.vitwo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

public class TowerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TowerConfig INSTANCE;

    public enum DifficultyProfile {
        CASUAL(0.5, 0.15, 2.0, 3, 0.6, 60),
        STANDARD(1.0, 0.05, 1.0, 1, 1.0, 45),
        HARDCORE(1.5, 0.0, 0.75, 0, 1.5, 30);

        public final double curseMultiplier;
        public final double aiMisplayRate;
        public final double previewTimeMultiplier;
        public final int mercyContinues;
        public final double bpMultiplier;
        public final int turnTimerSeconds;

        DifficultyProfile(double curseMultiplier, double aiMisplayRate, double previewTimeMultiplier, int mercyContinues, double bpMultiplier, int turnTimerSeconds) {
            this.curseMultiplier = curseMultiplier;
            this.aiMisplayRate = aiMisplayRate;
            this.previewTimeMultiplier = previewTimeMultiplier;
            this.mercyContinues = mercyContinues;
            this.bpMultiplier = bpMultiplier;
            this.turnTimerSeconds = turnTimerSeconds;
        }
    }

    public DifficultyProfile difficulty = DifficultyProfile.STANDARD;
    public GeneralConfig general = new GeneralConfig();
    public ClausesConfig clauses = new ClausesConfig();
    public RestrictedPokemonConfig restrictedPokemon = new RestrictedPokemonConfig();
    public LevelCapsConfig levelCaps = new LevelCapsConfig();
    public AffixesConfig affixes = new AffixesConfig();
    public RestStationConfig restStation = new RestStationConfig();
    public DuoConfig duo = new DuoConfig();
    public AiConfig ai = new AiConfig();
    public BpConfig bp = new BpConfig();
    public ArenaConfig arena = new ArenaConfig();

    public static class ArenaConfig {
        public int sectorSpacing = 300;
        public int groundY = 64;
        public int battlefieldHeightOffset = 19;
        public int maxArenaSlots = 64;
    }

    public static class GeneralConfig {
        public String keybind = "KEY_Y";
        public int maxFloors = 100;
        public int restStationInterval = 5;
        public List<Integer> checkpointFloors = List.of(25, 50, 75);
        public boolean spectatorOnWipe = true;
    }

    public static class ClausesConfig {
        public boolean speciesClause = true;
        public boolean itemClause = true;
        public boolean evasionClause = true;
        public boolean ohkoClause = true;
        public boolean moodyClause = true;
        public boolean swaggerClause = true;
        public boolean bagItemsBanned = true;
        public boolean pcAccessBanned = true;
        public boolean teamLockOnRunStart = true;
        public boolean heldItemLockOnRunStart = true;
        public boolean movesetLockOnRunStart = true;
        public boolean tradeBannedDuringRun = true;
    }

    public static class RestrictedPokemonConfig {
        public boolean enabled = true;
        public List<String> restrictedList = List.of(
                "mewtwo", "lugia", "ho_oh", "kyogre", "groudon", "rayquaza",
                "dialga", "palkia", "giratina", "reshiram", "zekrom", "kyurem_white", "kyurem_black",
                "xerneas", "yveltal", "zygarde_complete", "solgaleo", "lunala",
                "necrozma_dusk_mane", "necrozma_dawn_wings", "zacian", "zamazenta", "eternatus",
                "calyrex_ice_rider", "calyrex_shadow_rider", "koraidon", "miraidon", "terapagos"
        );
        public int capFloor1To75 = 1;
        public int capFloor76To100 = 2;
        public int nonRestrictedCapFloor1To75 = 2;
        public int nonRestrictedCapFloor76To100 = -1; // -1 = unlimited
    }

    public static class LevelCapsConfig {
        public int floor1To25 = 36;
        public int floor26To50 = 50;
        public int floor51To75 = 80;
        public int floor76To100 = 100;
    }

    public static class AffixesConfig {
        public int startFloor = 11;
        public int rerollInterval = 10;
        public int dualCurseStart = 91;
        public double curseIroncladReduction = 0.15;
        public double curseFogAccuracyPenalty = 0.10;
        public double curseRetaliationReflect = 0.10;
        public int curseFatigueExtraPp = 1;
    }

    public static class RestStationConfig {
        public double reviveHpPercent = 0.30;
        public double healAlivePercent = 0.40;
        public boolean restorePp = true;
        public boolean clearStatus = true;
    }

    public static class DuoConfig {
        public int disconnectGraceSeconds = 180;
        public boolean aiTakeoverOnDisconnect = true;
        public boolean voteToEndOnDisconnect = true;
        public boolean teamValidationMerged = true;
        public String takeoverTiming = "start_of_next_turn";
    }

    public static class AiConfig {
        public boolean teraPresetMode = true;
        public boolean teraFreeChoice = false;
        public int comboLightStartFloor = 31;
        public int comboFullStartFloor = 51;
        public int counterTeraStartFloor = 76;
        public double hazardStallMaxPercent = 0.05;
    }

    public static class BpConfig {
        public int perFloor = 50;
        public int bossBonus = 150;
        public int checkpoint25Bonus = 500;
        public int checkpoint50Bonus = 1000;
        public int checkpoint75Bonus = 2000;
        public int clear100TrueRun = 10000;
        public int clear100CheckpointRun = 5000;
        public double trueRunMultiplier = 1.0;
        public double checkpointMultiplier = 0.5;
        public double checkpointRunRewardMultiplier = 0.5;
    }

    public static TowerConfig getInstance() {
        if (INSTANCE == null) {
            loadConfig();
        }
        return INSTANCE;
    }

    public static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobbletower");
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File configFile = configDir.resolve("tower.json").toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                INSTANCE = GSON.fromJson(reader, TowerConfig.class);
                if (INSTANCE != null) return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        INSTANCE = new TowerConfig();
        saveConfig();
    }

    public static void saveConfig() {
        if (INSTANCE == null) return;
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobbletower");
        File configFile = configDir.resolve("tower.json").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
