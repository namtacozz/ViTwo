package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.network.c2s.BuyBpItemC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class TowerBpShopScreen extends AbstractTowerScreen {
    public static int currentBpBalance = 0;
    private int selectedCategory = 0; // 0=ALL, 1=BATTLE, 2=TRAINING, 3=EVOLUTION, 4=BALLS, 5=COSMETICS
    private String searchQuery = "";
    private TextFieldWidget searchField;
    private int scrollOffset = 0;
    private static final int COLS = 4;
    private static final int VISIBLE_ROWS = 3;
    private ShopEntry selectedEntry = null;
    private int selectedQuantity = 1;
    private boolean showConfirmModal = false;
    private final Map<String, ItemStack> itemStackCache = new HashMap<>();

    public record ShopEntry(
            String id,
            String displayName,
            int rawPrice,
            String categoryKey,
            String categoryDisplay,
            int tier,
            String weeklyLimit,
            String description,
            String battleEffect
    ) {
        public int price() {
            return com.vitwo.reward.TowerRewardManager.BP_SHOP_PRICES.getOrDefault(this.id, this.rawPrice);
        }
    }

    private static final List<ShopEntry> ALL_ENTRIES = List.of(
            // === HELD & BATTLE ITEMS (Category: BATTLE) ===
            new ShopEntry("focus_sash", "Focus Sash", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "A resilient sash held by a Pokémon.",
                    "If holder has full HP and takes a lethal hit, endures with 1 HP remaining."),

            new ShopEntry("choice_scarf", "Choice Scarf", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "A high-speed competitive choice scarf.",
                    "Boosts holder's Speed by 50%, but locks it into using only one move."),

            new ShopEntry("choice_band", "Choice Band", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "A power-enhancing choice band.",
                    "Boosts holder's Physical Attack by 50%, but locks it into using only one move."),

            new ShopEntry("choice_specs", "Choice Specs", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "High-focus magnifying lenses.",
                    "Boosts holder's Special Attack by 50%, but locks it into using only one move."),

            new ShopEntry("life_orb", "Life Orb", 250, "BATTLE", "Held Item", 0, "Unlimited",
                    "A glowing orb containing intense energy.",
                    "Boosts the damage of damaging moves by 30%, losing 10% max HP per attack."),

            new ShopEntry("assault_vest", "Assault Vest", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "A heavy defensive combat vest.",
                    "Boosts Special Defense by 50%, but prevents using Status/Buff moves."),

            new ShopEntry("heavy_duty_boots", "Heavy-Duty Boots", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "Reinforced tactical footwear.",
                    "Completely shields holder from Stealth Rock, Spikes, and Toxic Spikes on switch-in."),

            new ShopEntry("leftovers", "Leftovers", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "Nutrient-rich snack.",
                    "Gradually restores 1/16th of maximum HP at the end of each turn in combat."),

            new ShopEntry("eviolite", "Eviolite", 250, "BATTLE", "Held Item", 1, "Unlimited",
                    "A mysterious crystal that resonates with potential.",
                    "Boosts Defense and Sp. Def by 50% on Pokémon that are capable of still evolving."),

            new ShopEntry("rocky_helmet", "Rocky Helmet", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "A spiked rugged combat helmet.",
                    "Inflicts 1/6th max HP damage to attacker whenever holder is struck by direct-contact moves."),

            new ShopEntry("expert_belt", "Expert Belt", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "A master martial belt.",
                    "Increases damage of all super-effective moves by 20%."),

            new ShopEntry("air_balloon", "Air Balloon", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "A floating helium balloon.",
                    "Grants total immunity to Ground-type attacks and hazards until popped by an offensive move."),

            new ShopEntry("weakness_policy", "Weakness Policy", 200, "BATTLE", "Held Item", 0, "Unlimited",
                    "An insurance card against weaknesses.",
                    "Sharply raises both Attack and Sp. Atk (+2 stages) when struck by a super-effective attack."),

            new ShopEntry("toxic_orb", "Toxic Orb", 150, "BATTLE", "Held Item", 1, "Unlimited",
                    "A toxic poison orb.",
                    "Badly poisons holder at end of turn 1 (activates Guts, Poison Heal, Toxic Boost)."),

            new ShopEntry("flame_orb", "Flame Orb", 150, "BATTLE", "Held Item", 1, "Unlimited",
                    "A fiery combustion orb.",
                    "Inflicts burn on holder at end of turn 1 (activates Guts, Flare Boost, Facade)."),

            new ShopEntry("safety_goggles", "Safety Goggles", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "Protective tactical eyewear.",
                    "Protects holder from powder/spore moves (Spore) and sandstorm/hail weather damage."),

            new ShopEntry("white_herb", "White Herb", 100, "BATTLE", "Held Item", 0, "Unlimited",
                    "A medicinal herb.",
                    "Instantly restores lowered stat stages back to 0 once during battle."),

            new ShopEntry("power_herb", "Power Herb", 100, "BATTLE", "Held Item", 0, "Unlimited",
                    "A rapid energy catalyst herb.",
                    "Allows two-turn charging attacks (Solar Beam, Meteor Beam) to strike in 1 turn."),

            new ShopEntry("mental_herb", "Mental Herb", 100, "BATTLE", "Held Item", 0, "Unlimited",
                    "A focus herb.",
                    "Instantly cures Taunt, Encore, Torment, Attract, and Disable once."),

            new ShopEntry("mirror_herb", "Mirror Herb", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "A reflective mirror herb.",
                    "Copies any positive stat boosts gained by the opposing Pokémon once during battle."),

            new ShopEntry("black_sludge", "Black Sludge", 150, "BATTLE", "Held Item", 0, "Unlimited",
                    "A noxious organic ooze.",
                    "Restores 1/16th HP per turn for Poison-type Pokémon; damages other types."),

            new ShopEntry("scope_lens", "Scope Lens", 100, "BATTLE", "Held Item", 0, "Unlimited",
                    "An optical targeting scope.",
                    "Boosts holder's critical-hit ratio by +1 stage."),

            new ShopEntry("wide_lens", "Wide Lens", 100, "BATTLE", "Held Item", 0, "Unlimited",
                    "A magnifying optical lens.",
                    "Increases accuracy of all moves by 10% (Focus Blast, Stone Edge, Play Rough)."),

            new ShopEntry("tera_shard_stellar", "Stellar Tera Shard", 500, "BATTLE", "Tera Catalyst", 2, "Unlimited",
                    "A glittering crystal of the Terastal phenomenon.",
                    "Unlocks and transforms a Pokémon's Tera Type to the supreme Stellar archetype."),

            // Gems
            new ShopEntry("normal_gem", "Normal Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an ordinary essence.", "Boosts the power of the holder's first Normal-type move by 30%."),
            new ShopEntry("fire_gem", "Fire Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of fire.", "Boosts the power of the holder's first Fire-type move by 30%."),
            new ShopEntry("water_gem", "Water Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of water.", "Boosts the power of the holder's first Water-type move by 30%."),
            new ShopEntry("electric_gem", "Electric Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of electricity.", "Boosts the power of the holder's first Electric-type move by 30%."),
            new ShopEntry("grass_gem", "Grass Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of nature.", "Boosts the power of the holder's first Grass-type move by 30%."),
            new ShopEntry("ice_gem", "Ice Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of ice.", "Boosts the power of the holder's first Ice-type move by 30%."),
            new ShopEntry("fighting_gem", "Fighting Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of combat.", "Boosts the power of the holder's first Fighting-type move by 30%."),
            new ShopEntry("poison_gem", "Poison Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of poison.", "Boosts the power of the holder's first Poison-type move by 30%."),
            new ShopEntry("ground_gem", "Ground Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of earth.", "Boosts the power of the holder's first Ground-type move by 30%."),
            new ShopEntry("flying_gem", "Flying Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of the sky.", "Boosts the power of the holder's first Flying-type move by 30%."),
            new ShopEntry("psychic_gem", "Psychic Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of the mind.", "Boosts the power of the holder's first Psychic-type move by 30%."),
            new ShopEntry("bug_gem", "Bug Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of insects.", "Boosts the power of the holder's first Bug-type move by 30%."),
            new ShopEntry("rock_gem", "Rock Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of stone.", "Boosts the power of the holder's first Rock-type move by 30%."),
            new ShopEntry("ghost_gem", "Ghost Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of spirits.", "Boosts the power of the holder's first Ghost-type move by 30%."),
            new ShopEntry("dragon_gem", "Dragon Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of dragons.", "Boosts the power of the holder's first Dragon-type move by 30%."),
            new ShopEntry("dark_gem", "Dark Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of darkness.", "Boosts the power of the holder's first Dark-type move by 30%."),
            new ShopEntry("steel_gem", "Steel Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of steel.", "Boosts the power of the holder's first Steel-type move by 30%."),
            new ShopEntry("fairy_gem", "Fairy Gem", 50, "BATTLE", "Held Item", 0, "Unlimited", "A gem with an essence of magic.", "Boosts the power of the holder's first Fairy-type move by 30%."),

            // === MINTS & TRAINING (Category: TRAINING) ===
            new ShopEntry("gold_bottle_cap", "Gold Bottle Cap", 350, "TRAINING", "Hyper Training", 2, "Max 2/wk",
                    "Supreme silver & gold bottle cap.",
                    "Maximizes all 6 IV stats (31/31/31/31/31/31) of a Pokémon via Hyper Training."),

            new ShopEntry("bottle_cap", "Bottle Cap", 100, "TRAINING", "Hyper Training", 1, "Unlimited",
                    "A shiny silver bottle cap.",
                    "Maximizes 1 selected IV stat to 31 via Hyper Training."),

            new ShopEntry("pp_max", "PP Max", 200, "TRAINING", "PP Modifier", 1, "Unlimited",
                    "Supreme PP catalyst drink.",
                    "Maximizes the PP of a single selected move to its absolute upper limit (stage 3)."),

            new ShopEntry("pp_up", "PP Up", 80, "TRAINING", "PP Modifier", 0, "Unlimited",
                    "Nutrient drink for move stamina.",
                    "Raises the maximum PP of a single selected move by 20% (1 stage)."),

            new ShopEntry("adamant_mint", "Adamant Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Attack and -10% Sp. Atk."),

            new ShopEntry("modest_mint", "Modest Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Sp. Atk and -10% Attack."),

            new ShopEntry("jolly_mint", "Jolly Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Speed and -10% Sp. Atk."),

            new ShopEntry("timid_mint", "Timid Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Speed and -10% Attack."),

            new ShopEntry("bold_mint", "Bold Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Defense and -10% Attack."),

            new ShopEntry("calm_mint", "Calm Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Sp. Def and -10% Attack."),

            new ShopEntry("brave_mint", "Brave Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Attack and -10% Speed (ideal for Trick Room)."),

            new ShopEntry("quiet_mint", "Quiet Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Sp. Atk and -10% Speed (ideal for Trick Room)."),

            new ShopEntry("impish_mint", "Impish Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Defense and -10% Sp. Atk."),

            new ShopEntry("careful_mint", "Careful Mint", 100, "TRAINING", "Mints", 0, "Unlimited",
                    "Nature modification herb.",
                    "Shifts stat growth to +10% Sp. Def and -10% Sp. Atk."),

            new ShopEntry("ability_capsule", "Ability Capsule", 150, "TRAINING", "Abilities", 1, "Max 5/wk",
                    "Genetic modifier capsule.",
                    "Switches a Pokémon's ability between its primary and secondary standard abilities."),

            new ShopEntry("ability_patch", "Ability Patch", 400, "TRAINING", "Abilities", 2, "Max 2/wk",
                    "Supreme genetic modifier patch.",
                    "Unlocks and assigns the rare Hidden Ability (HA) of a Pokémon."),

            new ShopEntry("rare_candy", "Rare Candy", 30, "TRAINING", "Leveling", 0, "Unlimited",
                    "Sweet energy candy.",
                    "Instantly raises a Pokémon's level by 1."),

            new ShopEntry("exp_candy_xl", "EXP Candy XL", 50, "TRAINING", "Leveling", 0, "Unlimited",
                    "Massive concentration of EXP.",
                    "Grants 30,000 Experience Points to a single Pokémon."),

            new ShopEntry("exp_candy_l", "EXP Candy L", 25, "TRAINING", "Leveling", 0, "Unlimited",
                    "Large concentration of EXP.",
                    "Grants 10,000 Experience Points to a single Pokémon."),

            new ShopEntry("hp_up", "HP Up", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases HP Effort Values (EVs) by +10."),

            new ShopEntry("protein", "Protein", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases Physical Attack Effort Values (EVs) by +10."),

            new ShopEntry("iron", "Iron", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases Physical Defense Effort Values (EVs) by +10."),

            new ShopEntry("calcium", "Calcium", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases Special Attack Effort Values (EVs) by +10."),

            new ShopEntry("zinc", "Zinc", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases Special Defense Effort Values (EVs) by +10."),

            new ShopEntry("carbos", "Carbos", 15, "TRAINING", "EV Training", 0, "Unlimited",
                    "Nutritious drink for Pokémon.",
                    "Increases Speed Effort Values (EVs) by +10."),

            new ShopEntry("power_bracer", "Power Bracer", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Heavy physical training band.",
                    "Adds +8 Physical Attack Effort Values (EVs) per battle."),

            new ShopEntry("power_belt", "Power Belt", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Heavy defensive training belt.",
                    "Adds +8 Physical Defense Effort Values (EVs) per battle."),

            new ShopEntry("power_lens", "Power Lens", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Optical focus training glasses.",
                    "Adds +8 Special Attack Effort Values (EVs) per battle."),

            new ShopEntry("power_band", "Power Band", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Special defensive training band.",
                    "Adds +8 Special Defense Effort Values (EVs) per battle."),

            new ShopEntry("power_anklet", "Power Anklet", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Speed conditioning anklet.",
                    "Adds +8 Speed Effort Values (EVs) per battle."),

            new ShopEntry("power_weight", "Power Weight", 75, "TRAINING", "EV Training", 0, "Unlimited",
                    "Vitality conditioning weight.",
                    "Adds +8 HP Effort Values (EVs) per battle."),

            // === EVOLUTION ITEMS (Category: EVOLUTION) ===
            new ShopEntry("fire_stone", "Fire Stone", 60, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Elemental evolution stone.", "Evolves Eevee, Vulpix, Growlithe, Pansear, Capsakid."),
            new ShopEntry("water_stone", "Water Stone", 60, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Elemental evolution stone.", "Evolves Eevee, Poliwhirl, Shellder, Staryu, Lombre."),
            new ShopEntry("thunder_stone", "Thunder Stone", 60, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Elemental evolution stone.", "Evolves Eevee, Pikachu, Magneton, Eelektrik."),
            new ShopEntry("leaf_stone", "Leaf Stone", 60, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Elemental evolution stone.", "Evolves Eevee, Gloom, Weepinbell, Nuzleaf."),
            new ShopEntry("moon_stone", "Moon Stone", 80, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Lunar evolution stone.", "Evolves Nidorina, Nidorino, Clefairy, Jigglypuff, Munna."),
            new ShopEntry("sun_stone", "Sun Stone", 80, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Solar evolution stone.", "Evolves Gloom, Sunkern, Cottonee, Helioptile."),
            new ShopEntry("shiny_stone", "Shiny Stone", 100, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Dazzling evolution stone.", "Evolves Togetic, Roselia, Minccino, Floette."),
            new ShopEntry("dusk_stone", "Dusk Stone", 100, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Shadow evolution stone.", "Evolves Murkrow, Misdreavus, Lampent, Doublade."),
            new ShopEntry("dawn_stone", "Dawn Stone", 100, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Aurora evolution stone.", "Evolves Male Kirlia and Female Snorunt."),
            new ShopEntry("ice_stone", "Ice Stone", 80, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Glacial evolution stone.", "Evolves Alolan Vulpix, Alolan Sandshrew, Galarian Darumaka, Cetoddle."),
            new ShopEntry("electirizer", "Electirizer", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Electric charge box.", "Triggers evolution of Electabuzz into Electivire."),
            new ShopEntry("magmarizer", "Magmarizer", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Magma energy box.", "Triggers evolution of Magmar into Magmortar."),
            new ShopEntry("protector", "Protector", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Heavy armor plating.", "Triggers evolution of Rhydon into Rhyperior."),
            new ShopEntry("reaper_cloth", "Reaper Cloth", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Spiritual cloth.", "Triggers evolution of Dusclops into Dusknoir."),
            new ShopEntry("dragon_scale", "Dragon Scale", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Tough mythical scale.", "Triggers evolution of Seadra into Kingdra."),
            new ShopEntry("prism_scale", "Prism Scale", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Shimmering rainbow scale.", "Triggers evolution of Feebas into Milotic."),
            new ShopEntry("dubious_disc", "Dubious Disc", 150, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "Questionable data disc.", "Triggers evolution of Porygon2 into Porygon-Z."),
            new ShopEntry("upgrade", "Upgrade", 120, "EVOLUTION", "Trade Item", 0, "Unlimited",
                    "System upgrade disc.", "Triggers evolution of Porygon into Porygon2."),
            new ShopEntry("kings_rock", "King's Rock", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Royal crown stone.", "Triggers evolution of Poliwhirl (Politoed) and Slowpoke (Slowking); 10% flinch on attack."),
            new ShopEntry("metal_coat", "Metal Coat", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Reinforced steel film.", "Triggers evolution of Onix (Steelix) and Scyther (Scizor); boosts Steel attacks by 20%."),
            new ShopEntry("razor_fang", "Razor Fang", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Sharp predator fang.", "Triggers evolution of Gligar into Gliscor; grants 10% flinch chance."),
            new ShopEntry("razor_claw", "Razor Claw", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Sharp serrated claw.", "Triggers evolution of Sneasel into Weavile; boosts critical hit ratio."),
            new ShopEntry("deep_sea_tooth", "Deep Sea Tooth", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Abyssal fang item.", "Triggers evolution of Clamperl into Huntail; doubles Clamperl's Sp. Atk."),
            new ShopEntry("deep_sea_scale", "Deep Sea Scale", 120, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Abyssal scale item.", "Triggers evolution of Clamperl into Gorebyss; doubles Clamperl's Sp. Def."),
            new ShopEntry("oval_stone", "Oval Stone", 80, "EVOLUTION", "Evolution Stone", 0, "Unlimited",
                    "Smooth egg-shaped stone.", "Triggers evolution of Happiny into Chansey."),
            new ShopEntry("cracked_pot", "Cracked Pot", 80, "EVOLUTION", "Evolution Item", 0, "Unlimited",
                    "Antique cracked teapot.", "Triggers evolution of authentic Sinistea into Polteageist."),
            new ShopEntry("link_cable", "Link Cable", 120, "EVOLUTION", "Special Catalyst", 0, "Unlimited",
                    "Mystical linking cable.", "Instantly triggers trade-based evolutions (Kadabra, Machoke, Graveler, Haunter)."),

            // === BALLS & MEDICINE (Category: BALLS) ===
            new ShopEntry("master_ball", "Master Ball", 1500, "BALLS", "Pokéball", 2, "Max 1/wk",
                    "The legendary ultimate Pokéball.",
                    "Catches any wild Pokémon with 100% guaranteed success without fail."),

            new ShopEntry("beast_ball", "Beast Ball", 300, "BALLS", "Pokéball", 0, "Unlimited",
                    "Extradimensional capture sphere.",
                    "Has a 5x catch rate when thrown at Ultra Beasts."),

            new ShopEntry("cherish_ball", "Cherish Ball", 250, "BALLS", "Pokéball", 0, "Unlimited",
                    "Special decorative red ball.",
                    "A rare collector's Pokéball made to commemorate special achievements."),

            new ShopEntry("luxury_ball", "Luxury Ball", 100, "BALLS", "Pokéball", 0, "Unlimited",
                    "Comfortable luxury ball.",
                    "Doubles the rate at which the caught Pokémon gains friendship points."),

            new ShopEntry("heavy_ball", "Heavy Ball", 100, "BALLS", "Pokéball", 0, "Unlimited",
                    "Weight-specialized Pokéball.",
                    "Provides high catch rate bonus against heavy Pokémon (Snorlax, Groudon)."),

            new ShopEntry("fast_ball", "Fast Ball", 100, "BALLS", "Pokéball", 0, "Unlimited",
                    "High-velocity Pokéball.",
                    "Provides high catch rate bonus against Pokémon with base speed 100+."),

            new ShopEntry("moon_ball", "Moon Ball", 100, "BALLS", "Pokéball", 0, "Unlimited",
                    "Crescent moon Pokéball.",
                    "Has a 4x catch rate against Pokémon that evolve via Moon Stone."),

            new ShopEntry("dream_ball", "Dream Ball", 100, "BALLS", "Pokéball", 0, "Unlimited",
                    "Somnolent dream Pokéball.",
                    "Has a 4x catch rate against Pokémon that are currently asleep."),

            new ShopEntry("revival_herb", "Revival Herb", 80, "BALLS", "Medicine", 0, "Unlimited",
                    "Extremely bitter healing herb.",
                    "Revives a fainted Pokémon and completely restores 100% of maximum HP."),

            new ShopEntry("max_elixir", "Max Elixir", 80, "BALLS", "Medicine", 0, "Unlimited",
                    "Nutrient drink for moves.",
                    "Fully restores all PP for all four moves of a single Pokémon."),

            new ShopEntry("poke_ball", "Poké Ball", 5, "BALLS", "Pokéball", 0, "Unlimited", "Standard Pokéball.", "A device for catching wild Pokémon."),
            new ShopEntry("great_ball", "Great Ball", 10, "BALLS", "Pokéball", 0, "Unlimited", "High-performance Pokéball.", "Provides a higher catch rate than a standard Poké Ball."),
            new ShopEntry("ultra_ball", "Ultra Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Ultra-performance Pokéball.", "Provides a higher catch rate than a Great Ball."),
            new ShopEntry("premier_ball", "Premier Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Commemorative Pokéball.", "A rare Pokéball that has the same catch rate as a standard Poké Ball."),
            new ShopEntry("heal_ball", "Heal Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Restoring Pokéball.", "Fully heals the caught Pokémon's HP and status conditions."),
            new ShopEntry("net_ball", "Net Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "More effective at catching Water- and Bug-type Pokémon."),
            new ShopEntry("nest_ball", "Nest Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "More effective against lower-level Pokémon."),
            new ShopEntry("dive_ball", "Dive Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "More effective when catching Pokémon in or on the water."),
            new ShopEntry("dusk_ball", "Dusk Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "More effective at catching Pokémon at night or in caves."),
            new ShopEntry("timer_ball", "Timer Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "Becomes more effective the more turns have passed in battle."),
            new ShopEntry("quick_ball", "Quick Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "Highly effective if used on the very first turn of a battle."),
            new ShopEntry("repeat_ball", "Repeat Ball", 20, "BALLS", "Pokéball", 0, "Unlimited", "Specialized Pokéball.", "More effective when catching a Pokémon species you already own."),
            new ShopEntry("lure_ball", "Lure Ball", 100, "BALLS", "Pokéball", 0, "Unlimited", "Apricorn Pokéball.", "More effective when catching Pokémon hooked by a fishing rod."),
            new ShopEntry("level_ball", "Level Ball", 100, "BALLS", "Pokéball", 0, "Unlimited", "Apricorn Pokéball.", "More effective if your active Pokémon is a higher level than the target."),
            new ShopEntry("friend_ball", "Friend Ball", 100, "BALLS", "Pokéball", 0, "Unlimited", "Apricorn Pokéball.", "Sets the caught Pokémon's friendship base value to 200 immediately."),
            new ShopEntry("love_ball", "Love Ball", 100, "BALLS", "Pokéball", 0, "Unlimited", "Apricorn Pokéball.", "More effective if catching a Pokémon of the opposite gender to yours."),
            new ShopEntry("safari_ball", "Safari Ball", 150, "BALLS", "Pokéball", 0, "Unlimited", "Specialty Pokéball.", "A special Pokéball originally used only in the Safari Zone."),
            new ShopEntry("sport_ball", "Sport Ball", 150, "BALLS", "Pokéball", 0, "Unlimited", "Specialty Pokéball.", "A special Pokéball originally used in the Bug-Catching Contest."),

            // === COSMETICS & PRESTIGE (Category: COSMETICS) ===
            new ShopEntry("cosmetic_shiny_aura", "Shiny Golden Aura", 3000, "COSMETICS", "Aura", 3, "1-time",
                    "Aesthetic Particle Aura.",
                    "Surrounds trainer in radiant glittering golden starlight sparkles."),

            new ShopEntry("cosmetic_particle_trail", "Soul Flame Trail", 5000, "COSMETICS", "Aura", 3, "1-time",
                    "Aesthetic Particle Footsteps.",
                    "Leaves dynamic soul flame embers and luminous ink behind your footsteps."),

            new ShopEntry("cosmetic_victory_fanfare", "Victory Fanfare", 2000, "COSMETICS", "Perk", 3, "1-time",
                    "Aesthetic Audio Perk.",
                    "Plays a triumph fanfare chord and sparkles upon conquering tower floors."),

            new ShopEntry("weekly_challenge_reroll", "Challenge Reroll", 500, "COSMETICS", "Utility", 3, "Max 1/wk",
                    "Weekly Quest Reroll.",
                    "Re-rolls active weekly challenges for fresh BP objectives."),

            new ShopEntry("title_tower_champion", "« Tower Champion » Title", 5000, "COSMETICS", "Title", 2, "1-time",
                    "Prestigious Golden Title.",
                    "Unlocks and displays the gleaming Champion title prefix over your name."),

            new ShopEntry("title_tower_legend", "« Tower Legend » Title", 10000, "COSMETICS", "Title", 3, "Prestige 5",
                    "Supreme Paragon Title.",
                    "Unlocks the mythical violet Paragon title (Requires Tower Prestige Level 5).")
    );

    public TowerBpShopScreen() {
        super(Text.literal("Battle Point Exchange"));
    }

    private List<ShopEntry> getFilteredEntries() {
        return ALL_ENTRIES.stream().filter(e -> {
            // Category Filter (0=ALL, 1=BATTLE, 2=TRAINING, 3=EVOLUTION, 4=BALLS, 5=COSMETICS)
            boolean matchesCat = switch (selectedCategory) {
                case 1 -> "BATTLE".equalsIgnoreCase(e.categoryKey());
                case 2 -> "TRAINING".equalsIgnoreCase(e.categoryKey());
                case 3 -> "EVOLUTION".equalsIgnoreCase(e.categoryKey());
                case 4 -> "BALLS".equalsIgnoreCase(e.categoryKey());
                case 5 -> "COSMETICS".equalsIgnoreCase(e.categoryKey());
                default -> true;
            };
            if (!matchesCat) return false;

            // Search filter
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String q = searchQuery.trim().toLowerCase();
                boolean matchesName = e.displayName().toLowerCase().contains(q);
                boolean matchesDisplay = e.categoryDisplay().toLowerCase().contains(q);
                boolean matchesDesc = e.description().toLowerCase().contains(q);
                boolean matchesEffect = e.battleEffect().toLowerCase().contains(q);
                return matchesName || matchesDisplay || matchesDesc || matchesEffect;
            }
            return true;
        }).toList();
    }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new com.vitwo.network.c2s.RequestHubSyncC2SPacket());
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (showConfirmModal) {
            // Confirmation Modal Buttons
            int modalW = 220;
            int modalH = 100;
            int modalX = centerX - modalW / 2;
            int modalY = centerY - modalH / 2;

            this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§a§lYES, BUY"), btn -> {
                if (selectedEntry != null) {
                    ClientPlayNetworking.send(new BuyBpItemC2SPacket(selectedEntry.id(), selectedQuantity));
                }
                showConfirmModal = false;
                this.clearAndInit();
            }).dimensions(modalX + 15, modalY + 65, 90, 22).build());

            this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§c§lCANCEL"), btn -> {
                showConfirmModal = false;
                this.clearAndInit();
            }).dimensions(modalX + 115, modalY + 65, 90, 22).build());
            return;
        }

        // Search Field
        searchField = new TextFieldWidget(this.textRenderer, centerX - 180, centerY - 118, 140, 18, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("§8🔍 Search items..."));
        searchField.setText(searchQuery);
        searchField.setEditable(true);
        searchField.setFocusUnlocked(true);
        searchField.setChangedListener(text -> {
            searchQuery = text;
            scrollOffset = 0;
            selectedEntry = null;
        });
        this.addDrawableChild(searchField);

        // 6 Category Filter Tabs (ALL, BATTLE, TRAINING, EVOLUTION, BALLS, COSMETICS)
        String[] catLabels = {"ALL", "BATTLE", "TRAIN", "EVO", "BALLS", "COSMETIC"};
        int tabWidth = 57;
        int tabStartX = centerX - 180;
        int tabY = centerY - 95;

        for (int i = 0; i < 6; i++) {
            final int catIndex = i;
            String prefix = (selectedCategory == catIndex) ? "§6§l[" : "§7";
            String suffix = (selectedCategory == catIndex) ? "]" : "";
            this.addDrawableChild(TowerButton.towerBuilder(Text.literal(prefix + catLabels[i] + suffix), btn -> {
                selectedCategory = catIndex;
                scrollOffset = 0;
                selectedEntry = null;
                selectedQuantity = 1;
                this.clearAndInit();
            }).dimensions(tabStartX + (i * (tabWidth + 3)), tabY, tabWidth, 18).build());
        }

        // Quantity Controls ([-] [Qty] [+] [x10] [MAX])
        int qtyY = centerY + 90;
        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§c-"), btn -> {
            selectedQuantity = Math.max(1, selectedQuantity - 1);
        }).dimensions(centerX - 180, qtyY, 20, 20).build());

        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§a+"), btn -> {
            int maxAffordable = selectedEntry != null && selectedEntry.price() > 0 ? Math.max(1, currentBpBalance / selectedEntry.price()) : 64;
            selectedQuantity = Math.min(Math.min(64, maxAffordable), selectedQuantity + 1);
        }).dimensions(centerX - 120, qtyY, 20, 20).build());

        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§e+10"), btn -> {
            int maxAffordable = selectedEntry != null && selectedEntry.price() > 0 ? Math.max(1, currentBpBalance / selectedEntry.price()) : 64;
            selectedQuantity = Math.min(Math.min(64, maxAffordable), selectedQuantity + 10);
        }).dimensions(centerX - 95, qtyY, 32, 20).build());

        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§6MAX"), btn -> {
            if (selectedEntry != null && selectedEntry.price() > 0) {
                selectedQuantity = Math.max(1, Math.min(64, currentBpBalance / selectedEntry.price()));
            } else {
                selectedQuantity = 64;
            }
        }).dimensions(centerX - 60, qtyY, 32, 20).build());

        // Buy Button
        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§a§lBUY ITEM"), btn -> {
            if (selectedEntry != null) {
                int totalCost = selectedEntry.price() * selectedQuantity;
                if (currentBpBalance >= totalCost) {
                    if (selectedQuantity > 1 || selectedEntry.tier() >= 2) {
                        showConfirmModal = true;
                        this.clearAndInit();
                    } else {
                        ClientPlayNetworking.send(new BuyBpItemC2SPacket(selectedEntry.id(), selectedQuantity));
                    }
                }
            }
        }).dimensions(centerX - 22, qtyY, 100, 20).build());

        // Back to Hub Button
        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§fBack to Hub"), btn -> {
            this.client.setScreen(new TowerHubScreen());
        }).dimensions(centerX + 85, qtyY, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Container Window using AbstractTowerScreen theme
        this.renderPanelBackground(context, centerX - 190, centerY - 128, 380, 248);

        super.render(context, mouseX, mouseY, delta);

        // Header Title & Balance
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l❖ BP EXCHANGE SHOP ❖", centerX + 20, centerY - 121, TowerTheme.SECONDARY_GOLD);
        context.drawTextWithShadow(this.textRenderer, "§6BP: §e" + currentBpBalance, centerX + 115, centerY - 121, 0xFFFFFF);

        List<ShopEntry> currentList = getFilteredEntries();
        int maxRows = (int) Math.ceil((double) currentList.size() / COLS);

        // Grid properties
        int gridX = centerX - 180;
        int gridY = centerY - 72;
        int cellW = 88;
        int cellH = 44;
        int gapX = 4;
        int gapY = 4;

        // Draw Scrollbar Indicator
        int scrollbarX = centerX + 175;
        context.fill(scrollbarX, gridY, scrollbarX + 4, gridY + (VISIBLE_ROWS * cellH + (VISIBLE_ROWS - 1) * gapY), 0x55FFFFFF);
        if (maxRows > VISIBLE_ROWS) {
            int thumbH = Math.max(10, (VISIBLE_ROWS * (VISIBLE_ROWS * cellH)) / maxRows);
            int thumbY = gridY + (int)(((float)scrollOffset / (maxRows - VISIBLE_ROWS)) * (VISIBLE_ROWS * cellH + (VISIBLE_ROWS - 1) * gapY - thumbH));
            context.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, TowerTheme.PRIMARY_CYAN);
        }

        ShopEntry hoveredEntry = null;

        // Draw Grid Items
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int actualRow = scrollOffset + row;
            if (actualRow >= maxRows) break;

            for (int col = 0; col < COLS; col++) {
                int index = actualRow * COLS + col;
                if (index >= currentList.size()) break;

                ShopEntry entry = currentList.get(index);
                int cellX = gridX + col * (cellW + gapX);
                int cellY = gridY + row * (cellH + gapY);

                boolean isHovered = mouseX >= cellX && mouseX <= cellX + cellW && mouseY >= cellY && mouseY <= cellY + cellH;
                boolean isSelected = (entry == selectedEntry);

                if (isHovered) {
                    hoveredEntry = entry;
                }

                // Cell background
                int bgColor = isSelected ? 0x8000E5FF : (isHovered ? 0x80304050 : 0x50000000);
                int borderColor = isSelected ? TowerTheme.PRIMARY_CYAN : (isHovered ? TowerTheme.SECONDARY_GOLD : TowerTheme.BEVEL_DARK);

                context.fill(cellX, cellY, cellX + cellW, cellY + cellH, bgColor);
                context.drawBorder(cellX, cellY, cellW, cellH, borderColor);

                // Draw Item Icon
                ItemStack stack = getItemStackForId(entry.id());
                context.drawItem(stack, cellX + cellW / 2 - 8, cellY + 4);

                // Item Name (Shortened if needed)
                String name = entry.displayName();
                if (this.textRenderer.getWidth(name) > cellW - 4) {
                    name = this.textRenderer.trimToWidth(name, cellW - 10) + "...";
                }
                context.drawCenteredTextWithShadow(this.textRenderer, name, cellX + cellW / 2, cellY + 22, 0xFFFFFF);

                // Price
                int priceColor = currentBpBalance >= entry.price() ? 0xFFFFD700 : 0xFFFF5555;
                context.drawCenteredTextWithShadow(this.textRenderer, entry.price() + " BP", cellX + cellW / 2, cellY + 32, priceColor);
            }
        }

        // Draw Selected Item Detail Panel
        if (selectedEntry != null) {
            int panelY = centerY + 70;
            context.fill(centerX - 180, panelY, centerX + 175, panelY + 16, 0x80000000);
            context.drawBorder(centerX - 180, panelY, 355, 16, TowerTheme.BEVEL_DARK);

            String detailStr = "§e" + selectedEntry.displayName() + " §7(" + selectedEntry.price() + " BP) §8| §f" + selectedEntry.battleEffect();
            if (this.textRenderer.getWidth(detailStr) > 345) {
                detailStr = this.textRenderer.trimToWidth(detailStr, 340) + "...";
            }
            context.drawTextWithShadow(this.textRenderer, detailStr, centerX - 175, panelY + 4, 0xFFFFFF);

            // Draw Quantity Indicator inside Qty Box
            context.drawCenteredTextWithShadow(this.textRenderer, "§e" + selectedQuantity, centerX - 140, centerY + 96, 0xFFFFFF);
        }

        // Draw Authentic Vanilla Item Tooltip on Hover
        if (hoveredEntry != null && !showConfirmModal) {
            ItemStack hoveredStack = getItemStackForId(hoveredEntry.id());
            List<Text> tooltipLines = getCustomVanillaTooltip(hoveredStack, hoveredEntry);
            context.drawTooltip(this.textRenderer, tooltipLines, Optional.empty(), mouseX, mouseY);
        }

        // Draw Confirmation Modal
        if (showConfirmModal && selectedEntry != null) {
            int modalW = 240;
            int modalH = 100;
            int modalX = centerX - modalW / 2;
            int modalY = centerY - modalH / 2;

            // Dim backdrop
            context.fill(0, 0, this.width, this.height, 0x99000000);

            // Modal container
            context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xF610151C);
            context.drawBorder(modalX, modalY, modalW, modalH, TowerTheme.PRIMARY_CYAN);
            context.drawBorder(modalX + 1, modalY + 1, modalW - 2, modalH - 2, TowerTheme.PANEL_BORDER_INNER);

            int totalCost = selectedEntry.price() * selectedQuantity;
            context.drawCenteredTextWithShadow(this.textRenderer, "§6§lCONFIRM PURCHASE", centerX, modalY + 10, TowerTheme.SECONDARY_GOLD);
            context.drawCenteredTextWithShadow(this.textRenderer, "§fBuy §e" + selectedQuantity + "x " + selectedEntry.displayName() + "§f?", centerX, modalY + 28, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Total Cost: §6" + totalCost + " BP §7(Balance: §e" + currentBpBalance + " BP§7)", centerX, modalY + 44, 0xCCCCCC);
        }
    }

    private List<Text> getCustomVanillaTooltip(ItemStack stack, ShopEntry entry) {
        List<Text> tooltip = new ArrayList<>();
        String rarityPrefix = switch (entry.tier()) {
            case 3 -> "§d§l[PLATINUM] ";
            case 2 -> "§6§l[GOLD] ";
            case 1 -> "§b§l[SILVER] ";
            default -> "§f";
        };
        tooltip.add(Text.literal(rarityPrefix + "§e" + entry.displayName()));
        tooltip.add(Text.literal("§7Category: §f" + entry.categoryDisplay()));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("§6Price: §e" + entry.price() + " BP"));
        tooltip.add(Text.literal("§7Limit: §f" + entry.weeklyLimit()));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("§a" + entry.battleEffect()));
        return tooltip;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showConfirmModal) return true;
        List<ShopEntry> currentList = getFilteredEntries();
        int maxRows = (int) Math.ceil((double) currentList.size() / COLS);

        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        } else if (verticalAmount < 0) {
            scrollOffset = Math.min(Math.max(0, maxRows - VISIBLE_ROWS), scrollOffset + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfirmModal) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (searchField != null) {
            boolean inSearch = mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth()
                    && mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight();
            searchField.setFocused(inSearch);
            this.setFocused(inSearch ? searchField : null);
            if (inSearch && searchField.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        List<ShopEntry> currentList = getFilteredEntries();
        int maxRows = (int) Math.ceil((double) currentList.size() / COLS);

        int gridX = centerX - 180;
        int gridY = centerY - 72;
        int cellW = 88;
        int cellH = 44;
        int gapX = 4;
        int gapY = 4;

        // Check Grid Item Selection
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int actualRow = scrollOffset + row;
            if (actualRow >= maxRows) break;

            for (int col = 0; col < COLS; col++) {
                int index = actualRow * COLS + col;
                if (index >= currentList.size()) break;

                int cellX = gridX + col * (cellW + gapX);
                int cellY = gridY + row * (cellH + gapY);

                if (mouseX >= cellX && mouseX <= cellX + cellW && mouseY >= cellY && mouseY <= cellY + cellH) {
                    selectedEntry = currentList.get(index);
                    selectedQuantity = 1;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack getItemStackForId(String id) {
        if (id == null) return new ItemStack(Items.BARRIER);
        return itemStackCache.computeIfAbsent(id, k -> {
            Identifier cobblemonId = Identifier.of("cobblemon", k);
            if (Registries.ITEM.containsId(cobblemonId)) {
                return new ItemStack(Registries.ITEM.get(cobblemonId));
            }

            Identifier mcId = Identifier.of("minecraft", k);
            if (Registries.ITEM.containsId(mcId)) {
                return new ItemStack(Registries.ITEM.get(mcId));
            }

            return switch (k) {
                case "rare_candy", "exp_candy_xl", "exp_candy_l", "pp_max", "pp_up" -> new ItemStack(Items.EXPERIENCE_BOTTLE);
                case "hp_up", "protein", "iron", "calcium", "zinc", "carbos" -> new ItemStack(Items.POTION);
                case "gold_bottle_cap" -> new ItemStack(Items.GOLD_INGOT);
                case "bottle_cap" -> new ItemStack(Items.IRON_NUGGET);
                case "master_ball", "beast_ball", "cherish_ball", "luxury_ball", "heavy_ball", "fast_ball", "moon_ball", "dream_ball" -> new ItemStack(Items.ENDER_EYE);
                case "tera_shard_stellar" -> new ItemStack(Items.AMETHYST_SHARD);
                case "fire_stone" -> new ItemStack(Items.BLAZE_POWDER);
                case "water_stone" -> new ItemStack(Items.PRISMARINE_SHARD);
                case "thunder_stone" -> new ItemStack(Items.GLOWSTONE_DUST);
                case "leaf_stone" -> new ItemStack(Items.EMERALD);
                case "moon_stone" -> new ItemStack(Items.ENDER_PEARL);
                case "sun_stone" -> new ItemStack(Items.SUNFLOWER);
                case "shiny_stone", "dawn_stone" -> new ItemStack(Items.DIAMOND);
                case "dusk_stone" -> new ItemStack(Items.OBSIDIAN);
                case "ice_stone" -> new ItemStack(Items.BLUE_ICE);
                case "electirizer", "magmarizer", "protector", "reaper_cloth", "dragon_scale", "prism_scale", "dubious_disc", "upgrade", "kings_rock", "metal_coat", "razor_fang", "razor_claw", "deep_sea_tooth", "deep_sea_scale", "oval_stone", "cracked_pot", "link_cable" -> new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
                case "cosmetic_shiny_aura", "cosmetic_particle_trail" -> new ItemStack(Items.NETHER_STAR);
                case "cosmetic_victory_fanfare" -> new ItemStack(Items.JUKEBOX);
                case "weekly_challenge_reroll" -> new ItemStack(Items.CLOCK);
                case "title_tower_champion", "title_tower_legend" -> new ItemStack(Items.NAME_TAG);
                default -> new ItemStack(Items.EMERALD);
            };
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!showConfirmModal && searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!showConfirmModal && searchField != null && searchField.isFocused()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!showConfirmModal && searchField != null && searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
