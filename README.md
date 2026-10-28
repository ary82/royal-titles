# Royal Titles

A lightweight Forge 1.20.1 mod for op-granted titles like **Steve, [Dragonslayer]**.

- Ops define titles and rarities (each with a color) and grant titles to players.
- Players pick one **main title**. It shows in chat and the tab list, and above their head if they turn that on.
- Press **J** to open a screen with every player's titles.

## Players

On the titles screen (**J**, rebindable), hover a title to read its description. Click one of
yours to equip it, or click it again to unequip it. **Nametag: On/Off** toggles the title above your head.

The same from chat: `/royal equip <title>`, `/royal unequip`, `/royal nametag on|off`.

## Operators (permission level 2)

| Command                                    | Does                                              |
| ------------------------------------------ | ------------------------------------------------- |
| `/royal title create <id> <rarity> <name>` | Define a title                                    |
| `/royal title describe <id> <text>`        | Set the hover description                         |
| `/royal title rename <id> <name>`          | Change the display name                           |
| `/royal title rarity <id> <rarity>`        | Change a title's rarity                           |
| `/royal title delete <id>`                 | Delete a title (removed from everyone)            |
| `/royal title list`                        | List titles                                       |
| `/royal rarity set <name> <color>`         | Add a rarity, or recolor one (`FFD700` or `gold`) |
| `/royal rarity remove <name>` / `list`     | Manage rarities                                   |
| `/royal grant <players> <id>`              | Give a title (works for offline players)          |
| `/royal revoke <players> <id>`             | Take a title away                                 |
| `/royal reload`                            | Reload the config file after editing it by hand   |

```
/royal rarity set mythic FF5555
/royal title create dragonslayer mythic Dragonslayer
/royal title describe dragonslayer Defeated the Ender Dragon.
/royal grant Steve dragonslayer
```

## Config

Titles and rarities live in `config/royal_titles.json` on the server. The commands above edit it,
or you can edit it by hand and run `/royal reload`. Rarities are listed from most common to rarest.

```json
{
  "rarities": {
    "common": "#AAAAAA",
    "legendary": "#FFAA00"
  },
  "titles": {
    "dragonslayer": {
      "name": "Dragonslayer",
      "description": "Defeated the Ender Dragon.",
      "rarity": "legendary"
    }
  }
}
```

If the file has a syntax error, the server keeps the last good version and won't overwrite your edit.
Who owns and has equipped which title is stored in the world save.

Titles don't use scoreboard teams, so they won't conflict with team-based mods.

## Install

Minecraft 1.20.1 with Forge 47.x. Install on the server **and** on every client.

## Build

```
nix develop        # JDK 17 + Gradle 8, opens your zsh
./gradlew build    # jar in build/libs/
./gradlew runClient / runServer
```

If `runClient` fails with a graphics/driver error, run it from outside the Nix shell.

## AI Disclosure

Parts of this mod's code and docs were written with help from an AI assistant (Claude). All of it was reviewed and tested by the author.

## License

[LGPL-3.0-only](LICENSE) (builds on the GPL-3.0 in [COPYING](COPYING))
