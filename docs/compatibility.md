# Compatibility

**Golden rule:** if something fails inside SecureLock, that function is disabled, a warning is logged and the game keeps running. SecureLock never crashes the game and never stops another mod from loading.

- The only required dependency is **Fabric API**. Jade, LuckPerms, Open Parties and Claims and Flan are `suggests`.
- Padlocks never replace blocks: a padlocked chest is still a `ChestBlock`, a modded chest is still the modded chest.
- SecureLock's own blocks extend the vanilla classes (`DoorBlock`, `ChestBlock`, `BarrelBlock`, `TrapDoorBlock`, `FenceGateBlock`, `ButtonBlock`) and the passcode chest/barrel use the vanilla block entities.
- Mixins: see [MIXINS.md](../MIXINS.md). Problems can be worked around with `compat.disable` while a fix is released.

## Tested together (automated)
`./gradlew :<version>:runGameTest -PcompatPack` runs the whole bypass suite with these mods loaded, on 26.1.2, 26.2 and 26.3:

| Mod | Why it matters | Result |
|---|---|---|
| Lithium | Rewrites hopper logic | ✅ hopper tests pass |
| FerriteCore | Changes block state internals | ✅ |
| Jade | Tooltip integration | ✅ plugin loads |
| Open Parties and Claims (+ Forge Config API Port) | Claims integration | ✅ |
| Flan | Claims integration | ✅ |

## Known notes
| Mod | Notes |
|---|---|
| Sodium / Iris | Only standard JSON models and the vanilla chest renderer are used; no rendering mixins. |
| Create | Deployers are fake players: they act with their own UUID (see `fakePlayersUseOwnerPermissions`). Contraptions can't move SecureLock blocks (unbreakable, immovable). Storage access through the Transfer API is denied on protected containers. |
| Storage mods (Tom's, Expanded Storage, AE2-like) | Protected containers expose no storage through the Transfer API. |
| Carpet | Bots are players and follow the same rules. |
| Sorting mods (IPN, Mouse Tweaks) | If the player has access, the menu is a normal vanilla menu. |
| Polymer / Geyser (server-only mode) | Planned for v1.x. |
| Jade inside Fabric's *client gametest* runner | Jade 26.3.4 crashes there by itself (also without SecureLock); this only affects that test runner. |

## Reporting an incompatibility
Open an issue with the **Compatibility** template (mod list, versions, log). Target: a fix in less than 72 hours for crashes caused by SecureLock. Every fixed incompatibility becomes a gametest.
