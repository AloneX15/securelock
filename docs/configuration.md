# Configuration

File: `config/securemod.json`. It is created on first start; reload it with `/securemod reload`. If the file is broken, Secure Mod logs a warning and uses the defaults (it never crashes).

| Key | Default | Meaning |
|---|---|---|
| `profile` | `"custom"` | Preset: `friends` (permissive), `survival`, `factions` (raid windows on, longer lockout). Anything else keeps your values. |
| `maxFailedAttempts` | `3` | Wrong codes before a lockout. |
| `lockoutSeconds` | `30` | Lockout per player + block. |
| `codeMinLength` / `codeMaxLength` | `4` / `16` | Allowed code length (digits only, max 16). |
| `sessionSeconds` | `10` | After a correct code the player can use the block again for this long. |
| `allowHoppersFromSameOwner` | `true` | A hopper **padlocked by the same owner** can move items in/out of a protected container. |
| `comparatorOutputHidden` | `true` | Comparators read 0 from protected containers. |
| `explosionProof` | `true` | Explosions skip protected blocks. |
| `allowTeamAccess` | `true` | In *shared* mode, members of the owner's scoreboard team can use the block. |
| `protectSupportBlocks` | `true` | The block under a protected door is protected too. |
| `claimsGrantAccess` | `false` | With Open Parties and Claims or Flan: a player with container access in the claim can **use** protected blocks there (never break/configure). |
| `maxProtectedBlocksPerPlayer` | `0` | Limit per player (0 = unlimited). Creative players are not limited. |
| `inactiveOwnerDays` | `0` | Days after which an owner counts as inactive (for `purgeinactive`). |
| `autoPurgeInactiveOwners` | `false` | Purge inactive owners automatically when the server starts. |
| `maxInteractDistance` | `8.0` | Max distance for Secure Mod packets. |
| `maxPacketsPerSecond` | `10` | Rate limit for Secure Mod packets per player. |
| `ownerHud` | `true` | Show "Owned by X" under the crosshair (Jade shows it in its tooltip instead). |
| `keypadSignalTicks` / `cardReaderSignalTicks` / `scannerSignalTicks` | `60` / `60` / `40` | Redstone pulse length. |
| `auditLog` | `true` | Write `logs/securemod-audit.log`. |
| `fakePlayersUseOwnerPermissions` | `true` | Fake players from other mods (Create deployers…) are checked with their own UUID (usually the placing player). `false` = always denied. |
| `allowedFakePlayerNames` | `[]` | Fake player names that are always treated as the owner. |
| `extraLockableBlocks` | `[]` | Block ids the padlock can lock in addition to the `securemod:lockable` tag. |
| `neverLockableBlocks` | `[]` | Block ids that can never be locked (in addition to `securemod:never_lock`). |
| `raidWindowsEnabled` | `false` | Allow breaking/exploding protected blocks during raid windows. |
| `raidWindows` | `[]` | e.g. `["SAT 18:00-22:00", "ALL 21:00-21:30"]` (server time; days `MON`…`SUN` or `ALL`; windows can cross midnight). |
| `compat.disable` | `[]` | Turn off a protection if it conflicts with another mod: `hopper_protection`, `explosion_protection`, `piston_protection`, `fire_protection`, `comparator_protection`, `transfer_protection`, `mob_protection`, `golem_protection`. |

## Datapack tags
- `securemod:lockable` (block) – what the padlock can lock. Default: doors, trapdoors, fence gates, shulker boxes, copper chests, chest, trapped chest, barrel, furnaces, hopper, dropper, dispenser, brewing stand, crafter, chiseled bookshelf, lectern, jukebox, decorated pot, `#c:chests`, `#c:barrels`, `#c:shulker_boxes`.
- `securemod:never_lock` (block) – excluded. Default: ender chest, `#c:ender_chests`, Secure Mod's own blocks.

Server owners and modpack makers can change both with a datapack without touching the mod.
