# BBLink

Chat relay between Discord and Minecraft 1.7.10 servers, built for GT New Horizons (Forge 10.13.4.1614).
Runs on Java 8, and on Java 17+ through lwjgl3ify.

## Building

```sh
./gradlew build
```

The mod jar is `projects/minecraft/v1.7.10/forge/build/libs/bblink-1.7.10-<version>.jar` (built with
[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle)). It only needs to be on the server.

## Configuration

Settings are in `config/bblink.toml`, which is created on first start.

## Linking multiple servers

Several servers on the same machine can share one Discord channel and see each other's chat in-game.
Set these in each server's `config/bblink.toml`:

```toml
# Unique per server. Shown as a [tag] in Discord and on the other servers.
serverName = "GTNH Main"
# Same absolute path on every linked server.
sharedDatabasePath = "/srv/minecraft/bblink-shared.db"
# Also relay joins/leaves/deaths/achievements/start/stop between servers (chat is always relayed).
crossServerEvents = true
```

How messages flow (each message is shown once everywhere):

* **In-game → Discord:** each server posts only its own messages, tagged with its `serverName`.
* **Discord → in-game:** each server's bot reads the channel and shows messages from people. Messages from bots
  and webhooks (including the other servers' relays) are ignored.
* **In-game → other servers:** through the shared SQLite database, not through Discord. Each server reads new rows
  in order and skips its own, so nothing is shown twice. History isn't replayed when a server starts.

Each server can use its own bot token (recommended: each bot then shows its own player count), or they can share
one. Discord commands run on every linked server unless you name one: `!list` lists every server's players, and
`!GTNHMain stop` stops only `GTNH Main` (spaces in the name are left out).

The database needs to be on a local disk. SQLite's locking doesn't work reliably over network filesystems.

## Player names

Messages use the player's nickname (from nickname mods using Forge's name format event) or their account name. Colour codes, gradients,
team prefixes and decorative characters (symbols, emoji, resource-pack icons, invisible characters) are removed.

## Development Notes

If editing the buildscripts with IntelliJ and references are missing then you need
to [manually clear the cache](https://youtrack.jetbrains.com/issue/KT-42950#focus=Comments-27-4608898.0-0).
