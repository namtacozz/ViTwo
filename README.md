# 🗼 CobbleTower (ViTwo)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.0+-blue.svg)](https://fabricmc.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.0+-red.svg)](https://cobblemon.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**CobbleTower** is an official **100-Floor Competitive Roguelike Battle Tower** mod engineered for **Cobblemon (Fabric 1.21.1)**. Supporting both **Solo (2-Slot control)** and **Duo Co-op (2 Players)** in standard VGC-style **Double Battle** format, featuring authentic competitive AI, archetype synergy teams, floor affixes, level caps, and native Minecraft advancement journey integration.

---

## 🌟 Key Features

### 1. ⚔️ Competitive Format: VGC Double Battles
* **Solo Mode:** 1 Player commands both active field slots (2 active Pokémon out of 6) against elite NPC Trainers.
* **Duo Co-op Mode:** 2 Players team up (each commanding 1 slot with 3 Pokémon). If one player's team is wiped out, the remaining partner takes over both field slots (**2-Slot Takeover**), while the fainted player enters **Spectator Mode**.

### 2. ⚖️ Competitive Clauses & Regional Progression
* **Species Clause:** Duplicate Pokémon of the same National Pokédex number are strictly prohibited.
* **Item Clause:** Duplicate held items across the party are prohibited.
* **Legendary Cap:**
  * **Floors 1 – 75:** Max **1** Legendary / Mythical Pokémon per team.
  * **Floors 76 – 100:** Max **2** Legendary / Mythical Pokémon per team.
* **Kanto Journey Unlock:** The Tower Realm unlocks naturally once players have journeyed through Kanto Gyms (Level 20+ ready team).

### 3. ☠️ Ancient Floor Curses (Affixes)
Starting from Floor 11, every 10-floor bracket activates an unpredictable ancient arena modifier:
* 🛡 **Curse of the Ironclad:** Opponent Pokémon take 20% reduced damage.
* ⚡ **Curse of Inertia:** Stat-boosting moves (Swords Dance, Dragon Dance, Nasty Plot...) are sealed.
* ⏳ **Curse of Fatigue:** All moves consume +1 extra PP per use.
* 🌫 **Curse of the Fog:** Dense mist reduces move accuracy by 15%.
* 🩸 **Curse of Retaliation:** 15% of direct damage dealt is reflected back as recoil.

### 4. 🌪️ AI Strategy Archetypes & Competitive Held Items
From Floor 51 onwards, NPC Trainers deploy synergistic competitive team rosters:
* 🌧 **Rain Synergy:** Drizzle Pelipper + Swift Swim Kingdra / Barraskewda + Thunder Zapdos + Urshifu-RS.
* ☀️ **Sun Synergy:** Drought Torkoal + Protosynthesis Paradoxes (Chi-Yu, Flutter Mane, Walking Wake, Roaring Moon).
* ⏳ **Trick Room:** Hatterene + Ursaluna Bloodmoon + Torkoal + Kingambit + Cresselia.
* 🛡 **Hazard Stall:** Ting-Lu + Toxapex + Gliscor + Corviknight + Blissey + Garganacl.
* **VGC Items:** NPC Pokémon carry Focus Sash, Choice Scarf, Choice Band, Choice Specs, Life Orb, Assault Vest, Heavy-Duty Boots, and Booster Energy.
* **Smart Adaptive Counter-Tera:** Intelligent AI reactive Terastallization targeting player weaknesses across all 18 Pokémon types.

### 5. 📊 Tiered Level Caps & Perfect Boss Pokémon
* **Floor 1 – 25:** Cap **Lv. 36** (Boss Lv. 32 – 36).
* **Floor 26 – 50:** Cap **Lv. 50** (Boss Lv. 46 – 50).
* **Floor 51 – 75:** Cap **Lv. 80** (Boss Lv. 75 – 80 + **1 Guaranteed Shiny ✨**).
* **Floor 76 – 100:** Cap **Lv. 100** (Boss Lv. 95 – 100 + **1 Guaranteed Shiny ✨**).
* **Boss Stats:** All Boss Pokémon possess **6x31 Perfect IVs** with competitive EV spreads.

### 6. ⛺ Roguelike Rest Stations (Every 5 Floors)
Persistent HP & PP decay between standard floors. At Floors 5, 10, 15, 20... choose:
* 🟢 **Team Recovery:** Revive fainted Pokémon (10% HP), restore +50% HP to survivors, and refresh 100% Move PP.
* 🎁 **Mystery Loot Cache:** Forego healing for rare rewards (Rare Candies, Bottle Caps, Diamonds, Netherite).

### 7. 🏆 Extensive Advancement Journey Tree (34+ Advancements)
Fully integrated into Minecraft's native Advancement screen (`[L]` key) with **AdvancementPlaques** and **PaginatedAdvancements** support:
* **Floor Milestones:** Poke Tower (10), Great Tower (25), Ultra Tower (50), Champion of the Realm (75), Sovereign Gatekeeper (90), Master Tower (100).
* **Combat Challenges:** Tempest Breaker, Trial of Adversity, Flawless Ascent (Deathless), Pure Raw Power (No Gimmicks), True Synergy (Duo 100).
* **18 Monotype Masteries:** Solo clear achievements for all 18 elemental types.

---

## 🎮 Controls & User Interface

| Keybind / Action | Action | Description |
| :--- | :--- | :--- |
| **`Y`** | Open CobbleTower Hub | Toggle Solo/Duo, pick checkpoints, accept invites, launch runs |
| **`Shift + Right-Click`** | Invite Player | Target a player in the overworld to send an instant invite |
| **Right-Click Block** | Tower Gateway Block | Open the CobbleTower Hub GUI |

---

## 📦 Installation

1. Ensure **Minecraft 1.21.1**, **Fabric Loader >= 0.16.0**, and **Cobblemon >= 1.7.0** are installed.
2. Download the latest `vitwo-1.4.0.jar` from [Releases](https://github.com/namtacozz/ViTwo/releases).
3. Place the `.jar` into your Minecraft `.minecraft/mods` folder.
4. Launch the game and press **`[Y]`** to access the CobbleTower Hub!

---

## 🔨 Building from Source

```bash
# Clone the repository
git clone https://github.com/namtacozz/ViTwo.git
cd ViTwo

# Build the mod JAR
./gradlew build
```

Compiled JAR will be located at `build/libs/vitwo-1.0.0.jar`.

---

## 📜 Credits & License
* **Authors:** Vit, Arjun, Serik, Zitj, and Nam
* **License:** [MIT License](LICENSE)
