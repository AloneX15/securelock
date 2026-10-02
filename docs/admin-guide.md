# Admin guide

## Audit log
`logs/securemod-audit.log`, one line per event:
```
2026-10-02 18:21:07 | ACCESS_DENIED | Griefer | minecraft:overworld 120 64 -33 | minecraft:chest
```
Events: `PLACE`, `LOCK`, `UNLOCK`, `BREAK`, `ACCESS_DENIED`, `CODE_OK`, `CODE_FAIL`, `CODE_LOCKOUT`, `CODE_CHANGED`, `ALLOW_ADD/REMOVE`, `DENY_ADD/REMOVE`, `MODE_CHANGED`, `CARD_LEVEL_CHANGED`, `CARD_LINKED`, `ADMIN_BYPASS`, `ADMIN_UNLOCK`, `ADMIN_TRANSFER`, `ADMIN_PURGE`, `ADMIN_UNLOCK_ALL`, `TRUST`, `UNTRUST`, `PACKET_REJECTED`.
Passcodes are never written. `/securemod inspect` shows the last 10 lines of the block you look at.

## Where data lives
- Every protection is stored **in the chunk** with the Fabric Data Attachment API (`securemod:chunk_locks`): it moves with the region file and never needs chunks to be loaded to be checked.
- A per-world index (SavedData `securemod:protected_blocks` in the world's `data` folder) keeps the blocks per player, global trust and last-seen dates (for `/securemod list`, limits and purges).
- The passcode hash and salt never leave the server.

## Inactive players and bans
```
/securemod admin purge <player>
/securemod admin purgeinactive 60
```
Or set `inactiveOwnerDays` + `autoPurgeInactiveOwners` to purge at startup. Purged Secure Mod blocks become their vanilla version with their contents.

## Limits per rank
Set `maxProtectedBlocksPerPlayer` as the default and override it per rank with the integer permission `securemod:limit` (for example with LuckPerms meta).

## Backups and rollback
Before every `purge`, `purgeinactive` and `unlockall`, Secure Mod writes a full backup (including passcode hashes) to `securemod-backups/<date>-<reason>.json`. Keep that folder private. To undo:
```
/securemod admin rollback 20261002-181500-purge-Griefer.json
```
Protections are restored where the block still exists; Secure Mod blocks that were converted to vanilla come back as padlocks.

## Uninstalling without losing anything
1. `/securemod admin export` (optional backup).
2. `/securemod admin unlockall confirm` — removes every padlock and turns Secure Mod blocks into their vanilla equivalents **keeping their contents** (reinforced iron door → iron door, passcode chest → chest, …). Keypads, readers and scanners are removed.
3. Remove the jar.

If the mod is removed without step 2: padlocks on vanilla/modded blocks simply disappear (they are attached data, the block stays intact), and Secure Mod's own blocks disappear like any modded block.

## Faction / raid servers
Use `"profile": "factions"` and set `raidWindows`. During a window protected blocks can be broken and blown up; outside it they can't.

## Corrupt or newer data
If a protection can't be read or comes from a newer Secure Mod version, the block stays **closed for everyone except admins** and the problem is logged.
