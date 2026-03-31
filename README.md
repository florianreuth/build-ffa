# Build FFA

A Paper plugin focused on fast-paced BuildFFA gameplay with configurable kits, arena spawns, persistent stats, combat tagging, and automatic placed-block cleanup.

## Features

- Configurable kits (`kits.yml`) with armor, items, and potion effects
- Multi-spawn arena support (`arenas.yml`) with random respawns
- Persistent player stats (`stats.yml`) including K/D and kill streaks
- Kill rewards and streak broadcasts
- Build mode with break-protection for map blocks and auto-cleanup for placed blocks
- Live HUD with scoreboard, tab list footer/header, and action bar combat/gadget status
- Configurable gadgets (heal/leap/fireball) with cooldowns and permissions

## Commands

- `/kit [name|list]` - list and select kits
- `/ffastats [player]` - show BuildFFA stats
- `/gadget [name|list]` - list and select gadgets
- `/buildffa <reload|setspawn|info>` - admin tools

## Quick Start

1. Build the jar and place it in your Paper server `plugins/` folder.
2. Start the server once to generate data files.
3. Join as admin and add arena spawns with `/buildffa setspawn`.
4. Optionally edit `kits.yml` and `config.yml`, then run `/buildffa reload`.

## Downloads

You can download the latest jar file
from [my build server](https://build.florianreuth.de/job/build-ffa), [GitHub Actions](https://github.com/florianreuth/build-ffa/actions)
or use the [releases tab](https://github.com/florianreuth/build-ffa/releases).

## Contact

If you encounter any issues, please report them on the
[issue tracker](https://github.com/florianreuth/build-ffa/issues).
If you just want to talk or need help with build-ffa, feel free to join my
[Discord](http://florianreuth.de/discord).
