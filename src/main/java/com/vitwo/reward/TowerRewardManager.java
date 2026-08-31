package com.vitwo.reward;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.vitwo.battle.HellModeTeamLoader;
import com.vitwo.battle.TrainerPool;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.network.c2s.ClaimGachaPokemonC2SPacket;
import com.vitwo.network.c2s.ClaimItemGachaC2SPacket;
import com.vitwo.network.s2c.OpenItemGachaS2CPacket;
import com.vitwo.network.s2c.OpenPokemonGachaS2CPacket;
import com.vitwo.network.s2c.TowerBattleGradeS2CPacket;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TowerRewardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-RewardManager");
    private static final TowerRewardManager INSTANCE = new TowerRewardManager();
    public static TowerRewardManager getInstance() { return INSTANCE; }

    private static final Random RANDOM = new Random();

    // BP Shop 5-Category Price Table & Balanced Economy
    public static final Map<String, Integer> BP_SHOP_PRICES = Map.ofEntries(
            // Held & Battle Items
            Map.entry("focus_sash", 800),
            Map.entry("choice_scarf", 1000),
            Map.entry("choice_band", 1000),
            Map.entry("choice_specs", 1000),
            Map.entry("life_orb", 1200),
            Map.entry("assault_vest", 1000),
            Map.entry("heavy_duty_boots", 800),
            Map.entry("leftovers", 800),
            Map.entry("eviolite", 1200),
            Map.entry("rocky_helmet", 800),
            Map.entry("expert_belt", 700),
            Map.entry("air_balloon", 600),
            Map.entry("weakness_policy", 900),
            Map.entry("toxic_orb", 600),
            Map.entry("flame_orb", 600),
            Map.entry("safety_goggles", 700),
            Map.entry("white_herb", 500),
            Map.entry("power_herb", 500),
            Map.entry("mental_herb", 500),
            Map.entry("mirror_herb", 700),
            Map.entry("black_sludge", 700),
            Map.entry("scope_lens", 500),
            Map.entry("wide_lens", 500),
            Map.entry("tera_shard_stellar", 1000),

            // Mints & Training
            Map.entry("rare_candy", 100),
            Map.entry("exp_candy_xl", 150),
            Map.entry("exp_candy_l", 80),
            Map.entry("hp_up", 15),
            Map.entry("protein", 15),
            Map.entry("iron", 15),
            Map.entry("calcium", 15),
            Map.entry("zinc", 15),
            Map.entry("carbos", 15),
            Map.entry("gold_bottle_cap", 2000),
            Map.entry("bottle_cap", 500),
            Map.entry("pp_max", 1200),
            Map.entry("pp_up", 400),
            Map.entry("adamant_mint", 300),
            Map.entry("modest_mint", 300),
            Map.entry("jolly_mint", 300),
            Map.entry("timid_mint", 300),
            Map.entry("bold_mint", 300),
            Map.entry("calm_mint", 300),
            Map.entry("brave_mint", 300),
            Map.entry("quiet_mint", 300),
            Map.entry("impish_mint", 300),
            Map.entry("careful_mint", 300),
            Map.entry("ability_capsule", 600),
            Map.entry("ability_patch", 1500),
            Map.entry("power_bracer", 250),
            Map.entry("power_belt", 250),
            Map.entry("power_lens", 250),
            Map.entry("power_band", 250),
            Map.entry("power_anklet", 250),
            Map.entry("power_weight", 250),

            // Evolution Items
            Map.entry("fire_stone", 200),
            Map.entry("water_stone", 200),
            Map.entry("thunder_stone", 200),
            Map.entry("leaf_stone", 200),
            Map.entry("moon_stone", 250),
            Map.entry("sun_stone", 250),
            Map.entry("shiny_stone", 300),
            Map.entry("dusk_stone", 300),
            Map.entry("dawn_stone", 300),
            Map.entry("ice_stone", 250),
            Map.entry("electirizer", 400),
            Map.entry("magmarizer", 400),
            Map.entry("protector", 400),
            Map.entry("reaper_cloth", 400),
            Map.entry("dragon_scale", 400),
            Map.entry("prism_scale", 400),
            Map.entry("dubious_disc", 400),
            Map.entry("upgrade", 350),
            Map.entry("kings_rock", 350),
            Map.entry("metal_coat", 350),
            Map.entry("razor_fang", 350),
            Map.entry("razor_claw", 350),
            Map.entry("deep_sea_tooth", 300),
            Map.entry("deep_sea_scale", 300),
            Map.entry("oval_stone", 200),
            Map.entry("cracked_pot", 250),
            Map.entry("link_cable", 350),

            // Balls & Medicine
            Map.entry("master_ball", 25000), // Game-breaking ultimate ball
            Map.entry("beast_ball", 1000),
            Map.entry("cherish_ball", 800),
            Map.entry("luxury_ball", 200),
            Map.entry("heavy_ball", 200),
            Map.entry("fast_ball", 200),
            Map.entry("moon_ball", 200),
            Map.entry("dream_ball", 200),
            Map.entry("revival_herb", 150),
            Map.entry("max_elixir", 150),

            // Cosmetics & Prestige
            Map.entry("cosmetic_shiny_aura", 5000),
            Map.entry("cosmetic_particle_trail", 8000),
            Map.entry("cosmetic_victory_fanfare", 4000),
            Map.entry("weekly_challenge_reroll", 1000),
            Map.entry("title_tower_champion", 10000),
            Map.entry("title_tower_legend", 20000),

            // Type Gems
            Map.entry("normal_gem", 150),
            Map.entry("fire_gem", 150),
            Map.entry("water_gem", 150),
            Map.entry("electric_gem", 150),
            Map.entry("grass_gem", 150),
            Map.entry("ice_gem", 150),
            Map.entry("fighting_gem", 150),
            Map.entry("poison_gem", 150),
            Map.entry("ground_gem", 150),
            Map.entry("flying_gem", 150),
            Map.entry("psychic_gem", 150),
            Map.entry("bug_gem", 150),
            Map.entry("rock_gem", 150),
            Map.entry("ghost_gem", 150),
            Map.entry("dragon_gem", 150),
            Map.entry("dark_gem", 150),
            Map.entry("steel_gem", 150),
            Map.entry("fairy_gem", 150),

            // Additional Poke Balls
            Map.entry("poke_ball", 20),
            Map.entry("great_ball", 40),
            Map.entry("ultra_ball", 80),
            Map.entry("premier_ball", 80),
            Map.entry("heal_ball", 80),
            Map.entry("net_ball", 80),
            Map.entry("nest_ball", 80),
            Map.entry("dive_ball", 80),
            Map.entry("dusk_ball", 80),
            Map.entry("timer_ball", 80),
            Map.entry("quick_ball", 80),
            Map.entry("repeat_ball", 80),
            Map.entry("lure_ball", 200),
            Map.entry("level_ball", 200),
            Map.entry("friend_ball", 200),
            Map.entry("love_ball", 200),
            Map.entry("safari_ball", 300),
            Map.entry("sport_ball", 300)
    );

    public static final Map<String, Integer> WEEKLY_STOCK_LIMITS = Map.of(
            "ability_capsule", 5,
            "ability_patch", 2,
            "master_ball", 1,
            "gold_bottle_cap", 2,
            "weekly_challenge_reroll", 1
    );

    private TowerRewardManager() {}

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor, boolean isTrueRun, int turnsThisFloor, int faintsThisFloor) {
        var bpCfg = com.vitwo.config.TowerConfig.getInstance().bp;
        int baseBp = bpCfg.perFloor;
        if (floor % 5 == 0) baseBp += bpCfg.bossBonus;
        if (floor >= 90 && floor < 100) baseBp += 350; // Elite Four floors
        if (floor == 25) baseBp += bpCfg.checkpoint25Bonus;
        else if (floor == 50) baseBp += bpCfg.checkpoint50Bonus;
        else if (floor == 75) baseBp += bpCfg.checkpoint75Bonus;
        else if (floor == 100) baseBp += (isTrueRun ? bpCfg.clear100TrueRun : bpCfg.clear100CheckpointRun);

        // Calculate Battle Grade
        String grade;
        float gradeBonusMultiplier = 0.0f;
        if (faintsThisFloor == 0 && turnsThisFloor <= 6) {
            grade = "S";
            gradeBonusMultiplier = 0.50f; // +50% BP
        } else if (faintsThisFloor == 0 && turnsThisFloor <= 12) {
            grade = "A";
            gradeBonusMultiplier = 0.25f; // +25% BP
        } else if (faintsThisFloor <= 1) {
            grade = "B";
            gradeBonusMultiplier = 0.10f; // +10% BP
        } else {
            grade = "C";
        }

        if (playerA != null) {
            float mult = isTrueRun ? (float) bpCfg.trueRunMultiplier : TowerPlayerDataManager.getInstance().getCheckpointBpMultiplier(playerA.getUuid());
            float prestigeMult = TowerPlayerDataManager.getInstance().getPrestigeBpMultiplier(playerA.getUuid());
            int rankBonusBp = (int) Math.ceil(baseBp * gradeBonusMultiplier);
            int finalBp = Math.max(1, (int) Math.ceil((baseBp + rankBonusBp) * mult * prestigeMult));
            TowerPlayerDataManager.getInstance().addBp(playerA.getUuid(), finalBp);
            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(playerA.getUuid());
            partyOpt.ifPresent(party -> party.addBpEarnedInRun(finalBp));
            playerA.sendMessage(Text.literal("§6[CobbleTower] §a+" + finalBp + " BP §7(Grade: §e" + grade + "§7) §f— Total BP: §e" + TowerPlayerDataManager.getInstance().getBp(playerA.getUuid()) + " BP"), false);
            ServerPlayNetworking.send(playerA, new TowerBattleGradeS2CPacket(floor, grade, rankBonusBp, turnsThisFloor, faintsThisFloor));
        }
        if (playerB != null) {
            float mult = isTrueRun ? (float) bpCfg.trueRunMultiplier : TowerPlayerDataManager.getInstance().getCheckpointBpMultiplier(playerB.getUuid());
            float prestigeMult = TowerPlayerDataManager.getInstance().getPrestigeBpMultiplier(playerB.getUuid());
            int rankBonusBp = (int) Math.ceil(baseBp * gradeBonusMultiplier);
            int finalBp = Math.max(1, (int) Math.ceil((baseBp + rankBonusBp) * mult * prestigeMult));
            TowerPlayerDataManager.getInstance().addBp(playerB.getUuid(), finalBp);
            playerB.sendMessage(Text.literal("§6[CobbleTower] §a+" + finalBp + " BP §7(Grade: §e" + grade + "§7) §f— Total BP: §e" + TowerPlayerDataManager.getInstance().getBp(playerB.getUuid()) + " BP"), false);
            ServerPlayNetworking.send(playerB, new TowerBattleGradeS2CPacket(floor, grade, rankBonusBp, turnsThisFloor, faintsThisFloor));
        }
    }

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor, boolean isTrueRun) {
        grantFloorReward(playerA, playerB, floor, isTrueRun, 5, 0);
    }

    public ItemStack getCobblemonItem(String name, int count) {
        Identifier id = Identifier.of("cobblemon", name);
        if (Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id), Math.max(1, count));
        }
        return new ItemStack(Items.DIAMOND, Math.max(1, count));
    }

    public static Species getBaseSpecies(Species species) {
        if (species == null) return null;
        Species curr = species;
        while (curr.getPreEvolution() != null && curr.getPreEvolution().getSpecies() != null) {
            curr = curr.getPreEvolution().getSpecies();
        }
        return curr;
    }

    public static String extractRegionalAspect(Pokemon mon) {
        if (mon == null) return "";
        for (String aspect : mon.getAspects()) {
            String lower = aspect.toLowerCase(Locale.ROOT);
            if (lower.contains("hisui") || lower.contains("alola") || lower.contains("galar") || lower.contains("paldea") || lower.contains("bloodmoon")) {
                return lower;
            }
        }
        if (mon.getForm() != null && mon.getForm().getName() != null) {
            String formName = mon.getForm().getName().toLowerCase(Locale.ROOT);
            if (formName.contains("hisui")) return "hisui";
            if (formName.contains("alola")) return "alola";
            if (formName.contains("galar")) return "galar";
            if (formName.contains("paldea")) return "paldea";
        }
        return "";
    }

    public static boolean isLegendaryOrMythical(Species species) {
        if (species == null) return false;
        String name = species.getName().toLowerCase(Locale.ROOT);
        var restricted = com.vitwo.config.TowerConfig.getInstance().restrictedPokemon.restrictedList;
        if (restricted != null && restricted.contains(name)) return true;

        var eggGroups = species.getEggGroups();
        if (eggGroups != null) {
            for (EggGroup group : eggGroups) {
                if (group.name().equalsIgnoreCase("UNDISCOVERED") || group.name().equalsIgnoreCase("NO_EGGS_DISCOVERED")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void populateCandidatesFromTrainer(List<GachaPokemonCandidate> pool, String trainerId, int floor) {
        if (trainerId == null || trainerId.isBlank()) {
            trainerId = TrainerPool.getRctTrainerIdForFloor(floor);
        }
        List<Pokemon> team = HellModeTeamLoader.createTeamFromTrainerId(trainerId, 50);
        if (team == null || team.isEmpty()) return;

        for (Pokemon mon : team) {
            if (mon == null || mon.getSpecies() == null) continue;
            Species origSpecies = mon.getSpecies();
            String origName = origSpecies.getName().toLowerCase(Locale.ROOT);
            boolean exists = pool.stream().anyMatch(c -> c.speciesName().equalsIgnoreCase(origName));
            if (exists) continue;

            Species baseSpecies = getBaseSpecies(origSpecies);
            String baseName = baseSpecies != null ? baseSpecies.getName().toLowerCase(Locale.ROOT) : origName;
            String regionalAspect = extractRegionalAspect(mon);
            boolean isLegend = isLegendaryOrMythical(origSpecies);
            boolean isShiny = mon.getShiny();
            String t1 = mon.getPrimaryType() != null ? mon.getPrimaryType().getName() : "Normal";
            String t2 = mon.getSecondaryType() != null ? mon.getSecondaryType().getName() : "";
            String display = mon.getDisplayName(false).getString();

            pool.add(GachaPokemonCandidate.of(pool.size(), origName, display, baseName, regionalAspect, t1, t2, isLegend, isShiny));
        }
    }

    private void enrichBossGachaPool(List<GachaPokemonCandidate> pool, int floor) {
        if (floor >= 90) {
            // Floors 90 - 100: Grand Elite Four & Cynthia Apex Pool
            String[] apexBosses = {
                    // High Legends & Mythicals
                    "mewtwo", "rayquaza", "giratina", "dialga", "palkia", "arceus", "koraidon", "miraidon", "darkrai", "mew", "deoxys", "jirachi", "kyogre", "groudon",
                    // Sub-Legends & Paradox
                    "urshifu", "roaring_moon", "iron_valiant", "ogerpon", "chien_pao", "chi_yu", "ting_lu", "wo_chien", "zapdos", "moltres", "articuno", "raikou", "entei", "suicune", "latios", "latias",
                    // Champion & E4 Aces
                    "garchomp", "metagross", "dragonite", "salamence", "tyranitar", "dragapult", "baxcalibur", "volcarona", "lucario", "togekiss", "spiritomb", "milotic", "hydreigon", "kingambit", "gengar", "scizor",
                    // Starters
                    "charizard", "greninja", "skeledirge", "meowscarada", "blaziken", "swampert", "sceptile", "infernape"
            };
            for (String mon : apexBosses) {
                boolean exists = pool.stream().anyMatch(c -> c.speciesName().equalsIgnoreCase(mon));
                if (!exists) {
                    var sp = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByName(mon);
                    var base = getBaseSpecies(sp);
                    String baseName = base != null ? base.getName().toLowerCase(Locale.ROOT) : mon;
                    String t1 = (sp != null && sp.getPrimaryType() != null) ? sp.getPrimaryType().getName() : "Normal";
                    String t2 = (sp != null && sp.getSecondaryType() != null) ? sp.getSecondaryType().getName() : "";
                    boolean isLegend = isLegendaryOrMythical(sp);
                    pool.add(GachaPokemonCandidate.of(pool.size(), mon, capitalize(mon), baseName, "", t1, t2, isLegend, false));
                }
            }
        }
    }

    // ==========================================
    // 🎰 CS:GO STYLE GACHA ROULETTE SYSTEMS
    // ==========================================

    private final Map<UUID, List<GachaPokemonCandidate>> activePokemonGachaPools = new ConcurrentHashMap<>();
    private final Map<UUID, List<GachaItemCandidate>> activeItemGachaPools = new ConcurrentHashMap<>();

    public void triggerPokemonGacha(TowerParty party, MinecraftServer server, int floor) {
        if (party == null || server == null) return;
        String bossName = party.getCurrentBossName();

        // 1. Drain all encountered Pokemon since last gacha
        List<GachaPokemonCandidate> pool = party.drainEncounteredCandidates(floor);

        // 2. If pool is small, populate from recent floors
        if (pool.size() < 6) {
            int startFloor = Math.max(1, floor - 4);
            for (int f = startFloor; f <= floor; f++) {
                String tid = TrainerPool.getRctTrainerIdForFloor(f);
                populateCandidatesFromTrainer(pool, tid, f);
            }
        }

        // 3. For Late Game (Floors 90-100), enrich the pool with the Apex Legendaries, Mythicals & Champion Aces
        enrichBossGachaPool(pool, floor);

        if (pool.isEmpty()) {
            pool.addAll(List.of(
                    GachaPokemonCandidate.of(0, "garchomp", "Garchomp", "gible", "", "Dragon", "Ground", false, false),
                    GachaPokemonCandidate.of(1, "lucario", "Lucario", "riolu", "", "Fighting", "Steel", false, false),
                    GachaPokemonCandidate.of(2, "charizard", "Charizard", "charmander", "", "Fire", "Flying", false, false),
                    GachaPokemonCandidate.of(3, "greninja", "Greninja", "froakie", "", "Water", "Dark", false, false),
                    GachaPokemonCandidate.of(4, "tyranitar", "Tyranitar", "larvitar", "", "Rock", "Dark", false, false),
                    GachaPokemonCandidate.of(5, "metagross", "Metagross", "beldum", "", "Steel", "Psychic", false, false)
            ));
        }

        // 4. Weighted Rarity Selection for the Winning Pokémon (0.6% High Legend, 4% Low Legend, 12% Pseudo, 25% Starter, 58.4% Common)
        Random rng = new Random();
        List<GachaPokemonCandidate> highLegends = pool.stream().filter(c -> c.rarity() == PokemonRarity.HIGH_LEGEND || c.rarity() == PokemonRarity.MYTHICAL).toList();
        List<GachaPokemonCandidate> lowLegends = pool.stream().filter(c -> c.rarity() == PokemonRarity.LOW_LEGEND).toList();
        List<GachaPokemonCandidate> pseudos = pool.stream().filter(c -> c.rarity() == PokemonRarity.PSEUDO_LEGENDARY).toList();
        List<GachaPokemonCandidate> starters = pool.stream().filter(c -> c.rarity() == PokemonRarity.STARTER).toList();
        List<GachaPokemonCandidate> commons = pool.stream().filter(c -> c.rarity() == PokemonRarity.COMMON).toList();

        GachaPokemonCandidate winningBase;
        float roll = rng.nextFloat();
        if (roll < 0.006f && !highLegends.isEmpty()) {
            winningBase = highLegends.get(rng.nextInt(highLegends.size())); // 0.6% Holy Grail Tier
        } else if (roll < 0.046f && !lowLegends.isEmpty()) {
            winningBase = lowLegends.get(rng.nextInt(lowLegends.size())); // 4% Sub-Legendary
        } else if (roll < 0.166f && !pseudos.isEmpty()) {
            winningBase = pseudos.get(rng.nextInt(pseudos.size())); // 12% Pseudo-Legendary
        } else if (roll < 0.416f && !starters.isEmpty()) {
            winningBase = starters.get(rng.nextInt(starters.size())); // 25% Starter
        } else if (!commons.isEmpty()) {
            winningBase = commons.get(rng.nextInt(commons.size()));
        } else {
            winningBase = pool.get(rng.nextInt(pool.size()));
        }

        // 5. Construct an extended 50-item CS:GO carousel reel from pool, placing winningBase at target winning index
        int winningIndex = 38 + rng.nextInt(6); // Slot 38-43
        List<GachaPokemonCandidate> reel = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            GachaPokemonCandidate cardBase = (i == winningIndex) ? winningBase : pool.get(rng.nextInt(pool.size()));
            reel.add(new GachaPokemonCandidate(
                    i,
                    cardBase.speciesName(),
                    cardBase.displayName(),
                    cardBase.baseSpecies(),
                    cardBase.formAspect(),
                    cardBase.primaryType(),
                    cardBase.secondaryType(),
                    cardBase.rarity(),
                    cardBase.isLegendary(),
                    cardBase.isShiny()
            ));
        }

        boolean isShinyWinner = rng.nextFloat() < 0.01f; // 1% Shiny chance

        // Pre-roll IVs (with bonus stats for higher floors)
        int[] rolledIvs = new int[6];
        int minIv = floor >= 90 ? 25 : (floor >= 50 ? 20 : (floor >= 25 ? 15 : 10));
        for (int i = 0; i < 6; i++) {
            // Chance of 31 is higher on milestone floors
            if (rng.nextFloat() < (0.25f + (floor * 0.005f))) {
                rolledIvs[i] = 31;
            } else {
                rolledIvs[i] = minIv + rng.nextInt(32 - minIv);
            }
        }

        OpenPokemonGachaS2CPacket packet = new OpenPokemonGachaS2CPacket(floor, bossName, reel, winningIndex, isShinyWinner, rolledIvs);

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        if (leader != null) {
            activePokemonGachaPools.put(leader.getUuid(), reel);
            ServerPlayNetworking.send(leader, packet);
        }
        if (!party.isSolo() && party.getMemberId() != null) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(party.getMemberId());
            if (member != null) {
                activePokemonGachaPools.put(member.getUuid(), reel);
                ServerPlayNetworking.send(member, packet);
            }
        }
    }

    public void handleGachaPokemonClaim(ServerPlayerEntity player, ClaimGachaPokemonC2SPacket packet) {
        if (player == null || player.getServer() == null || packet == null) return;
        List<GachaPokemonCandidate> reel = activePokemonGachaPools.remove(player.getUuid());

        int winIdx = packet.winningCandidateIndex();
        GachaPokemonCandidate winner = (reel != null && winIdx >= 0 && winIdx < reel.size())
                ? reel.get(winIdx)
                : null;

        if (winner == null) {
            // Fallback candidate if reel expired
            winner = GachaPokemonCandidate.of(0, "garchomp", "Garchomp", "gible", "", "Dragon", "Ground", false, false);
        }

        int floor = packet.floor();

        try {
            String baseSpecies = winner.baseSpecies();
            String formStr = (winner.formAspect() != null && !winner.formAspect().isBlank()) ? (" " + winner.formAspect()) : "";
            String parseStr = baseSpecies + formStr + " level=1" + (packet.isShiny() ? " shiny=true" : "");
            Pokemon babyMon = PokemonProperties.Companion.parse(parseStr).create();

            List<Stat> permanentStats = new ArrayList<>(Stats.Companion.getPERMANENT());
            int perfectCount = 0;
            for (int i = 0; i < Math.min(6, permanentStats.size()); i++) {
                int ivVal = (packet.rolledIvs() != null && packet.rolledIvs().length > i)
                        ? Math.min(31, Math.max(0, packet.rolledIvs()[i]))
                        : 31;
                babyMon.getIvs().set(permanentStats.get(i), ivVal);
                if (ivVal == 31) perfectCount++;
            }

            babyMon.setLevel(1);
            babyMon.setShiny(packet.isShiny());
            fullHeal(babyMon);

            var partyStorage = Cobblemon.INSTANCE.getStorage().getParty(player);
            boolean addedToParty = false;
            if (partyStorage != null) {
                addedToParty = partyStorage.add(babyMon);
            }

            if (!addedToParty) {
                var pcStorage = Cobblemon.INSTANCE.getStorage().getPC(player);
                if (pcStorage != null) {
                    pcStorage.add(babyMon);
                }
            }

            String formPrefix = (winner.formAspect() != null && !winner.formAspect().isBlank()) ? (" [" + capitalize(winner.formAspect()) + "] ") : " ";
            String shinyTag = packet.isShiny() ? " §6✨ [SHINY]" : "";
            String destText = addedToParty ? "§a(Added to Party!)" : "§e(Party full, safely sent to PC Box!)";
            player.sendMessage(Text.literal("§d★ [CobbleTower Gacha] §fCongratulations! You won §e" + capitalize(baseSpecies) + formPrefix + shinyTag + " §7(From §f" + winner.displayName() + "§7) §a" + perfectCount + "/6 Best 31 IVs! " + destText), false);
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Grand Server Broadcast for Ultra Rare Jackpots (High Legend / Mythical, Shiny 1%, or 5-6 Max 31 IVs)
            boolean isHighJackpot = winner.rarity() == PokemonRarity.HIGH_LEGEND || winner.rarity() == PokemonRarity.MYTHICAL;
            if (isHighJackpot || packet.isShiny() || perfectCount >= 5) {
                broadcastGrandJackpot(player.getServer(), player.getName().getString(), capitalize(baseSpecies) + formPrefix, packet.isShiny(), perfectCount, floor);
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Failed to create gacha pokemon for player {}: ", player.getName().getString(), t);
            giveItemToPlayer(player, new ItemStack(Items.DIAMOND_BLOCK, 4), floor);
        }
    }

    public static void broadcastGrandJackpot(MinecraftServer server, String playerName, String pokemonName, boolean isShiny, int maxIvs, int floor) {
        if (server == null) return;
        String shinyTag = isShiny ? " §6✨ [SHINY]" : "";
        Text line1 = Text.literal("§6§m-----------------------------------------------------");
        Text line2 = Text.literal("§6§l✦ [COBBLETOWER BROADCAST] §eGRAND JACKPOT CELEBRATION! ✦");
        Text line3 = Text.literal("§fTrainer §e§l" + playerName + " §fhas struck gold on Floor §6" + floor + "§f!");
        Text line4 = Text.literal("§fAwarded: §d§l" + pokemonName + shinyTag + " §f| Stats: §a§l" + maxIvs + "/6 Best 31 IVs§f!");
        Text line5 = Text.literal("§6§l✦ Congratulations to the luckiest Champion on the server! ✦");
        Text line6 = Text.literal("§6§m-----------------------------------------------------");

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(line1, false);
            p.sendMessage(line2, false);
            p.sendMessage(line3, false);
            p.sendMessage(line4, false);
            p.sendMessage(line5, false);
            p.sendMessage(line6, false);
            p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public void triggerItemGacha(TowerParty party, MinecraftServer server, int floor) {
        if (party == null || server == null) return;
        Random rng = new Random();

        // 4-Tier CS:GO Item & BP Pool
        List<GachaItemCandidate> pool = List.of(
                // Tier 3: Gold / Jackpot (3-5%)
                new GachaItemCandidate(0, "master_ball", "Master Ball", "ball", 0, 1, 3, 0xFFFFD700),
                new GachaItemCandidate(1, "gold_bottle_cap", "Gold Bottle Cap", "train", 0, 1, 3, 0xFFFFD700),
                new GachaItemCandidate(2, "ability_patch", "Ability Patch", "train", 0, 1, 3, 0xFFFF55FF),
                new GachaItemCandidate(3, "bp_jackpot", "1,500 BP Jackpot", "bp", 1500, 1, 3, 0xFFFFD700),

                // Tier 2: Purple / Rare (15-20%)
                new GachaItemCandidate(4, "bottle_cap", "Bottle Cap", "train", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(5, "ability_capsule", "Ability Capsule", "train", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(6, "choice_scarf", "Choice Scarf", "battle", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(7, "choice_band", "Choice Band", "battle", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(8, "choice_specs", "Choice Specs", "battle", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(9, "life_orb", "Life Orb", "battle", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(10, "focus_sash", "Focus Sash", "battle", 0, 1, 2, 0xFF9B59B6),
                new GachaItemCandidate(11, "bp_major", "500 BP Reward", "bp", 500, 1, 2, 0xFF9B59B6),

                // Tier 1: Blue / Uncommon (35%)
                new GachaItemCandidate(12, "hp_up", "HP Up x3", "train", 0, 3, 1, 0xFF1E90FF),
                new GachaItemCandidate(13, "protein", "Protein x3", "train", 0, 3, 1, 0xFF1E90FF),
                new GachaItemCandidate(14, "carbos", "Carbos x3", "train", 0, 3, 1, 0xFF1E90FF),
                new GachaItemCandidate(15, "exp_candy_xl", "EXP Candy XL x3", "train", 0, 3, 1, 0xFF1E90FF),
                new GachaItemCandidate(16, "adamant_mint", "Adamant Mint", "train", 0, 1, 1, 0xFF1E90FF),
                new GachaItemCandidate(17, "jolly_mint", "Jolly Mint", "train", 0, 1, 1, 0xFF1E90FF),
                new GachaItemCandidate(18, "bp_medium", "250 BP Reward", "bp", 250, 1, 1, 0xFF1E90FF),

                // Tier 0: Gray / Common (40%)
                new GachaItemCandidate(19, "rare_candy", "Rare Candy x2", "train", 0, 2, 0, 0xFF7F8C8D),
                new GachaItemCandidate(20, "ultra_ball", "Ultra Ball x5", "ball", 0, 5, 0, 0xFF7F8C8D),
                new GachaItemCandidate(21, "revival_herb", "Revival Herb x3", "medicine", 0, 3, 0, 0xFF7F8C8D),
                new GachaItemCandidate(22, "max_elixir", "Max Elixir x2", "medicine", 0, 2, 0, 0xFF7F8C8D),
                new GachaItemCandidate(23, "exp_candy_l", "EXP Candy L x4", "train", 0, 4, 0, 0xFF7F8C8D),
                new GachaItemCandidate(24, "bp_small", "100 BP Reward", "bp", 100, 1, 0, 0xFF7F8C8D)
        );

        // Build 40-item weighted roulette reel
        List<GachaItemCandidate> reel = new ArrayList<>(40);
        for (int i = 0; i < 40; i++) {
            float roll = rng.nextFloat();
            GachaItemCandidate base;
            if (roll < 0.04f) {
                // Tier 3
                base = pool.get(rng.nextInt(4));
            } else if (roll < 0.22f) {
                // Tier 2
                base = pool.get(4 + rng.nextInt(8));
            } else if (roll < 0.58f) {
                // Tier 1
                base = pool.get(12 + rng.nextInt(7));
            } else {
                // Tier 0
                base = pool.get(19 + rng.nextInt(6));
            }
            reel.add(new GachaItemCandidate(i, base.id(), base.displayName(), base.category(), base.bpAmount(), base.quantity(), base.rarityTier(), base.color()));
        }

        int winningIndex = 30 + rng.nextInt(5);
        OpenItemGachaS2CPacket packet = new OpenItemGachaS2CPacket(floor, reel, winningIndex);

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        if (leader != null) {
            activeItemGachaPools.put(leader.getUuid(), reel);
            ServerPlayNetworking.send(leader, packet);
        }
        if (!party.isSolo() && party.getMemberId() != null) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(party.getMemberId());
            if (member != null) {
                activeItemGachaPools.put(member.getUuid(), reel);
                ServerPlayNetworking.send(member, packet);
            }
        }
    }

    public void handleGachaItemClaim(ServerPlayerEntity player, ClaimItemGachaC2SPacket packet) {
        if (player == null || player.getServer() == null || packet == null) return;
        List<GachaItemCandidate> reel = activeItemGachaPools.remove(player.getUuid());

        int winIdx = packet.winningIndex();
        GachaItemCandidate winner = (reel != null && winIdx >= 0 && winIdx < reel.size())
                ? reel.get(winIdx)
                : null;

        if (winner == null) {
            winner = new GachaItemCandidate(0, "bp_small", "100 BP", "bp", 100, 1, 0, 0xFF7F8C8D);
        }

        int floor = packet.floor();

        if ("bp".equalsIgnoreCase(winner.category()) || winner.bpAmount() > 0) {
            int bp = Math.max(winner.bpAmount(), 100);
            TowerPlayerDataManager.getInstance().addBp(player.getUuid(), bp);
            player.sendMessage(Text.literal("§6★ [CobbleTower Gacha] §fYou received §e+" + bp + " BP §f(Total BP: §a" + TowerPlayerDataManager.getInstance().getBp(player.getUuid()) + "§f)!"), false);
        } else {
            ItemStack stack = getCobblemonItem(winner.id(), winner.quantity());
            giveItemToPlayer(player, stack, floor);
            player.sendMessage(Text.literal("§6★ [CobbleTower Gacha] §fYou received §e" + winner.displayName() + "§f!"), false);
        }
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void fullHeal(Pokemon mon) {
        if (mon == null) return;
        try {
            mon.heal();
            if (mon.getMoveSet() != null) {
                mon.getMoveSet().heal();
                for (Move move : mon.getMoveSet()) {
                    if (move != null) {
                        move.setCurrentPp(move.getMaxPp());
                        move.update();
                    }
                }
                mon.getMoveSet().update();
            }
        } catch (Throwable t) {
            LOGGER.warn("[CobbleTower] Failed to full-heal pokemon: {}", t.getMessage());
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // ==========================================
    // 🛠️ HEALING & REST STATION HELPERS
    // ==========================================

    public void applyFullTeamRest(ServerPlayerEntity player) {
        if (player == null) return;
        try {
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party != null) {
                for (Pokemon mon : party) {
                    if (mon != null) {
                        fullHeal(mon);
                        try {
                            party.onPokemonChanged(mon);
                        } catch (Throwable ignored) {}
                    }
                }
                try {
                    party.sendTo(player);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Error applying full team rest for player {}", player.getName().getString(), t);
        }
        player.sendMessage(Text.literal("§a[CobbleTower] §fYour Pokémon team has been fully healed (100% HP, 100% PP & status cured)!"), false);
    }

    public void handleBpPurchase(ServerPlayerEntity player, String itemId, int quantity) {
        if (player == null || itemId == null) return;
        quantity = Math.max(1, Math.min(64, quantity));

        Integer unitPrice = BP_SHOP_PRICES.get(itemId);
        if (unitPrice == null) {
            player.sendMessage(Text.literal("§c[BP Shop] Invalid item selection."), false);
            return;
        }

        int totalPrice = unitPrice * quantity;
        var profile = TowerPlayerDataManager.getInstance().getProfile(player.getUuid());

        // 1. Tier Unlock Validation
        boolean isSilverTier = List.of("ability_capsule", "bottle_cap", "toxic_orb", "flame_orb", "eviolite", "pp_up").contains(itemId);
        boolean isGoldTier = List.of("ability_patch", "gold_bottle_cap", "tera_shard_stellar", "master_ball", "pp_max", "title_tower_champion").contains(itemId);
        boolean isPlatinumTier = List.of("cosmetic_shiny_aura", "cosmetic_particle_trail", "cosmetic_victory_fanfare", "weekly_challenge_reroll", "title_tower_legend").contains(itemId);

        if (isSilverTier && profile.highestFloorTrueRun < 50) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Silver Tier unlocks after clearing Floor 50 in True Run."), false);
            return;
        }

        if (isGoldTier && profile.highestFloorTrueRun < 100) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Gold Tier unlocks after conquering Floor 100 (Master Tower)."), false);
            return;
        }

        if (isPlatinumTier && profile.prestigeLevel < 3) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Platinum Tier unlocks at Prestige Level 3."), false);
            return;
        }

        if (itemId.equals("title_tower_legend") && profile.prestigeLevel < 5) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Title « Tower Legend » requires Prestige Level 5 (Paragon)."), false);
            return;
        }

        // 2. Weekly Stock Limit Validation
        if (WEEKLY_STOCK_LIMITS.containsKey(itemId)) {
            int limit = WEEKLY_STOCK_LIMITS.get(itemId);
            int purchased = TowerPlayerDataManager.getInstance().getWeeklyPurchasedStock(player.getUuid(), itemId);
            if (purchased + quantity > limit) {
                player.sendMessage(Text.literal("§c[BP Shop] Weekly stock limit reached (" + purchased + "/" + limit + ")! Max you can buy now is " + Math.max(0, limit - purchased) + "."), false);
                return;
            }
        }

        boolean success = TowerPlayerDataManager.getInstance().spendBp(player.getUuid(), totalPrice);
        if (!success) {
            player.sendMessage(Text.literal("§c[BP Shop] Insufficient Battle Points! Required: §e" + totalPrice + " BP§c, Balance: §e" + profile.battlePoints + " BP"), false);
            return;
        }

        if (WEEKLY_STOCK_LIMITS.containsKey(itemId)) {
            TowerPlayerDataManager.getInstance().recordStockPurchase(player.getUuid(), itemId, quantity);
        }

        if (itemId.equals("title_tower_champion")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§6★ [BP Shop] Unlocked Title: §e« Tower Champion »§6!"), false);
            return;
        }

        if (itemId.equals("title_tower_legend")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§d★ [BP Shop] Unlocked Apex Title: §b« Tower Legend »§d!"), false);
            return;
        }

        if (itemId.startsWith("cosmetic_")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§d★ [BP Shop] Unlocked & Activated effect: §b" + itemId.replace("cosmetic_", "").replace("_", " ") + "§d!"), false);
            return;
        }

        Identifier cobbleId = Identifier.of("cobblemon", itemId);
        Identifier mcId = Identifier.of("minecraft", itemId);
        ItemStack stack;
        if (Registries.ITEM.containsId(cobbleId)) {
            stack = new ItemStack(Registries.ITEM.get(cobbleId), quantity);
        } else if (Registries.ITEM.containsId(mcId)) {
            stack = new ItemStack(Registries.ITEM.get(mcId), quantity);
        } else {
            stack = new ItemStack(Items.DIAMOND, quantity);
        }

        giveItemToPlayer(player, stack, 1);
        player.sendMessage(Text.literal("§a[BP Shop] Successfully purchased §e" + stack.getName().getString() + (quantity > 1 ? (" x" + quantity) : "") + " §afor §e" + totalPrice + " BP§a! Remaining Balance: §e" + TowerPlayerDataManager.getInstance().getBp(player.getUuid()) + " BP"), false);
        TowerPartyManager.getInstance().syncPlayerState(player);
    }

    public void handleBpPurchase(ServerPlayerEntity player, String itemId) {
        handleBpPurchase(player, itemId, 1);
    }

    private void giveItemToPlayer(ServerPlayerEntity player, ItemStack stack, int floor) {
        if (player == null || stack.isEmpty()) return;
        String itemName = stack.getName().getString();
        int count = stack.getCount();

        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        player.sendMessage(Text.literal("§6[CobbleTower] §aReceived: §e" + itemName + (count > 1 ? (" x" + count) : "")), false);
    }

    public void checkMilestones(ServerPlayerEntity p1, ServerPlayerEntity p2, int floor, boolean isTrueRun) {
        Identifier advId = null;
        if (floor == 10) advId = Identifier.of("vitwo", "poke_tower");
        else if (floor == 25) advId = Identifier.of("vitwo", "great_tower");
        else if (floor == 50) advId = Identifier.of("vitwo", "ultra_tower");
        else if (floor == 75) advId = Identifier.of("vitwo", "floor_75");
        else if (floor == 90) advId = Identifier.of("vitwo", "floor_90");
        else if (floor == 100 && isTrueRun) {
            advId = Identifier.of("vitwo", "master_tower");
            if (p2 != null) {
                grantAdvancement(p1, Identifier.of("vitwo", "duo_conqueror"));
                grantAdvancement(p2, Identifier.of("vitwo", "duo_conqueror"));
            }
        }

        if (advId != null) {
            if (p1 != null) grantAdvancement(p1, advId);
            if (p2 != null) grantAdvancement(p2, advId);
        }
    }

    private void grantAdvancement(ServerPlayerEntity player, Identifier id) {
        if (player == null || player.getServer() == null) return;
        AdvancementEntry entry = player.getServer().getAdvancementLoader().get(id);
        if (entry != null) {
            PlayerAdvancementTracker tracker = player.getAdvancementTracker();
            for (String criterion : entry.value().criteria().keySet()) {
                tracker.grantCriterion(entry, criterion);
            }
        }
    }
}
