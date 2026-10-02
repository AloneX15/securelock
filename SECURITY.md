# Security policy

Secure Mod is a security mod: a way to open, break, empty or move a protected block without permission is a **security bug**, not a normal bug.

## Reporting a vulnerability
**Do not open a public issue.** Report it privately through GitHub:

1. Go to <https://github.com/AloneX15/securemod/security/advisories/new>.
2. Describe the bypass: Minecraft version, Secure Mod version, other mods installed, and the steps to reproduce.
3. If you can, include a world or a datapack that reproduces it.

You will get an answer within 72 hours. Critical bypasses are fixed and released as soon as possible, and you will be credited in the changelog unless you prefer otherwise.

## Supported versions
| Version | Supported |
|---|---|
| Latest release, every supported Minecraft version (26.1.x, 26.2.x, 26.3.x) | ✅ |
| Older releases | ❌ (update first) |

## Scope
In scope: anything that lets a player without permission use, break, empty, move, copy or read the code of a protected block, crash or lag the server with Secure Mod packets, or escalate permissions.

Out of scope: operators or players with the `securemod:admin.bypass` permission, physical access to the server files, and issues in other mods that ignore the [public API](docs/api.md).
