# 🎩 VoltrazCosmetics (MagicCosmetics Fork)

Plugin kosmetik untuk server Minecraft yang memungkinkan pemain mengenakan item kosmetik seperti **topi**, **tas ransel**, **tongkat jalan**, **balon**, dan **spray**.

## ✨ Fitur

- **Hat** — Kosmetik helmet dengan mode overlay (combined) dan non-overlay
- **Walking Stick** — Kosmetik off-hand dengan support item combining
- **Backpack** — Tas ransel yang tampil di punggung pemain
- **Balloon** — Balon yang melayang di atas pemain
- **Spray** — Spray paint yang bisa ditempel di dinding
- **Zone System** — Area khusus wardrobe untuk preview kosmetik
- **NPC Support** — Kosmetik bisa dipasang pada NPC (Citizens)
- **Token System** — Sistem mata uang untuk unlock kosmetik
- **Color Customization** — Pewarnaan kosmetik dengan color picker

## 📋 Persyaratan

- **Java** 8+
- **Minecraft** 1.17 — 1.21.5
- **Server** Paper / Spigot
- **Maven** 3.6+ (untuk build)

## 🔧 Build

```bash
mvn clean package -pl plugin -am
```

Output JAR: `plugin/target/MagicCosmetics-3.1.1.jar`

## 📦 Modul Proyek

| Modul | Deskripsi |
|-------|-----------|
| `api` | API publik & class dasar (`Cosmetic`, `CosmeticType`) |
| `plugin` | Plugin utama, listener, database, cache |
| `v1_18_R1` — `v1_21_R4` | NMS adapter per versi Minecraft |
| `meg3_support` / `meg4_support` | Integrasi ModelEngine |
| `bungeecord` / `velocity` | Proxy support |

## 🔌 Integrasi Plugin (Soft Dependencies)

| Plugin | Fungsi |
|--------|--------|
| LuckPerms | Permission-based cosmetics |
| ItemsAdder / Nexo | Custom resource pack items |
| ModelEngine | Model 3D pada kosmetik |
| PlaceholderAPI | Placeholder support |
| Citizens | NPC cosmetics |
| HuskSync | Cross-server sync |
| WorldGuard | Region-based restrictions |
| Multiverse-Core | Multi-world support |

## 💾 Database

Mendukung **SQLite** (default) dan **MySQL**. Menggunakan HikariCP connection pool.

Konfigurasi MySQL di `config.yml`:
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

## 🎮 Perintah

| Perintah | Alias |
|----------|-------|
| `/magicosmetics` | `/cosmetics`, `/mcosmetics`, `/magiccos` |

## 📁 Struktur Kode

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

## 📝 Changelog Terbaru

### v3.1.1
- **Fix:** Helmet tidak hilang lagi saat player disconnect/quit dengan kosmetik aktif
- **Fix:** Helmet tidak hilang saat death dengan `keepInventory: true`
- **Fix:** Walking stick tidak lagi menggantikan item di off-hand
- **Fix:** `clearClose()` sekarang me-restore item tersimpan di mode non-overlaps
- **Fix:** Menghapus double `clearCosmeticsToSaveData()` yang membatalkan restorasi item
- **Fix:** `PlayerData.players` menggunakan `ConcurrentHashMap` untuk thread safety
- **Fix:** `EntityCache.entities` menggunakan `ConcurrentHashMap` untuk thread safety
- **Fix:** Typo di `EntityCache.activeWStick()` yang menggunakan `hat` bukan `wStick`
- **Fix:** HikariCP connection pool sekarang di-close saat plugin disable
- **Fix:** Null check pada `PlayerData.getPlayer()` di semua event handler

## 📄 Lisensi

Plugin ini dibuat oleh **FrancoBM**. Fork ini dikelola untuk kebutuhan server **Voltraz**.
