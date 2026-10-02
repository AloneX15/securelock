# Changelog

All notable changes to SecureLock are documented here. The project follows [Semantic Versioning](https://semver.org/).
Changes that affect existing configs or worlds are marked **⚠ BREAKING**.

## [Unreleased]

## [1.0.0] - not yet published
First version, for Minecraft 26.1.x, 26.2.x and 26.3.x.

### Added
- Blocks: reinforced iron and oak doors, reinforced iron trapdoor, reinforced oak fence gate, passcode chest (single and double), passcode barrel, keypad, keycard reader, retina scanner.
- Items: padlock (locks any vanilla or modded container/door without replacing it), universal block modifier, keycards level 1–5, card writer, universal block remover, admin tool.
- Server-side authorization, salted SHA-256 passcodes, brute-force lockout, packet rate limiting, audit log (`logs/securelock-audit.log`).
- Protection against breaking, explosions, pistons, hoppers/hopper minecarts/droppers, copper golems, withers, the ender dragon, door-breaking zombies, fire, comparators (optional), the Fabric Transfer API, creative pick-block and spectators.
- Commands `/securelock info|trust|untrust|trusted|list|inspect|debug|reload` and `/securelock admin unlock|transfer|purge|purgeinactive|unlockall|export|rollback`.
- Automatic backup before purges and `unlockall`, restorable with `/securelock admin rollback`.
- Per-rank limit with the integer permission `securelock:limit`; 5-second permission cache per player.
- Sounds, particles and action-bar messages on denied and granted access.
- Permissions through the Fabric Permission API (LuckPerms compatible) with OP level fallback.
- Config `config/securelock.json` with profiles (`friends`, `survival`, `factions`), raid windows and `compat.disable`.
- Optional integrations: Jade (owner tooltip), Open Parties and Claims and Flan (claim access can grant use).
- Public API: `SecureLockApi` and `SecureLockEvents` (`ACCESS_DENIED`, `BLOCK_LOCKED`, `BLOCK_UNLOCKED`).
- Languages: English, Spanish (es_es, es_mx, es_ar), Brazilian Portuguese, French, German.
