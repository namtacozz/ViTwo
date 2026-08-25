# 🗼 CobbleTower — v1.3.0 Definitive

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?logo=minecraft)](https://minecraft.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.16.0+-blue.svg?logo=fabric)](https://fabricmc.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.0+-red.svg)](https://cobblemon.com/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.3.0-orange.svg)](https://github.com/namtacozz/ViTwo/releases)

**CobbleTower** is the ultimate competitive **100-Floor Roguelike Double Battle Tower** engineered for **Cobblemon** on **Minecraft Fabric 1.21.1**.

Climb solo in standard 6v6 Double Battles or team up with a partner in Co-op Duo (3+3), conquer scaled Level Cap tiers, challenge tournament-ready AI teams equipped with 6x31 perfect IVs and optimal EVs, survive tactical floor curses, experience cinematic boss cutscenes, redeem Battle Points (BP) for rare competitive rewards, and ascend through Prestige tiers!

---

## 🌟 Highlights in v1.3.0

* ⚔️ **Full Hell Mode Integration (1,663 Trainers & 861 Team Pool):**
  * Integrated all 1,663 Hell Mode trainer profiles across the entire CobbleVerse modpack with native `GEN_9_DOUBLES` enforcement.
  * Massive merged **861-Team Poketeampool** for the Tower Dimension combining custom milestone bosses with competitive sets.
* 🎬 **Cinematic Boss Floor Cutscenes:**
  * Landmark floors (F.10, F.25, F.50, F.75, F.90, F.91–F.99, F.100) trigger dramatic widescreen letterbox intros, glowing subtitles, boss sound effects, and custom dialogue.
* 🏆 **Global / World Leaderboard & Hall of Fame (`/tower leaderboard`):**
  * Persistent ranking system recording Top 10 fastest Floor 100 True Run completions with time, turn count, and faints.
* ⭐ **Prestige Ascension System (`/tower prestige`):**
  * Conquering Floor 100 allows players to ascend prestige ranks, gaining permanent BP gain multipliers (+5% per rank) and exclusive cosmetic particle auras.
* 📊 **Post-Battle Performance Ratings (Rank S/A/B/C/D):**
  * Arcade-style grade evaluations displayed on battle completion with bonus BP rewards (+25% for Flawless Rank S).
* 🔒 **Per-World Isolated Data Storage:**
  * Progress, checkpoints, and profiles are saved cleanly inside `saves/<world>/cobbletower_data/`, preventing cross-world leakage.

---

## ⚔️ Double Battle Architecture

### 1. Solo Mode (6v6 Double Battle)
* 1 Player commands both active field positions with their 6-Pokémon competitive team in standard 2v2 VGC doubles.

### 2. Co-op Duo Mode (3+3 Merged Team)
* 2 Players form a party via Hub menu or `/tower duo <player>`.
* Each player contributes 3 Pokémon into a merged 6-Pokémon squad on the field.
* **2-Slot Takeover**: If a teammate's Pokémon faint, the surviving partner seamlessly commands both active field slots.
* **Interactive Ghost Support**: Fainted partners enter spectator mode to execute tactical support buffs:
  * `[Z]` **Ghost Heal**: Restores 25% Max HP to active ally Pokémon.
  * `[X]` **Ghost Guard**: Grants defensive shielding against incoming attacks.
  * `[C]` **Ghost Buff**: Boosts offensive attack power.

---

## 🧗 100-Floor Progression & Level Caps

| Stage | Floor Range | Level Cap | Difficulty & Boss Encounters |
| :---: | :---: | :---: | :--- |
| **Stage 1** | **Floors 1 – 10** | **Lv. 30** | Novice Gym Leaders & Early Trainers (Boss: F.10) |
| **Stage 2** | **Floors 11 – 25** | **Lv. 50** | Mid-game Gym Leaders & Weather Teams (Boss: F.25) |
| **Stage 3** | **Floors 26 – 50** | **Lv. 65** | Advanced Veteran Trainers & Trick Room Sets (Boss: F.50) |
| **Stage 4** | **Floors 51 – 75** | **Lv. 80** | Elite Four Members & Hyper-Offense Teams (Boss: F.75) |
| **Stage 5** | **Floors 76 – 90** | **Lv. 90** | Regional Champions & Mega Evolution Aces (Boss: F.90) |
| **Stage 6** | **Floors 91 – 99** | **Lv. 100** | **Primal Sovereigns** (Groudon, Kyogre, Rayquaza, Zacian, Ogerpon, Calyrex) |
| **Summits** | **Floor 100** | **Lv. 100** | 👑 **Genesis Arceus Sovereign** (Spear Pillar Peak) |

---

## 💰 Battle Points (BP) Exchange Shop

Earn BP after every victorious floor and redeem them in the Hub Shop (`[Y]`):
* **Hyper Training & IVs**: Bottle Caps, Gold Bottle Caps.
* **Competitives & EV Items**: Power Anklet/Bracer/Belt, Ability Capsules, Ability Patches.
* **Evolution Catalysts**: Linking Cords, Evolutionary Stones, Auspicious/Malicious Armor.
* **Tera Shards**: Stellar Tera Shards, Type-specific shards.
* **Held Items**: Choice Specs/Band/Scarf, Focus Sash, Life Orb, Assault Vest, Heavy-Duty Boots.

---

## 🎮 Keybindings & Commands

| Action | Keybind / Command | Description |
| :--- | :--- | :--- |
| **Open Tower Hub** | `Y` *(Default)* | Opens the main CobbleTower Hub screen. |
| **Forfeit Run** | `F8` *(Default)* | Safely yield the run and teleport back to Overworld. |
| **Spectator Heal** | `Z` *(Spectator)* | Restores partner's active Pokémon HP. |
| **Spectator Guard** | `X` *(Spectator)* | Grants damage reduction barrier to partner. |
| **Spectator Buff** | `C` *(Spectator)* | Boosts partner's offensive attack power. |
| **View Stats** | `/tower stats` | Displays BP balance, prestige level, and perks. |
| **Ascend Prestige** | `/tower prestige` | Ascends prestige level after completing Floor 100. |
| **Hall of Fame** | `/tower leaderboard` | Opens the Top 10 fastest Floor 100 clear records. |
| **Invite Partner** | `/tower duo <player>` | Sends an invite to team up for Co-op Duo. |

---

## 📦 Requirements & Compatibility

* **Minecraft**: `1.21.1`
* **Fabric Loader**: `>= 0.16.0`
* **Fabric API**: Latest for 1.21.1
* **Cobblemon**: `>= 1.7.0`
* **Java**: `21`
* **Optional Addons**: **Mega Showdown** *(Mega Evolutions, Z-Moves, Dynamax)*, **RCTMod** *(Expanded trainer models)*.

---

## 📜 Credits & License

* **Authors**: Vit, Arjun, Serik, Zitj, Nam
* **Community**: Designed with ❤️ for the **CobbleVerse** adventure!
* **License**: **MIT License** — free for use and distribution in modpacks.
