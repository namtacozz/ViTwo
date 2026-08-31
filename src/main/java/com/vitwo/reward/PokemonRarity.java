package com.vitwo.reward;

import java.util.Locale;
import java.util.Set;

public enum PokemonRarity {
    HIGH_LEGEND("High Legend", 0xFFFF3344, 0xFFFFD700, 1),
    LOW_LEGEND("Low Legend", 0xFF9B59B6, 0xFFD7BDE2, 2),
    MYTHICAL("Mythical", 0xFFFF1493, 0xFFFFB6C1, 3),
    PSEUDO_LEGENDARY("Á Thần (Pseudo)", 0xFF8A2BE2, 0xFF00E5FF, 4),
    STARTER("Starter", 0xFF1E90FF, 0xFF85C1E9, 5),
    COMMON("Common", 0xFF7F8C8D, 0xFFE5E7E9, 6);

    private final String displayName;
    private final int headerColor;
    private final int glowColor;
    private final int tierOrder;

    PokemonRarity(String displayName, int headerColor, int glowColor, int tierOrder) {
        this.displayName = displayName;
        this.headerColor = headerColor;
        this.glowColor = glowColor;
        this.tierOrder = tierOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHeaderColor() {
        return headerColor;
    }

    public int getGlowColor() {
        return glowColor;
    }

    public int getTierOrder() {
        return tierOrder;
    }

    // High Legendaries (Box / Cover / Major deities)
    private static final Set<String> HIGH_LEGEND_SET = Set.of(
            "mewtwo", "lugia", "ho_oh", "kyogre", "groudon", "rayquaza",
            "dialga", "palkia", "giratina", "reshiram", "zekrom", "kyurem", "kyurem_white", "kyurem_black",
            "xerneas", "yveltal", "zygarde", "solgaleo", "lunala",
            "necrozma", "necrozma_dusk_mane", "necrozma_dawn_wings", "zacian", "zamazenta", "eternatus",
            "calyrex", "koraidon", "miraidon", "terapagos", "arceus"
    );

    // Mythicals
    private static final Set<String> MYTHICAL_SET = Set.of(
            "mew", "celebi", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin",
            "victini", "keldeo", "meloetta", "genesect", "diancie", "hoopa", "volcanion",
            "magearna", "marshadow", "zeraora", "meltan", "melmetal", "zarude", "pecharunt"
    );

    // Low Legendaries / Sub-Legendaries
    private static final Set<String> LOW_LEGEND_SET = Set.of(
            "articuno", "zapdos", "moltres", "raikou", "entei", "suicune",
            "regirock", "regice", "registeel", "latias", "latios", "uxie", "mesprit", "azelf",
            "heatran", "regigigas", "cresselia", "cobalion", "terrakion", "virizion",
            "tornadus", "thundurus", "landorus", "type_null", "silvally",
            "tapu_koko", "tapu_lele", "tapu_bulu", "tapu_fini",
            "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord",
            "poipole", "naganadel", "stakataka", "blacephalon",
            "kubfu", "urshifu", "regieleki", "regidrago", "glastrier", "spectrier", "enamorus",
            "wo_chien", "chien_pao", "ting_lu", "chi_yu",
            "roaring_moon", "iron_valiant", "walking_wake", "iron_leaves", "okidogi", "munkidori", "fezandipiti", "ogerpon",
            "gouging_fire", "raging_bolt", "iron_boulder", "iron_crown"
    );

    // Pseudo-Legendaries & Evolutionary Lines
    private static final Set<String> PSEUDO_SET = Set.of(
            "dratini", "dragonair", "dragonite",
            "larvitar", "pupitar", "tyranitar",
            "bagon", "shelgon", "salamence",
            "beldum", "metang", "metagross",
            "gible", "gabite", "garchomp",
            "deino", "zweilous", "hydreigon",
            "goomy", "sliggoo", "goodra",
            "jangmo_o", "hakamo_o", "kommo_o",
            "dreepy", "drakloak", "dragapult",
            "frigibax", "arctibax", "baxcalibur"
    );

    // Starters & Evolutionary Lines
    private static final Set<String> STARTER_SET = Set.of(
            "bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard", "squirtle", "wartortle", "blastoise",
            "chikorita", "bayleef", "meganium", "cyndaquil", "quilava", "typhlosion", "totodile", "croconaw", "feraligatr",
            "treecko", "grovyle", "sceptile", "torchic", "combusken", "blaziken", "mudkip", "marshtomp", "swampert",
            "turtwig", "grotle", "torterra", "chimchar", "monferno", "infernape", "piplup", "prinplup", "empoleon",
            "snivy", "servine", "serperior", "tepig", "pignite", "emboar", "oshawott", "dewott", "samurott",
            "chespin", "quilladin", "chesnaught", "fennekin", "braixen", "delphox", "froakie", "frogadier", "greninja",
            "rowlet", "dartrix", "decidueye", "litten", "torracat", "incineroar", "popplio", "brionne", "primarina",
            "grookey", "thwackey", "rillaboom", "scorbunny", "raboot", "cinderace", "sobble", "drizzile", "inteleon",
            "sprigatito", "floragato", "meowscarada", "fuecoco", "crocalor", "skeledirge", "quaxly", "quaxwell", "quaquaval",
            "pikachu", "eevee"
    );

    public static PokemonRarity fromSpecies(String speciesName) {
        if (speciesName == null) return COMMON;
        String clean = speciesName.toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");

        if (HIGH_LEGEND_SET.contains(clean)) return HIGH_LEGEND;
        if (MYTHICAL_SET.contains(clean)) return MYTHICAL;
        if (LOW_LEGEND_SET.contains(clean)) return LOW_LEGEND;
        if (PSEUDO_SET.contains(clean)) return PSEUDO_LEGENDARY;
        if (STARTER_SET.contains(clean)) return STARTER;

        return COMMON;
    }
}

