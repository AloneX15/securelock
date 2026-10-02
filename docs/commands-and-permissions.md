# Commands and permissions

Permissions use the **Fabric Permission API** (built into Fabric API), so LuckPerms and other providers work out of the box. If no provider answers, the fallback in the table is used.

Node names are identifiers in the `securelock` namespace, e.g. `securelock:admin.bypass`.

## Commands
| Command | Node | Fallback | What it does |
|---|---|---|---|
| `/securelock info` | `securelock:command.info` | everyone | Info about the block you are looking at. Strangers only see owner and mode. |
| `/securelock trust <player>` | `securelock:command.trust` | everyone | The player can use **all** your protected blocks. |
| `/securelock untrust <player>` | `securelock:command.trust` | everyone | |
| `/securelock trusted` | `securelock:command.trust` | everyone | Lists your trusted players. |
| `/securelock list` | `securelock:command.list` | everyone | Your protected blocks with coordinates (and the limit). |
| `/securelock inspect` | `securelock:admin.inspect` | OP 2 | Full info + the last 10 audit entries of the block. |
| `/securelock debug` | `securelock:command.debug` | OP 2 | Mixin status, `compat.disable`, totals, raid window. |
| `/securelock reload` | `securelock:command.reload` | OP 2 | Reloads `config/securelock.json`. |
| `/securelock admin unlock` | `securelock:command.admin` | OP 2 | Removes the protection of the block you look at (SecureLock blocks become their vanilla version, contents kept). |
| `/securelock admin transfer <player>` | `securelock:command.admin` | OP 2 | Changes the owner of the block you look at. |
| `/securelock admin purge <player>` | `securelock:command.admin` | OP 2 | Removes all protections of a player (banned/inactive). |
| `/securelock admin purgeinactive [days]` | `securelock:command.admin` | OP 2 | Purges owners not seen for N days (default `inactiveOwnerDays`). |
| `/securelock admin unlockall confirm` | `securelock:command.admin` | OP 2 | Removes **every** protection (use before uninstalling). |
| `/securelock admin export` | `securelock:command.admin` | OP 2 | Writes every protection to `securelock-exports/securelock-export-<date>.json` (no passcode hashes). |
| `/securelock admin rollback <backup>` | `securelock:command.admin` | OP 2 | Restores protections from a backup in `securelock-backups/` (one is created automatically before every purge and `unlockall`). SecureLock blocks that were turned into vanilla come back as padlocks. |

Player names are resolved from online players and players that already joined the server.

## Other nodes
| Node | Fallback | Meaning |
|---|---|---|
| `securelock:admin.bypass` | OP 2 | Open, break and configure any protected block. Every use is written to the audit log. |
| `securelock:admin.inspect` | OP 2 | Use the Admin Tool and `/securelock inspect`. |
| `securelock:admin.remove` | OP 2 | Force‑unlock with the Admin Tool and use the Universal Block Remover on other players' blocks. |
| `securelock:limit` (integer) | config value | Max protected blocks for that player or rank (e.g. LuckPerms meta). Overrides `maxProtectedBlocksPerPlayer`. |

### LuckPerms example
```
/lp group default permission set securelock:command.trust true
/lp group mod permission set securelock:admin.inspect true
/lp group admin permission set securelock:admin.bypass true
```
