# 🗼 CobbleTower (ViTwo) — v1.3.0 Definitive

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?logo=minecraft)](https://minecraft.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.16.0+-blue.svg?logo=fabric)](https://fabricmc.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.0+-red.svg)](https://cobblemon.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.3.0-orange.svg)](https://github.com/namtacozz/ViTwo/releases)

**CobbleTower** is a competitive **100-Floor Roguelike Battle Tower** mod engineered for **Cobblemon (Minecraft Fabric 1.21.1)**. Supporting both **Solo (2-Slot 6v6)** and **Duo Co-op (3+3 Double Battles)** in standard VGC format, featuring intelligent competitive AI, dynamic floor curses, anti-cheese tournament clauses, interactive ghost support spectator mode, and full advancement journey integration.

---

## 🌟 Key Features in v1.3.0

### 1. ⚔️ Competitive VGC Double Battles (Solo & Duo Co-op)
* **Solo Mode (6v6):** 1 Player commands both active field positions with 6 competitive Pokémon against elite NPC Trainers.
* **Duo Co-op (3+3):** 2 Players team up (each commanding 1 position with 3 Pokémon).
* **2-Slot Takeover Protocol:** When a partner's Pokémon faint, the surviving teammate seamlessly assumes full control of both field slots.
* **Interactive Ghost Support:** Fainted players enter **Ghost Spectator Mode** and utilize tactical battlefield abilities (`[Z]` Heal Pulse, `[X]` Quick Guard, `[C]` Battle Cry, `[V]` Spectral Insight) powered by scaled Ghost Charges (2⚡/3⚡/4⚡).
* **Disconnect Recovery Grace Period:** 180-second reconnect window protects active runs against network dropouts.

---

### 2. ☠️ Ancient Floor Curses & Dual Curse Matrix
Starting from Floor 11, ancient arena modifiers alter battlefield dynamics:
* 🛡 **Curse of the Ironclad:** Opponents take 15% reduced damage.
* ⚡ **Curse of Inertia:** Pure stat-boosting moves (*Swords Dance, Dragon Dance, Nasty Plot...*) are sealed.
* ⏳ **Curse of Fatigue:** Moves with ≥8 PP consume +1 extra PP per execution.
* 🌫 **Curse of the Fog:** Dense mist reduces move accuracy by 10%.
* ⌛ **Curse of Temporal Decay:** Consecutive protect moves rapidly decay (50% ➔ 12.5% ➔ 0%).
* 🩸 **Curse of Retaliation:** 10% of direct damage dealt is reflected back (capped at 20% Max HP).
* 🔇 **Curse of Silence:** All sound-based moves (*Boomburst, Hyper Voice, Bug Buzz, Snarl...*) are disabled.
* 🌑 **Curse of the Gravity Well:** Permanent gravity nullifies Ground immunities (Flying/Levitate) and boosts move accuracy by +20%.
* 🍂 **Curse of Famine:** All direct healing and recovery effects are reduced by 50%.
* 💀 **Dual Curse Matrix (Floors 91–100):** Fixed non-random strategic combinations designed specifically for Sovereign Boss encounters.

---

### 3. ⚖️ Competitive Integrity & Anti-Cheese Clauses
Enforced automatically upon entering the Tower realm:
* **Species Clause:** No duplicate Pokémon species (same National Dex number).
* **Item Clause:** No duplicate held items across the team.
* **Legendary Tiering Caps:**
  * **Floors 1 – 75:** Max **1** Restricted Box Legend, max **2** Non-Restricted Sub-Legends.
  * **Floors 76 – 100:** Max **2** Restricted Box Legends, unlimited Sub-Legends.
* **Anti-Cheese Clauses:**
  * **Evasion Clause:** Banned evasion-raising moves (*Double Team, Minimize*).
  * **OHKO Clause:** Banned one-hit knockout moves (*Sheer Cold, Fissure, Horn Drill, Guillotine*).
  * **Moody Clause:** Banned ability *Moody*.
  * **Swagger Clause:** Banned confusion-boosting move *Swagger*.
* **Party & Inventory Lock:** Pokémon stats, EVs, IVs, movesets, and held items are frozen during runs.

---

### 4. 🏕️ Roguelike Rest Stations (Every 5 Floors)
Strategic decisions at each 5-floor rest milestone:
* 🟢 **Medical Bay:** Revive fainted Pokémon with 30% HP, heal active Pokémon +40% HP, refresh 100% PP, and clear all status afflictions.
* ⚔️ **War Council:** Acquire a 5-floor strategic buff (*Iron Vanguard, Swift Wind, Apex Surge, or Mystic Ward*).
* 🎁 **Treasure Vault:** Forego recovery to receive Battle Points (BP) and rare competitive loot (Mints, Bottle Caps, Ability Patches, Tera Shards).

---

### 5. 🏪 4-Tier Battle Point (BP) Exchange Shop
Access the shop directly via the Hub screen (`[Y]`):
* **Bronze Tier (Default):** Rare Candy, Stat Mints, Focus Sash, Choice Band/Specs/Scarf, Life Orb, Assault Vest, Heavy-Duty Boots, Booster Energy.
* **Silver Tier (Floor 50+ True Run):** Ability Capsules (5/wk), Bottle Caps (10/wk), Toxic/Flame Orbs, Eviolite.
* **Gold Tier (Floor 100+ True Run):** Ability Patches (2/wk), Gold Bottle Caps (2/wk), Stellar Tera Shards, Master Ball (1/wk), Title `« Tower Champion »`.
* **Platinum Tier (Prestige 3+):** Shiny Aura, Particle Trail, Custom Victory Fanfare, Challenge Re-rolls, Title `« Tower Legend »` (Prestige 5).

---

### 6. ⚙️ Difficulty Profiles
Configured via `config/cobbletower/tower.json`:
| Profile | Curse Mult | AI Misplay | Timer | Mercy Continue | BP Multiplier |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **CASUAL** | 0.5x | 15% | 60s | 3 Continues | 0.6x |
| **STANDARD** | 1.0x | 5% | 45s | 1 Continue (50% HP) | 1.0x |
| **HARDCORE** | 1.5x | 0% | 30s | 0 Continues | 1.5x |

---

## 🎮 Controls & Commands

### Keybinds & Interactivity
* **`[Y]`** — Open CobbleTower Hub GUI.
* **`Shift + Right-Click Player`** — Send instant Duo Co-op invite.
* **`Right-Click Tower Gateway`** — Access Hub dashboard in-world.
* **`[Z]` / `[X]` / `[C]` / `[V]`** — Ghost Spectator Support actions during Duo battles.

### Command Suite (`/tower`)
```bash
# Start a True Run from Floor 1
/tower start
/tower solo

# Start a Checkpoint Run from unlocked checkpoints (Floor 26, 51, 76)
/tower checkpoint <floor>

# Duo Co-op management
/tower duo <player>
/tower duo accept
/tower duo decline

# Run persistence & pause
/tower pause
/tower resume

# Profile & stats
/tower stats
/tower forfeit

# Admin config reload
/tower reload
```

---

## 📦 Installation & Setup

1. Verify that **Minecraft 1.21.1**, **Fabric Loader >= 0.16.0**, and **Cobblemon >= 1.7.0** are installed.
2. Download the latest **`CobbleTower-1.3.0.jar`** from [Releases](https://github.com/namtacozz/ViTwo/releases).
3. Place the `.jar` into your `.minecraft/mods` folder.
4. Launch Minecraft and press **`[Y]`** to enter the CobbleTower Hub!

---

## 🔨 Building from Source

```bash
# Clone the repository
git clone https://github.com/namtacozz/ViTwo.git
cd ViTwo

# Compile and build the mod JAR
./gradlew build
```

The compiled binary will be generated under `build/libs/CobbleTower-1.3.0.jar`.

---

## 📜 License & Credits
* **Development Team:** Vit, Arjun, Serik, Zitj, and Nam (ViTwo Team)
* **License:** [MIT License](LICENSE)
