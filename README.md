# Secure Mod

*Designed by TakumiStudios.*

**Security for Fabric servers.** Reinforced doors, passcode chests and barrels, keypads, keycards, retina scanners, padlocks for *any* container (vanilla or modded) and first-class admin tools. Every check runs on the server.

> *Leer en español: [README.es.md](README.es.md)*

| Minecraft | Loader | Java | Jar |
|---|---|---|---|
| 26.3.x | Fabric Loader ≥ 0.19.5 + Fabric API | 25 | `securemod-<version>+mc26.3.jar` |
| 26.2.x | Fabric Loader ≥ 0.19.5 + Fabric API | 25 | `securemod-<version>+mc26.2.jar` |
| 26.1.x | Fabric Loader ≥ 0.19.5 + Fabric API | 25 | `securemod-<version>+mc26.1.2.jar` |

Required on **client and server** (it adds blocks, items and screens). The only hard dependency is **Fabric API**.

## Features

### Blocks
| Block | What it does |
|---|---|
| Reinforced Iron Door / Reinforced Oak Door | Only the owner and allowed players open it (even the iron one, by hand). Unbreakable for everyone else, blast‑proof, immovable, ignores external redstone, does not fall if its support is broken. |
| Reinforced Iron Trapdoor / Reinforced Oak Fence Gate | Same rules as the door. |
| Passcode Chest (single and double) / Passcode Barrel | Strangers get a keypad; the right code opens it and is remembered for a few seconds. Uses the vanilla block entity, so storage, sorting and rendering mods keep working. |
| Keypad | Emits redstone for N ticks when the right code is entered. |
| Keycard Reader | Opens with a level 1–5 keycard linked to the owner. |
| Retina Scanner | Opens only for the owner and whitelisted players. |

Reinforced doors, trapdoors and gates only react to keypads, readers and scanners **of the same owner** (placed next to them or on an adjacent solid block).

### Items
- **Padlock** – locks *any* chest, door, trapdoor, gate, barrel, shulker box, furnace… vanilla or from other mods. It does **not** replace the block: the lock is stored as data attached to the chunk.
- **Universal Block Modifier** – owner panel: mode (private / shared with team / public), allowed and blocked players, passcode, keycard level.
- **Keycards (levels 1–5)** + **Card Writer** – hold the writer in one hand and a card in the other, right‑click: the card is linked to you.
- **Universal Block Remover** – the owner gets their protected block back (or removes their padlock).
- **Admin Tool** (OP / permission only) – right‑click shows full info, sneak + right‑click force‑unlocks.

### Server security
- 100 % server‑side authorization. The client only sends intents; the server checks distance, dimension, that the block exists and the permission.
- Passcodes are stored as `SHA‑256(salt + code)` with a random salt per block and never leave the server.
- Anti brute force (3 failures → 30 s lockout per player + block, configurable), packet rate limiting, audit log.
- Bypass protection: breaking, explosions (TNT, creepers, beds, withers), pistons, hoppers, hopper minecarts, droppers, copper golems, endermen/withers/ender dragon, zombies breaking doors, fire, comparators (optional), the Fabric Transfer API (storage mods), creative pick‑block, spectators.
- A protected block's support block is protected too.

### Admin tools
`/securemod info | trust | untrust | trusted | list | inspect | debug | reload` and `/securemod admin unlock | transfer | purge | purgeinactive | unlockall | export`, LuckPerms‑compatible permission nodes (Fabric Permission API), audit log with per‑block history, automatic purge of inactive owners, per‑player limits, raid windows for faction servers. See [docs/commands-and-permissions.md](docs/commands-and-permissions.md).

## Documentation
- [Blocks, items and recipes](docs/blocks-and-items.md)
- [Commands and permissions](docs/commands-and-permissions.md)
- [Configuration](docs/configuration.md)
- [Admin guide (audit log, purge, uninstalling)](docs/admin-guide.md)
- [Compatibility](docs/compatibility.md) and [MIXINS.md](MIXINS.md)
- [Public API for other mods](docs/api.md)
- [Testing](docs/testing.md) · [FAQ](docs/faq.md)
- [Implementation status vs. the plan](docs/implementation-status.md)

## Building
```
./gradlew buildAndCollect          # jars for every version in build/libs/<version>/
./gradlew :26.3:runClient          # dev client
./gradlew :26.3:runServer          # dev dedicated server
./gradlew :26.3:test               # unit tests
./gradlew :26.3:runGameTest        # bypass test suite in a real world
./gradlew :26.3:runGameTest -PcompatPack   # same, with Lithium, Jade, OPAC, Flan, FerriteCore loaded
./gradlew :26.3:runClientGameTest  # client test: renders every block and screen, takes screenshots
```
The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build every Minecraft version from one source tree. Version‑specific code is marked with `//? if >=26.2 { ... }` comments. Versions live in `stonecutter.properties.toml`.

## Security reports
Please report vulnerabilities privately — see [SECURITY.md](SECURITY.md).

## License
[MIT](LICENSE) © TakumiStudios
