# Blocks, items and recipes

All SecureLock blocks are **unbreakable in survival** (hardness −1), have a blast resistance of 3 600 000, can't be moved by pistons and are immune to withers and the ender dragon. The owner gets them back with the **Universal Block Remover**; in creative the owner (or an admin) can break them normally.

When you place a SecureLock block you become its owner. Chests, barrels and keypads open the settings panel right away so you can set a code.

## Blocks

### Reinforced Iron Door / Reinforced Oak Door
```
 I       I = iron ingot
IXI      X = iron door / oak door
 O       O = obsidian
```
- Opens by hand for the owner, allowed players, trusted players and (in *shared* mode) team members. Everyone else sees *"Locked by …"*. If a code is set, strangers get the keypad.
- Ignores external redstone. It only opens with a keypad, keycard reader or retina scanner **of the same owner**, placed right next to the door or on a solid block next to it.
- Doesn't fall if the block below is broken; that block is also protected.
- The oak version is not a "wooden door" for mobs: villagers don't open it and zombies don't break it. Wind charges don't open either version.

### Reinforced Iron Trapdoor, Reinforced Oak Fence Gate
Same recipe pattern with an iron trapdoor / oak fence gate in the middle. Same rules as the door.

### Passcode Chest / Passcode Barrel
```
IRI      I = iron ingot
ICI      R = redstone
III      C = chest / barrel
```
- Strangers get a keypad. The right code opens it and keeps a 10‑second session (`sessionSeconds`).
- 3 wrong codes lock that player out of that block for 30 s (`maxFailedAttempts`, `lockoutSeconds`).
- The passcode chest only connects into a double chest with another passcode chest **of the same owner**.
- It uses the vanilla chest/barrel block entity, so sorting, storage and rendering mods treat it like a chest. It is in the `c:chests` / `c:barrels` tags.

### Keypad
```
BBB      B = stone button
IRI      I = iron ingot
III      R = redstone
```
Place it on a wall, floor or ceiling like a button. The owner and allowed players activate it directly; everyone else types the code. Emits a redstone signal for `keypadSignalTicks` (60).

### Keycard Reader
```
III      I = iron ingot
IRP      R = redstone
III      P = paper
```
Right‑click it with a keycard **linked to the reader's owner** whose level is equal or higher than the reader's level (set it with the Universal Block Modifier, 1–5). Emits redstone for `cardReaderSignalTicks`.

### Retina Scanner
```
III      I = iron ingot
IEI      E = eye of ender
IRI      R = redstone
```
Right‑click it: it activates only for the owner, allowed players, trusted players and (shared mode) team members.

## Items

| Item | Recipe | Use |
|---|---|---|
| Padlock | ` N ` / `N N` / `III` (N = iron nugget, I = iron ingot) | Right‑click a lockable block to lock it. Works on vanilla and modded blocks (tag `securelock:lockable`). The block is not replaced. |
| Universal Block Modifier | `  R` / ` I ` / `I  ` (R = redstone) | Owner panel: mode, allowed/blocked players, code, keycard level. |
| Universal Block Remover | `  D` / ` I ` / `I  ` (D = diamond) | Owner: removes a padlock (you get it back) or drops a SecureLock block as an item. 64 uses. |
| Card Writer | `III` / `PRP` / `III` | Hold it and a keycard in the other hand, right‑click the air: the card is linked to you. |
| Keycard Lv1 | shapeless: 2 paper + redstone + iron ingot | |
| Keycard Lv2–Lv5 | shapeless: previous level + gold ingot / diamond / emerald / netherite scrap | |
| Admin Tool | creative / `/give` only | Requires `securelock:admin.inspect`. Right‑click: full info. Sneak + right‑click (`securelock:admin.remove`): force unlock. |

## Access modes
| Mode | Who can use the block |
|---|---|
| Private | Owner, allowed players, globally trusted players (`/securelock trust`), anyone with the code (for a few seconds). |
| Shared | Private + members of the owner's scoreboard team (`allowTeamAccess`). |
| Public | Anyone, except blocked players. Only the owner can break or configure it. |

Blocked players are always denied (except admins with `securelock:admin.bypass`).
