# Commands and permissions

Permissions use the **Fabric Permission API** (built into Fabric API), so LuckPerms and other providers work out of the box. If no provider answers, the fallback in the table is used.

Node names are identifiers in the `securemod` namespace, e.g. `securemod:admin.bypass`.

## Commands
| Command | Node | Fallback | What it does |
|---|---|---|---|
| `/securemod info` | `securemod:command.info` | everyone | Info about the block you are looking at. Strangers only see owner and mode. |
| `/securemod trust <player>` | `securemod:command.trust` | everyone | The player can use **all** your protected blocks. |
| `/securemod untrust <player>` | `securemod:command.trust` | everyone | |
| `/securemod trusted` | `securemod:command.trust` | everyone | Lists your trusted players. |
| `/securemod list` | `securemod:command.list` | everyone | Your protected blocks with coordinates (and the limit). |
| `/securemod inspect` | `securemod:admin.inspect` | OP 2 | Full info + the last 10 audit entries of the block. |
| `/securemod debug` | `securemod:command.debug` | OP 2 | Mixin status, `compat.disable`, totals, raid window. |
| `/securemod reload` | `securemod:command.reload` | OP 2 | Reloads `config/securemod.json`. |
| `/securemod admin unlock` | `securemod:command.admin` | OP 2 | Removes the protection of the block you look at (Secure Mod blocks become their vanilla version, contents kept). |
| `/securemod admin transfer <player>` | `securemod:command.admin` | OP 2 | Changes the owner of the block you look at. |
| `/securemod admin purge <player>` | `securemod:command.admin` | OP 2 | Removes all protections of a player (banned/inactive). |
| `/securemod admin purgeinactive [days]` | `securemod:command.admin` | OP 2 | Purges owners not seen for N days (default `inactiveOwnerDays`). |
| `/securemod admin unlockall confirm` | `securemod:command.admin` | OP 2 | Removes **every** protection (use before uninstalling). |
| `/securemod admin export` | `securemod:command.admin` | OP 2 | Writes every protection to `securemod-exports/securemod-export-<date>.json` (no passcode hashes). |
| `/securemod admin rollback <backup>` | `securemod:command.admin` | OP 2 | Restores protections from a backup in `securemod-backups/` (one is created automatically before every purge and `unlockall`). Secure Mod blocks that were turned into vanilla come back as padlocks. |

Player names are resolved from online players and players that already joined the server.

## Other nodes
| Node | Fallback | Meaning |
|---|---|---|
| `securemod:admin.bypass` | OP 2 | Open, break and configure any protected block. Every use is written to the audit log. |
| `securemod:admin.inspect` | OP 2 | Use the Admin Tool and `/securemod inspect`. |
| `securemod:admin.remove` | OP 2 | Force‑unlock with the Admin Tool and use the Universal Block Remover on other players' blocks. |
| `securemod:limit` (integer) | config value | Max protected blocks for that player or rank (e.g. LuckPerms meta). Overrides `maxProtectedBlocksPerPlayer`. |

### LuckPerms example
```
/lp group default permission set securemod:command.trust true
/lp group mod permission set securemod:admin.inspect true
/lp group admin permission set securemod:admin.bypass true
```
