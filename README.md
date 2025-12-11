# ItemBlocker

Advanced Item Control Plugin for Minecraft Servers

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green.svg)](https://www.minecraft.net/)

**Control what items players can use on your Minecraft server**

---

## Overview

ItemBlocker is a Minecraft plugin that lets you control what items players can use on your server. Block crafting, pickup, usage, and more with simple commands and configuration.

### Features

- Multi-Language - English and Polish included
- 6 Block Types - Control crafting, pickup, drop, use, place, and armor
- Fast - Minimal server impact
- Permissions - Control who can bypass blocks
- Hot Reload - No restart needed for config changes

---

## Block Actions

Control different ways players interact with items:

| Action      | Description                                  | Configuration Key        |
| ----------- | -------------------------------------------- | ------------------------ |
| Crafting    | Prevent item creation through crafting table | `block-actions.crafting` |
| Pickup      | Block item pickup from ground                | `block-actions.pickup`   |
| Drop        | Prevent item dropping                        | `block-actions.drop`     |
| Use         | Block item usage (right-click)               | `block-actions.use`      |
| Place       | Prevent block placement                      | `block-actions.place`    |
| Armor Equip | Prevent armor equipping                      | `block-actions.armor`    |

---

## Commands

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

## Permissions

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

## Installation

### Requirements

- Java 21+
- Paper or Spigot 1.21+

### Build

```bash
# Clone repository
git clone https://github.com/kuqk/ItemBlocker.git
cd ItemBlocker

# Build with Maven
mvn clean package

# Output: target/ItemBlocker-1.0.0.jar
```

### Install

1. Put `ItemBlocker-1.0.0.jar` in `plugins/` folder
2. Restart server
3. Check: `/plugins` (should show ItemBlocker in green)
4. Use: `/ib help`

---

## Config

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
  # Add more items...
```

**Language files** (in `languages/` folder):

- `messages_en.yml` - English
- `messages_pl.yml` - Polish

You can edit all messages and use `&` color codes.

---

## Usage

**Basic commands:**

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

# Grant admin full access
/lp group admin permission set itemblocker.* true
```

---

## Project Structure

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
│   │   └── ArmorListener.java
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

## API

For plugin developers:

```java
// Get ItemBlocker instance
itemBlocker plugin = itemBlocker.getInstance();

// Check if item is blocked
Material material = Material.DIAMOND;
boolean isBlocked = plugin.getBlockedItemsManager().isBlocked(material);

// Add item programmatically
plugin.getBlockedItemsManager().addBlockedItem(Material.TNT);

// Remove item
plugin.getBlockedItemsManager().removeBlockedItem(Material.TNT);

// Get all blocked items
Set<Material> blockedItems = plugin.getBlockedItemsManager().getBlockedItems();
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 kuqk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## Support

**Issues:** [GitHub Issues](https://github.com/kuqk/ItemBlocker/issues)

**Discussions:** [GitHub Discussions](https://github.com/kuqk/ItemBlocker/discussions)

---

**Made by kuqk**
