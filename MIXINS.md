# Mixins

Secure Mod keeps mixins to a minimum and prefers Fabric API events (`UseBlockCallback`, `PlayerBlockBreakEvents`, `PlayerPickItemEvents`, attachments, payloads).
Every mixin:

- uses MixinExtras injectors (`@WrapOperation`, `@ModifyExpressionValue`, `@WrapWithCondition`, `@ModifyReturnValue`, `@WrapMethod`) or, as a last resort, `@Inject(cancellable = true)`. **No `@Overwrite`, no `@Redirect`.**
- has `require = 0`: if another mod changed the target, the game does not crash.
- contains a single call to `SecureModHooks`, which wraps the logic in `try/catch` and fails safe.
- names its handlers with the `securemod$` prefix.

`SecureModMixinPlugin` checks at load time that every handler is really connected to its target (including MixinExtras' late injections). A missing injection is logged once and shown in `/securemod debug`; the game keeps running. Functions can also be turned off with `compat.disable` in the config.

| Mixin | Target | Why | Feature (`compat.disable`) | What is lost if it fails |
|---|---|---|---|---|
| `ServerExplosionMixin` | `ServerExplosion.explode` (`@ModifyExpressionValue` on `calculateExplodedPositions`) | Removes protected blocks (and the support block of protected doors) from every explosion. | `explosion_protection` | Padlocked vanilla blocks can be blown up (own blocks keep their 3 600 000 blast resistance). |
| `HopperBlockEntityMixin` | `HopperBlockEntity.getBlockContainer` (`@ModifyReturnValue`), `getAttachedContainer`/`getSourceContainer` (`@WrapMethod`) | Hoppers, hopper minecarts, droppers and crafters can't insert into or extract from protected containers. Remembers the source hopper so a hopper padlocked by the same owner is allowed (config). | `hopper_protection` | Hoppers can empty padlocked containers. |
| `PistonStructureResolverMixin` | `PistonStructureResolver.resolve` (`@ModifyReturnValue`) | A piston can't push, pull or destroy a protected block or the support of a protected door. | `piston_protection` | Pistons can break padlocked doors (own blocks are immovable anyway). |
| `FireBlockMixin` | `FireBlock.checkBurnOut` (`@WrapOperation` / `@WrapWithCondition`) | Fire doesn't burn padlocked wooden blocks. | `fire_protection` | Fire can burn padlocked wooden doors and gates. |
| `WitherBossMixin` | `WitherBoss.customServerAiStep` (`@WrapOperation` on `destroyBlock`) | The wither doesn't destroy protected blocks. | `mob_protection` | The wither can break padlocked blocks (own blocks are in `#minecraft:wither_immune`). |
| `EnderDragonMixin` | `EnderDragon.checkWalls` (`@WrapOperation` on `removeBlock`) | The ender dragon doesn't destroy protected blocks. | `mob_protection` | The dragon can break padlocked blocks (own blocks are in `#minecraft:dragon_immune`). |
| `DoorBlockMixin` | `DoorBlock.isWoodenDoor(Level, BlockPos)` (`@ModifyReturnValue`) | Zombies don't break and illagers don't open padlocked wooden doors. | `mob_protection` | Zombies on Hard can break padlocked wooden doors. |
| `ComparatorBlockMixin` | `ComparatorBlock.getInputSignal` (`@WrapOperation` on `getAnalogOutputSignal`) | Optional (`comparatorOutputHidden`): a comparator reads 0 from a protected container. | `comparator_protection` | Comparators can reveal how full a padlocked container is. |
| `TransportItemsBetweenContainersMixin` | `TransportItemsBetweenContainers.isTargetValidToPick` (`@Inject` at RETURN) | Copper golems don't take from or put into protected containers. | `golem_protection` | Copper golems can move items out of padlocked copper chests. |
| `BlockApiLookupImplMixin` | Fabric API `BlockApiLookupImpl.find` (`@ModifyReturnValue`, only if `fabric-transfer-api-v1` is loaded) | Storage mods (Create, Tom's, AE2-like, Lithium's optimized hoppers…) that use the Transfer API see no storage on protected containers. | `transfer_protection` | Mods that use the Transfer API can extract from padlocked containers. |

Tested automatically on every supported Minecraft version by the `mixinsAreConnected` gametest, and with Lithium, Jade, Open Parties and Claims, Flan and FerriteCore loaded (`./gradlew runGameTest -PcompatPack`).
