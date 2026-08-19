package com.vitwo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class TowerTeraPresetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<String>> DEFAULT_PRESETS = new HashMap<>();
    private static Map<String, List<String>> presets = new HashMap<>();

    static {
        // Rain Synergy
        DEFAULT_PRESETS.put("kingdra", List.of("water", "dragon", "steel"));
        DEFAULT_PRESETS.put("pelipper", List.of("water", "flying", "ice"));
        DEFAULT_PRESETS.put("barraskewda", List.of("water", "fighting", "dark"));
        DEFAULT_PRESETS.put("zapdos", List.of("electric", "flying", "water"));
        DEFAULT_PRESETS.put("urshifu", List.of("water", "fighting", "poison"));
        DEFAULT_PRESETS.put("archaludon", List.of("steel", "dragon", "electric"));

        // Sun Synergy
        DEFAULT_PRESETS.put("torkoal", List.of("fire", "grass", "rock"));
        DEFAULT_PRESETS.put("chi_yu", List.of("fire", "dark", "ghost"));
        DEFAULT_PRESETS.put("flutter_mane", List.of("fairy", "ghost", "fire"));
        DEFAULT_PRESETS.put("walking_wake", List.of("water", "dragon", "fire"));
        DEFAULT_PRESETS.put("roaring_moon", List.of("dark", "dragon", "flying"));
        DEFAULT_PRESETS.put("great_tusk", List.of("ground", "fighting", "ice"));

        // Trick Room
        DEFAULT_PRESETS.put("hatterene", List.of("fairy", "psychic", "water"));
        DEFAULT_PRESETS.put("ursaluna", List.of("normal", "ground", "bloodmoon"));
        DEFAULT_PRESETS.put("kingambit", List.of("dark", "steel", "flying"));
        DEFAULT_PRESETS.put("cresselia", List.of("psychic", "fairy", "poison"));
        DEFAULT_PRESETS.put("amoonguss", List.of("grass", "poison", "water"));
        DEFAULT_PRESETS.put("armarouge", List.of("fire", "psychic", "grass"));

        // Bulky Control
        DEFAULT_PRESETS.put("incineroar", List.of("fire", "dark", "grass"));
        DEFAULT_PRESETS.put("ting_lu", List.of("ground", "dark", "poison"));
        DEFAULT_PRESETS.put("gholdengo", List.of("steel", "ghost", "flying"));
        DEFAULT_PRESETS.put("rillaboom", List.of("grass", "normal", "fire"));
        DEFAULT_PRESETS.put("landorus", List.of("ground", "flying", "poison"));
        DEFAULT_PRESETS.put("ogerpon", List.of("grass", "fire", "water"));

        // Legendary Bosses
        DEFAULT_PRESETS.put("arceus", List.of("normal", "dragon", "fairy"));
        DEFAULT_PRESETS.put("mewtwo", List.of("psychic", "fighting", "ghost"));
        DEFAULT_PRESETS.put("rayquaza", List.of("dragon", "flying", "normal"));
        DEFAULT_PRESETS.put("kyogre", List.of("water", "ice", "grass"));
        DEFAULT_PRESETS.put("groudon", List.of("ground", "fire", "grass"));
        DEFAULT_PRESETS.put("dialga", List.of("steel", "dragon", "fairy"));
        DEFAULT_PRESETS.put("palkia", List.of("water", "dragon", "fairy"));
        DEFAULT_PRESETS.put("giratina", List.of("ghost", "dragon", "steel"));
        DEFAULT_PRESETS.put("zacian", List.of("fairy", "steel", "ground"));
        DEFAULT_PRESETS.put("koraidon", List.of("fighting", "dragon", "fire"));
        DEFAULT_PRESETS.put("miraidon", List.of("electric", "dragon", "fairy"));
    }

    public static List<String> getTeraPresetsForSpecies(String species) {
        if (presets.isEmpty()) {
            loadPresets();
        }
        if (species == null) return List.of("normal");
        String clean = species.toLowerCase().replace(" ", "_").replace("-", "_");
        return presets.getOrDefault(clean, DEFAULT_PRESETS.getOrDefault(clean, List.of("normal", "steel", "fairy")));
    }

    public static void loadPresets() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobbletower");
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = configDir.resolve("tera_presets.json").toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                presets = GSON.fromJson(reader, type);
                if (presets != null && !presets.isEmpty()) return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        presets = new HashMap<>(DEFAULT_PRESETS);
        savePresets();
    }

    public static void savePresets() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobbletower");
        File file = configDir.resolve("tera_presets.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(presets, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
