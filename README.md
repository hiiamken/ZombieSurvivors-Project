<div align="center">

# 🧟 Zombie Survivors

**A top-down survival shooter inspired by Vampire Survivors**

Survive 10 minutes against endless waves of zombies, collect upgrades, and evolve your weapon into the legendary Death Spiral!

<p align="center">
  <img src="https://files.catbox.moe/f3apux.png" alt="Zombie Survivors Banner" width="800">
</p>

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A.svg)](https://gradle.org/)
[![LibGDX](https://img.shields.io/badge/LibGDX-GameApp-red.svg)](https://libgdx.com/)

</div>

---

## 📖 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Getting Started](#-getting-started)
- [How to Play](#-how-to-play)
- [Upgrade System](#-upgrade-system)
- [Project Structure](#-project-structure)
- [Documentation](#-documentation)
- [Credits](#-credits)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Wave-Based Combat** | Survive 10 waves of increasingly difficult zombies |
| **Auto-Fire Weapon** | Focus on movement while your weapon shoots automatically |
| **10-Level Weapon System** | Upgrade your pistol with more bullets, pierce, and spread patterns |
| **7 Passive Items** | Collect Power Herb, Iron Shield, Swift Boots, and more |
| **Weapon Evolution** | Evolve to Death Spiral - 16 rotating bullets with infinite pierce |
| **MiniBoss Fights** | Defeat MiniBosses for Treasure Chests |
| **Gacha Rewards** | Casino-style animation for chest rewards |
| **Stampede Events** | Dodge massive zombie hordes rushing across the screen |
| **Leaderboard** | Compete for top 10 rankings |
| **Full Audio** | Background music and sound effects |

---

## � Screenshots

> *Add screenshots here: Main Menu, Gameplay, Level-Up Menu, Gacha Animation, Victory Screen*

---

## 🚀 Getting Started

### System Requirements

| Requirement | Minimum |
|-------------|---------|
| **OS** | Windows 10/11, macOS, Linux |
| **Java** | Version 17 or higher |
| **RAM** | 4GB |
| **Display** | 1280x720 |

> 💡 Don't have Java? Download from [Adoptium](https://adoptium.net/)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/hiiamken/ZombieSurvivors-Project.git
   cd ZombieSurvivors-Project
   ```

2. **Build the game**
   ```bash
   # Windows
   gradlew.bat jar
   
   # macOS/Linux
   ./gradlew jar
   ```

3. **Run the game**
   ```bash
   java -jar build/libs/ZombieSurvivorsClone-1.0.0.jar
   ```

### Quick Start (Pre-built)

If a pre-built JAR exists:
```bash
# Windows
run.bat

# macOS/Linux
./run.sh
```

---

## 🎮 How to Play

### Controls

| Action | Key |
|--------|-----|
| **Move** | `W` `A` `S` `D` or Arrow Keys |
| **Shoot** | Automatic |
| **Pause** | `ESC` |
| **Fullscreen** | `F11` |
| **Menu Navigate** | `W`/`S` or Arrow Keys |
| **Select** | `Enter` or Click |
| **Quick Select** | `1` `2` `3` (during level-up) |

### Objective

**Survive for 10 minutes** against endless zombie waves.

- Kill zombies to earn XP
- Level up to choose upgrades
- Defeat MiniBosses for treasure chests
- Reach weapon evolution for ultimate power
- Achieve the highest score on the leaderboard

### Game Tips

1. **Keep moving** - Standing still makes you an easy target
2. **Prioritize weapon upgrades early** - Faster kills = faster leveling
3. **Magnet Stone** helps collect XP safely from a distance
4. **During stampedes**, move perpendicular to the horde direction
5. **Aim for evolution** - Death Spiral is incredibly powerful

---

## ⚔️ Upgrade System

### Weapon Levels (Max: 10)

| Level | Upgrades |
|-------|----------|
| 1-2 | +1 bullet per level |
| 3 | Front spread (3 directions), +15% fire rate |
| 5 | Pierce 1 enemy |
| 7 | Pierce 2 enemies |
| 8 | Back spread (3 directions), +25% damage |
| 10 | Maximum power (evolution ready) |

### Passive Items (Max: Level 5 each)

| Item | Effect per Level |
|------|------------------|
| **Power Herb** | +10% damage |
| **Iron Shield** | -5% damage taken |
| **Swift Boots** | +10% movement speed |
| **Lucky Coin** | +5% critical chance |
| **Magnet Stone** | +20% XP pickup range |
| **Life Essence** | +0.2 HP/second regeneration |
| **Vitality Core** | +20% max HP |

### Weapon Evolution: Death Spiral

**Requirements:** Weapon Level 10 + All 7 passives at Level 5

| Stat | Value |
|------|-------|
| Bullets | 16 (rotating spiral) |
| Pierce | Infinite |
| Lifesteal | 10% |
| Damage | +150% |
| Coverage | 360° automatic |

---

## � Project Structure

```
ZombieSurvivorsClone/
├── src/main/java/nl/saxion/
│   ├── game/
│   │   ├── MainGame.java          # Entry point
│   │   ├── config/                # Configuration management
│   │   ├── entities/              # Player, Enemy, Weapon, Items
│   │   ├── screens/               # Menu, Play, Settings, etc.
│   │   ├── systems/               # Spawner, Renderer, Sound, Gacha
│   │   ├── ui/                    # UI components
│   │   └── utils/                 # Utility classes
│   └── gameapp/                   # GameApp framework wrapper
├── src/main/resources/
│   ├── sprites/                   # Game sprites and animations
│   ├── sounds/                    # Music and SFX
│   ├── fonts/                     # Custom pixel fonts
│   └── maps/                      # Tile-based maps
├── build.gradle                   # Gradle build configuration
└── README.md
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `PlayScreen` | Main gameplay loop, entity management, HUD |
| `Player` | Health, XP, movement, passive items |
| `Weapon` | Firing, upgrades, evolution |
| `EnemySpawner` | Wave system, stampedes, MiniBoss spawning |
| `GachaSystem` | Treasure chest reward animation |
| `SoundManager` | Audio playback |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [`MANUAL.txt`](MANUAL.txt) | Complete game manual with all mechanics |
| [`README.txt`](README.txt) | Quick reference game guide |
| [`TECHNICAL_STRUCTURE.txt`](TECHNICAL_STRUCTURE.txt) | Architecture and code documentation |
| [`SPRINT1_DOCUMENTATION.txt`](SPRINT1_DOCUMENTATION.txt) | Sprint 1 development log |
| [`SPRINT2_DOCUMENTATION.txt`](SPRINT2_DOCUMENTATION.txt) | Sprint 2 development log |
| [`SPRINT3_DOCUMENTATION.txt`](SPRINT3_DOCUMENTATION.txt) | Sprint 3 development log |

---

## �️ Build Commands

```bash
# Build JAR
gradlew.bat jar                    # Windows
./gradlew jar                      # macOS/Linux

# Build with launch scripts
gradlew.bat dist                   # Creates run.bat and run.sh

# Create Windows installer
gradlew.bat packageExe             # Requires jpackage (JDK 14+)

# Run directly
gradlew.bat run                    # Development mode
```

---

## 🎓 Credits

**Saxion University of Applied Sciences**  
Course: Project IT's in the Game  
Academic Year: 2025-2026 | Quarter 2

### Tech Stack

- **Language:** Java 17
- **Framework:** [LibGDX](https://libgdx.com/) via [Saxion GameApp](https://gitlab.com/evertduipmans/saxiongameapp)
- **Build Tool:** Gradle
- **Inspired by:** [Vampire Survivors](https://store.steampowered.com/app/1794680/Vampire_Survivors/) by poncle

---

<div align="center">

**🎮 Good luck, Survivor! 🧟**

*Survive. Upgrade. Evolve. Dominate.*

</div>


