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

- **Java** 21+
- **Minecraft** 1.21 — 1.21.11
- **Server** Paper
- **Maven** 3.6+ (for building)

## 🔧 Build

```bash
mvn clean package -DskipTests
```

Output JAR: `plugin/target/VoltrazCosmetics-x.jar`

## 📦 Project Modules

| Module | Description |
|--------|-------------|
| `api` | Public API & base classes (`Cosmetic`, `CosmeticType`) |
| `plugin` | Main plugin, listeners, database, cache |
| `v1_21_R1` — `v1_21_R7` | NMS adapters for MC 1.21.x |
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

## 📄 License

This plugin was originally created by **FrancoBM**. This fork is maintained by **RevelX** for the **Voltraz** server.
