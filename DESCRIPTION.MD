# ItemBlocker

**Take full control over what items players can use on your server**

ItemBlocker is a lightweight, powerful plugin that lets you block specific items from being used, crafted, picked up, and more. Perfect for controlling overpowered items, preventing griefing, or creating custom gameplay experiences.

## Features

### Complete Item Control

Block items in 7 different ways - prevent crafting, picking up, dropping, using, placing blocks, equipping armor, and moving items in containers. Each action can be toggled independently for maximum flexibility.

### Smart Message System

Players get clear feedback when they try to use blocked items, with built-in cooldown system to prevent message spam. All messages are fully customizable with color codes.

### Multi-Language Ready

Switch between English and Polish instantly, or create your own language file. Perfect for international servers.

### Simple Management

- Add items with `/ib add <item>` or `/ib addhand` for the item in your hand
- Remove items just as easily with `/ib remove <item>`
- Check status of any item with `/ib check <item>`
- Full tab completion on all commands

### Flexible Permissions

Give VIP players or staff the ability to bypass blocks with `itemblocker.bypass` permission. Works with any permission plugin (LuckPerms, PermissionsEx, etc.)

### Zero Performance Impact

Optimized event handling ensures your server runs smoothly even with hundreds of blocked items. Tested on servers with 100+ concurrent players.

### Live Configuration

Update your config and reload with `/ib reload` - no server restart needed. Change languages, modify blocked items, or adjust settings on the fly.

## Quick Start

1. Download and place in `plugins/` folder
2. Restart server
3. Use `/ib add <item>` to block items
4. Configure in `plugins/ItemBlocker/`

## Commands

All commands support tab completion:

```
/ib add <item>       - Block an item
/ib remove <item>    - Unblock an item
/ib addhand          - Block item in hand
/ib list             - Show all blocked items
/ib check <item>     - Check if item is blocked
/ib reload           - Reload configuration
/ib help             - Show help
```

**Aliases:** `/itemblocker`, `/blocker`

## Permissions

```
itemblocker.*        - Full access
itemblocker.add      - Add blocked items
itemblocker.remove   - Remove blocked items
itemblocker.bypass   - Bypass all blocks (VIP/Staff)
itemblocker.reload   - Reload config
itemblocker.list     - View blocked items
```

## Configuration

**Simple config.yml:**

```yaml
# Language (en/pl)
language: "en"

# Block actions
block-actions:
  crafting: true
  pickup: true
  drop: true
  use: true
  place: true
  armor: true
  inventory: true

# Bypass permission
bypass-permission:
  enabled: true
```

**Customize messages:**

All messages are fully customizable with color codes in `languages/messages_en.yml` and `messages_pl.yml`

## Examples

**Block dangerous items:**

```
/ib add TNT
/ib add BEDROCK
/ib add COMMAND_BLOCK
```

**Block item in hand:**

```
/ib addhand
```

**Grant VIP bypass (LuckPerms):**

```
/lp group vip permission set itemblocker.bypass true
```

## Requirements

- Java 21+
- Paper or Spigot 1.21+

## Support

- **Issues:** [GitHub Issues](https://github.com/kuqk/ItemBlocker/issues)
- **Source:** [GitHub Repository](https://github.com/kuqk/ItemBlocker)

## Why ItemBlocker?

- ✅ Lightweight and fast
- ✅ Easy to use
- ✅ Highly configurable
- ✅ Active development
- ✅ Open source (MIT License)
- ✅ Multi-language support
- ✅ No dependencies required

Perfect for survival servers, minigames, custom gamemodes, and more!
