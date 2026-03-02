# 🎩 VoltrazCosmetics (MagicCosmetics Fork)

A cosmetic plugin for Minecraft servers that allows players to wear cosmetic items such as **hats**, **backpacks**, **walking sticks**, **balloons**, and **sprays**.

> 📖 [Baca dalam Bahasa Indonesia](README.id.md)

## ✨ Features

- **Hat** — Helmet cosmetics with overlay (combined) and non-overlay modes
- **Walking Stick** — Off-hand cosmetics with item combining support
- **Backpack** — Backpacks displayed on the player's back
- **Balloon** — Balloons floating above the player
- **Spray** — Spray paint that can be placed on walls
- **Zone System** — Dedicated wardrobe areas for cosmetic preview
- **NPC Support** — Cosmetics can be applied to NPCs (Citizens)
- **Token System** — Currency system for unlocking cosmetics
- **Color Customization** — Cosmetic coloring with a color picker

## 📋 Requirements

- **Java** 8+
- **Minecraft** 1.17 — 1.21.5
- **Server** Paper / Spigot
- **Maven** 3.6+ (for building)

## 🔧 Build

```bash
mvn clean package -pl plugin -am
```

Output JAR: `plugin/target/MagicCosmetics-3.1.1.jar`

## 📦 Project Modules

| Module | Description |
|--------|-------------|
| `api` | Public API & base classes (`Cosmetic`, `CosmeticType`) |
| `plugin` | Main plugin, listeners, database, cache |
| `v1_18_R1` — `v1_21_R4` | NMS adapters per Minecraft version |
| `meg3_support` / `meg4_support` | ModelEngine integration |
| `bungeecord` / `velocity` | Proxy support |

## 🔌 Plugin Integrations (Soft Dependencies)

| Plugin | Purpose |
|--------|---------|
| LuckPerms | Permission-based cosmetics |
| ItemsAdder / Nexo | Custom resource pack items |
| ModelEngine | 3D models for cosmetics |
| PlaceholderAPI | Placeholder support |
| Citizens | NPC cosmetics |
| HuskSync | Cross-server sync |
| WorldGuard | Region-based restrictions |
| Multiverse-Core | Multi-world support |

## 💾 Database

Supports **SQLite** (default) and **MySQL**. Uses HikariCP connection pooling.

MySQL configuration in `config.yml`:
```yaml
MySQL:
  enabled: true
  host: localhost
  port: 3306
  user: root
  password: ""
  database: cosmetics
  table: player_cosmetics
  options: "useSSL=false"
```

## 🎮 Commands

| Command | Aliases |
|---------|---------|
| `/magicosmetics` | `/cosmetics`, `/mcosmetics`, `/magiccos` |

## 📁 Code Structure

```
plugin/src/main/java/com/francobm/magicosmetics/
├── MagicCosmetics.java      # Main plugin class
├── api/                      # Cosmetic API & types
├── cache/                    # Player data, entity cache, inventories
│   ├── PlayerData.java       # Per-player cosmetic state
│   ├── EntityCache.java      # NPC/entity cosmetic state
│   └── cosmetics/            # Hat, WStick, Bag, Balloon, Spray
├── commands/                 # Command handler
├── database/                 # SQL, SQLite, MySQL, HikariCP
├── listeners/                # Event listeners
├── managers/                 # Cosmetics & zones manager
├── nms/                      # NMS abstraction layer
├── provider/                 # 3rd party integrations
└── utils/                    # Utilities
```

## 📝 Recent Changelog

### v3.1.1
- **Fix:** Helmets no longer disappear when a player disconnects/quits with active cosmetics
- **Fix:** Helmets no longer disappear on death with `keepInventory: true`
- **Fix:** Walking stick no longer replaces items in the off-hand
- **Fix:** `clearClose()` now restores saved items in non-overlaps mode
- **Fix:** Removed duplicate `clearCosmeticsToSaveData()` that was reverting item restoration
- **Fix:** `PlayerData.players` now uses `ConcurrentHashMap` for thread safety
- **Fix:** `EntityCache.entities` now uses `ConcurrentHashMap` for thread safety
- **Fix:** Typo in `EntityCache.activeWStick()` that used `hat` instead of `wStick`
- **Fix:** HikariCP connection pool is now properly closed on plugin disable
- **Fix:** Added null checks for `PlayerData.getPlayer()` in all event handlers

## 📄 License

This plugin was originally created by **FrancoBM**. This fork is maintained for the **Voltraz** server.
