# Testing

## Automated (runs in CI on every push, for every Minecraft version)
| Suite | Command | What it covers |
|---|---|---|
| Unit tests (JUnit) | `./gradlew :<v>:test` | `PasscodeHasher` (hash, salt, constant-time check, code format, 10 000-char input), `BruteForceTracker`, `RateLimiter`, `SessionTracker`, `AccessMode`, raid windows. |
| Gametests (dedicated server) | `./gradlew :<v>:runGameTest` | Hopper can't pull from / push into a padlocked chest · explosions spare padlocked chests and the support of padlocked doors · pistons can't destroy padlocked doors · strangers can't open (also sneaking) or break, the owner can · spectators can't peek · creative pick-block doesn't copy contents · both halves of a double chest are protected · reinforced door ignores external redstone and opens with the owner's keypad · own blocks are unbreakable and blast-proof · data survives serialization · data from a newer version is locked for everyone · purge creates a backup and rollback restores owner and code · 1 000 protected blocks: 100 000 lookups in well under a second · every mixin is connected · recipes, loot tables and tags load. |
| Compat pack | `./gradlew :<v>:runGameTest -PcompatPack` | The same suite with Lithium, FerriteCore, Jade, Open Parties and Claims and Flan loaded. |
| Client gametest | `./gradlew :<v>:runClientGameTest` | Creates a world, places every block, shows the owner HUD and opens the keypad and settings screens; screenshots in `versions/<v>/build/run/clientGameTest/screenshots/`. Then closes and reopens the world and checks that protections and the index persisted. |

## Manual checklist before a release
Repeat on **26.1.x, 26.2.x and 26.3.x** with a dedicated server (`./gradlew :<v>:runServer`, `online-mode=false` only locally) and two clients with different names. As the *other* player, for every protected block:

- [ ] Right-click (also sneaking and holding an item)
- [ ] Break in survival / creative without OP
- [ ] TNT, creeper, bed in the Nether, wither
- [ ] Piston pushing / pulling
- [ ] Hopper below / beside, hopper minecart
- [ ] Enderman, fire, lava
- [ ] Tampered packets (`SubmitCodeC2S` to far coordinates, 10 000-char code) → ignored or disconnected, server unaffected
- [ ] Brute force → temporary lockout
- [ ] Server restart → data persists
- [ ] Double chest: both halves protected
- [ ] Big modpack (200+ mods): starts and basic use works

Performance target: < 0.1 ms per tick with 10 000 protected blocks (measure with Spark). No SecureLock block ticks.
