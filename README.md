# 🧟 Zombie Survivors

> A Top-Down Survival Shooter Game inspired by Vampire Survivors

<p align="center">
  <img src="https://files.catbox.moe/rch77b.png" alt="Zombie Survivors Banner" width="800">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/LibGDX-Framework-red?style=for-the-badge" alt="LibGDX">
  <img src="https://img.shields.io/badge/Status-In%20Development-green?style=for-the-badge" alt="Status">
</p>

---

## 📖 Table of Contents

- [About The Project](#-about-the-project)
- [Getting Started](#-getting-started)
- [How To Play](#-how-to-play)
- [Features](#-features)
- [Project Architecture](#-project-architecture)
- [Sprint Progress](#-sprint-progress)
- [Known Issues](#-known-issues)
- [Credits](#-credits)

---

## 🎮 About The Project

**Zombie Survivors** is a top-down survival shooter game where you fight against endless waves of zombies. Survive as long as you can and become the ultimate survivor!

### Built With

- **Java 17** - Programming Language
- **LibGDX** - Game Framework
- **Saxion GameApp** - University Library
- **Gradle** - Build System
- **Tiled** - Map Editor

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Operating System | Windows 10/11 |
| Java Runtime | 17 or higher |

> 💡 **Don't have Java?** Download from [Adoptium](https://adoptium.net/)

### Installation

1. **Download** the game file (`ZombieSurvivors.exe`)
2. **Double-click** to launch
3. **Play!** 🎮

### Alternative Methods

If `.exe` doesn't work:

```bash
# Method 1: Run batch file
run.bat

# Method 2: Run JAR directly
java -jar ZombieSurvivorsClone-1.0.0.jar
```

---

## 🎯 How To Play

### Controls

| Key | Action |
|-----|--------|
| `W` | Move Up |
| `A` | Move Left |
| `S` | Move Down |
| `D` | Move Right |

> 💡 **Auto-Shoot:** Your weapon fires automatically at nearby enemies!

### Tips & Tricks

- 🏃 **Keep moving!** Standing still makes you an easy target
- ↗️ **Use diagonal movement** (W+D, W+A) to dodge faster
- ❤️ **Watch your health bar** - don't let zombies surround you
- 🏆 **Survive as long as possible** for the highest score!

---

## ✨ Features

### Core Gameplay
- ✅ Top-down shooter perspective
- ✅ Smooth WASD movement controls
- ✅ Auto-shooting weapon system
- ✅ Endless zombie waves

### Player System
- ✅ Player movement with delta-time
- ✅ Health system with damage/healing
- ✅ Player status interface for UI
- ✅ Boundary collision

### Enemy System
- ✅ Zombie AI - chase player behavior
- ✅ Multiple animations (idle, run, hit, death)
- ✅ Enemy spawner with difficulty scaling
- ✅ Knockback effect when hit

### Weapon & Combat
- ✅ Bullet entity system
- ✅ Fire rate control
- ✅ Collision detection
- ✅ Auto-fire mechanics

### Map System
- ✅ 16 procedural map rooms
- ✅ TMX map file parsing
- ✅ Tile-based collision detection
- ✅ Seamless room rendering

### UI & Graphics
- ✅ HUD interface (health bar, stats)
- ✅ Animated sprite sheets
- ✅ Game state management (menu, playing, game over)

---

## 🏗️ Project Architecture

```
ZombieSurvivorsClone/
│
├── 📁 src/main/java/nl/saxion/game/
│   │
│   ├── 📄 MainGame.java              # Game entry point
│   │
│   ├── 📁 core/
│   │   ├── GameState.java            # Game state enum
│   │   └── PlayerStatus.java         # Player data interface
│   │
│   ├── 📁 entities/
│   │   ├── Player.java               # Player entity
│   │   ├── Enemy.java                # Enemy entity
│   │   ├── Bullet.java               # Bullet entity
│   │   └── Weapon.java               # Weapon system
│   │
│   ├── 📁 screens/
│   │   └── PlayScreen.java           # Main gameplay screen
│   │
│   ├── 📁 systems/
│   │   ├── InputController.java      # Input handling
│   │   ├── EnemySpawner.java         # Enemy spawn logic
│   │   ├── CollisionHandler.java     # Collision detection
│   │   ├── MapRenderer.java          # Map rendering
│   │   ├── GameRenderer.java         # Entity rendering
│   │   ├── GameStateManager.java     # State management
│   │   └── ResourceLoader.java       # Asset loading
│   │
│   ├── 📁 ui/
│   │   └── HUD.java                  # Heads-up display
│   │
│   └── 📁 utils/
│       ├── TMXParser.java            # TMX file parser
│       ├── TMXMapData.java           # Map data structure
│       ├── CollisionChecker.java     # Collision utilities
│       └── CoordinateConverter.java  # Coordinate utils
│
└── 📁 src/main/resources/assets/
    ├── 📁 player/                    # Player sprites
    ├── 📁 enemy/                     # Enemy sprites
    ├── 📁 Bullet/                    # Bullet sprites
    ├── 📁 maps/                      # TMX maps
    ├── 📁 tiles/                     # Tile sheets
    └── 📁 fonts/                     # Game fonts
```

---

## 📋 Sprint Progress

### Sprint 1 - Core Foundation ✅

> **Status:** COMPLETED

| Task | Description | Status |
|------|-------------|--------|
| 1 | Project Setup & Core Structure | ✅ Done |
| 2 | Input Handling (WASD) | ✅ Done |
| 3 | Player Feature (Movement, Sprite, Health) | ✅ Done |
| 4 | Player Data Interface for UI | ✅ Done |
| 5 | Bullet System & Shooting | ✅ Done |
| 6 | Weapon System & Fire Rate | ✅ Done |
| 7 | Enemy System & AI | ✅ Done |
| 8 | Collision Detection | ✅ Done |

---

### Sprint 2 - Systems & Polish 🔄

> **Status:** IN PROGRESS

#### Week 1 - Core Systems

| Task | Description | Status |
|------|-------------|--------|
| - | Code Refactoring (Modular Systems) | ✅ Done |
| - | Map System (TMX Parsing) | ✅ Done |
| - | Enemy Spawner | ✅ Done |
| - | Zombie Animations | ✅ Done |
| - | HUD System | ✅ Done |
| - | Auto-Aim System | ⬜ Not Started |

#### Week 2 - Experience System

| Task | Description | Status |
|------|-------------|--------|
| 1 | XP & Leveling System | ⬜ Not Started |
| 2 | Upgrade System (Stats & Weapons) | ⬜ Not Started |
| 3 | Level-Up Menu | ⬜ Not Started |
| 4 | Multiple Weapons | ⬜ Not Started |

---

### Sprint 3 - Enhancements 📋

> **Status:** PLANNED

| Feature | Description | Status |
|---------|-------------|--------|
| Weapon Evolution | Combine weapons for upgrades | ⬜ Planned |
| More Enemy Types | Fast, Tank, Boss zombies | ⬜ Planned |
| Sound & Music | Audio effects | ⬜ Planned |
| Visual Effects | Particles, screen shake | ⬜ Planned |
| Save System | High scores, persistence | ⬜ Planned |

---

## ⚠️ Known Issues

### Current Bugs
- None critical at this time

### Limitations
- 🖥️ Windows only (Mac/Linux need `run.sh`)
- ☕ Requires Java 17+ installed
- 🔇 No sound effects yet

---

## 👥 Credits

### Development Team

**Team ZombieSurvivors**

| Member | Role |
|--------|------|
| Thuong | Developer |
| Daniel | Developer |
| Arnold | Developer |
| Mehmet | Developer |

### Institution

<p>
  <strong>Saxion University of Applied Sciences</strong><br>
  Course: Project IT's in the Game<br>
  Academic Year: 2025-2026<br>
  Quarter: 2
</p>

### Technologies

| Tech | Purpose |
|------|---------|
| Java 17 | Programming Language |
| LibGDX | Game Framework |
| Saxion GameApp | University Library |
| Gradle | Build System |
| Tiled | Map Editor |

### Special Thanks

- Saxion Game Development Course Staff
- LibGDX Community
- Vampire Survivors (inspiration)

---

## 📄 License

This project is developed for educational purposes as part of the Saxion University curriculum.

**© 2024 Team ZombieSurvivors - Saxion University**

---

<p align="center">
  <strong>🎮 Thank you for playing Zombie Survivors! 🧟</strong><br>
  <em>Good luck surviving!</em>
</p>

