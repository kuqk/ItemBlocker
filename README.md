# 🛡️ ItemBlocker

Advanced Item Control Plugin for Minecraft Servers

[![Java](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/built-with/java_vector.svg)](https://www.oracle.com/java/) [![Paper](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg)](https://papermc.io) [![Spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg)](https://www.spigotmc.org) [![Purpur](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg)](https://purpurmc.org) [![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat-square)](https://opensource.org/licenses/GPL-3.0) [![Version](https://img.shields.io/badge/Version-1.0.1-orange.svg?style=flat-square)](https://github.com/kuqk/ItemBlocker/releases)

**Take full control over what items players can use on your Minecraft server**

---

## ✨ Overview

ItemBlocker is a lightweight, powerful Minecraft plugin that lets you control what items players can interact with. Block crafting, pickup, usage, container interaction and more with simple commands and configuration.

### 🌟 Features

- 🌍 **Multi-Language** - English and Polish included out of the box
- 📦 **7 Block Types** - Control crafting, pickup, drop, use, place, armor, and inventory interaction
- ⚡ **Performance** - Minimal server impact, optimized event handling
- 🔑 **Permissions** - Flexible controls for VIP and Staff bypass
- ⚙️ **Hot Reload** - No restart needed for configuration changes
- 💬 **Anti-Spam** - Built-in message cooldown system

---

## 🛡️ Block Actions

Control different ways players interact with items:

| Action      | Description                                  | Configuration Key         |
| ----------- | -------------------------------------------- | ------------------------- |
| Crafting    | Prevent item creation through crafting table | `block-actions.crafting`  |
| Pickup      | Block item pickup from ground                | `block-actions.pickup`    |
| Drop        | Prevent item dropping                        | `block-actions.drop`      |
| Use         | Block item usage (right-click)               | `block-actions.use`       |
| Place       | Prevent block placement                      | `block-actions.place`     |
| Armor Equip | Prevent armor equipping                      | `block-actions.armor`     |
| Inventory   | Block moving items in chests/containers      | `block-actions.inventory` |

---

## 💻 Commands

All commands support tab completion:

```bash
/itemblocker add <item>      # Add item to blocked list
/itemblocker remove <item>   # Remove item from blocked list
/itemblocker addhand         # Add item from hand
/itemblocker list            # Display all blocked items
/itemblocker check <item>    # Check item blocking status
/itemblocker reload          # Reload configuration
/itemblocker help            # Display help menu
```

**Aliases:** `/ib`, `/blocker`

---

## 🔑 Permissions

```yaml
itemblocker.*              # Full access (admin)
itemblocker.add            # Add blocked items
itemblocker.remove         # Remove blocked items
itemblocker.list           # View blocked items
itemblocker.reload         # Reload configuration
itemblocker.addhand        # Add item from hand
itemblocker.check          # Check item status
itemblocker.bypass         # Bypass all blocks (VIP/Staff)
```

---

## 🚀 Installation

### 📋 Requirements

- Java 21+
- Paper or Spigot 1.21+

### 🛠️ Build

```bash
# Clone repository
git clone https://github.com/kuqk/ItemBlocker.git
cd ItemBlocker

# Build with Maven
mvn clean package

# Output: target/ItemBlocker-1.0.1.jar
```

### 📥 Install

1. Put `ItemBlocker-1.0.1.jar` in `plugins/` folder
2. Restart server
3. Check: `/plugins` (should show ItemBlocker in green)
4. Use: `/ib help`

---

## 📄 Configuration

**config.yml**

```yaml
# Set language (en/pl)
language: "en"

# Which actions to block
block-actions:
  crafting: true
  pickup: true
  drop: true
  use: true
  place: true
  armor: true
  inventory: true

# Bypass settings
bypass-permission:
  enabled: true

# Debug mode
debug-mode: false
```

**blocked-items.yml**

```yaml
blocked-items:
  - SPAWNER
  - END_PORTAL_FRAME
  - DRAGON_EGG
```

**Language files** (in `languages/` folder):

- `messages_en.yml` - English
- `messages_pl.yml` - Polish

---

## 💡 Examples

**Basic management:**

```bash
# Block TNT
/ib add TNT

# Block item in hand
/ib addhand

# Check if diamond is blocked
/ib check DIAMOND

# View all blocked items
/ib list

# Remove bedrock from list
/ib remove BEDROCK

# Reload configuration
/ib reload
```

**Setting up permissions (LuckPerms example):**

```bash
# Grant VIP bypass permission
/lp group vip permission set itemblocker.bypass true

# Grant moderator management permissions
/lp group moderator permission set itemblocker.add true
/lp group moderator permission set itemblocker.remove true
/lp group moderator permission set itemblocker.reload true
```

---

## 📂 Project Structure

```
ItemBlocker/
├── src/main/java/pl/variant/
│   ├── itemBlocker.java          # Main plugin class
│   ├── commands/
│   │   └── ItemBlockerCommand.java
│   ├── listeners/
│   │   ├── CraftListener.java
│   │   ├── PickupListener.java
│   │   ├── DropListener.java
│   │   ├── UseListener.java
│   │   ├── PlaceListener.java
│   │   ├── ArmorListener.java
│   │   └── InventoryListener.java # New in 1.0.1
│   ├── managers/
│   │   ├── ConfigManager.java
│   │   ├── MessageManager.java
│   │   └── BlockedItemsManager.java
│   └── utils/
│       └── MessageCooldown.java
└── src/main/resources/
    ├── config.yml
    ├── blocked-items.yml
    ├── languages/
    │   ├── messages_en.yml
    │   └── messages_pl.yml
    └── plugin.yml
```

---

## 🛠️ API

For plugin developers:

```java
// Get ItemBlocker instance
itemBlocker plugin = itemBlocker.getInstance();

// Check if item is blocked
Material material = Material.DIAMOND;
boolean isBlocked = plugin.getBlockedItemsManager().isBlocked(material);

plugin.getBlockedItemsManager().addBlockedItem(Material.TNT);
```

---

## ⚖️ License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

```
GNU General Public License v3.0

Copyright (c) 2026 kuqk
```

---

## 🆘 Support

- 🐛 **Issues:** [GitHub Issues](https://github.com/kuqk/ItemBlocker/issues)

---

**Made with ❤️ by kuqk**
