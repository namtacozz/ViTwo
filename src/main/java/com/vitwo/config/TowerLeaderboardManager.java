package com.vitwo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TowerLeaderboardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-Leaderboard");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TowerLeaderboardManager INSTANCE = new TowerLeaderboardManager();

    public static TowerLeaderboardManager getInstance() {
        return INSTANCE;
    }

    public record LeaderboardEntry(
            int rank,
            String playerNames,
            boolean isSolo,
            int durationSeconds,
            int totalTurns,
            int faints,
            long completionTimestamp
    ) {}

    private final List<LeaderboardEntry> topEntries = new ArrayList<>();

    private TowerLeaderboardManager() {
        loadLeaderboard();
    }

    private File getLeaderboardFile() {
        net.minecraft.server.MinecraftServer server = com.vitwo.party.TowerPartyManager.getInstance().getCurrentServer();
        Path baseDir;
        if (server != null) {
            baseDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("cobbletower_data");
        } else {
            baseDir = FabricLoader.getInstance().getGameDir().resolve("cobbletower_data");
        }
        File dir = baseDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return baseDir.resolve("leaderboard.json").toFile();
    }

    public synchronized void loadLeaderboard() {
        File file = getLeaderboardFile();
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<LeaderboardEntry>>() {}.getType();
            List<LeaderboardEntry> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                topEntries.clear();
                topEntries.addAll(loaded);
                sortAndTrim();
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to load leaderboard: ", e);
        }
    }

    public synchronized void saveLeaderboard() {
        File file = getLeaderboardFile();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(topEntries, writer);
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to save leaderboard: ", e);
        }
    }

    public synchronized boolean recordCompletion(String playerNames, boolean isSolo, int durationSeconds, int totalTurns, int faints) {
        if (playerNames == null || playerNames.isBlank()) return false;

        LeaderboardEntry newEntry = new LeaderboardEntry(
                1,
                playerNames,
                isSolo,
                durationSeconds,
                totalTurns,
                faints,
                System.currentTimeMillis()
        );

        topEntries.add(newEntry);
        sortAndTrim();
        saveLeaderboard();
        return true;
    }

    private void sortAndTrim() {
        // Sort primarily by duration (fastest time), then by total turns, then by faints
        topEntries.sort(Comparator
                .comparingInt(LeaderboardEntry::durationSeconds)
                .thenComparingInt(LeaderboardEntry::totalTurns)
                .thenComparingInt(LeaderboardEntry::faints)
        );

        // Re-assign ranks 1..10
        List<LeaderboardEntry> updated = new ArrayList<>();
        int max = Math.min(10, topEntries.size());
        for (int i = 0; i < max; i++) {
            LeaderboardEntry old = topEntries.get(i);
            updated.add(new LeaderboardEntry(
                    i + 1,
                    old.playerNames(),
                    old.isSolo(),
                    old.durationSeconds(),
                    old.totalTurns(),
                    old.faints(),
                    old.completionTimestamp()
            ));
        }

        topEntries.clear();
        topEntries.addAll(updated);
    }

    public synchronized List<LeaderboardEntry> getTopEntries() {
        return Collections.unmodifiableList(new ArrayList<>(topEntries));
    }
}
