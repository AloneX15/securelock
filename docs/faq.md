# FAQ

**Do players need the mod on their client?**
Yes, in v1 (it adds blocks, items and screens). A server-only mode with Polymer is planned for v1.x.

**I forgot the code of my chest.**
As the owner you never need the code: just open it. To change it, use the Universal Block Modifier.

**How do I give a friend access to everything?**
`/securemod trust <name>`. For a single block use the Universal Block Modifier (Allow). For your team, set the block to *Shared* and put both of you in the same scoreboard team.

**Can I lock blocks from other mods?**
Yes, if they are in the `securemod:lockable` tag (`#c:chests` and `#c:barrels` are included) or listed in `extraLockableBlocks`.

**My hopper doesn't fill my own padlocked chest.**
Padlock the hopper too. With `allowHoppersFromSameOwner` (default `true`) a hopper padlocked by the same owner is allowed.

**Can admins open everything?**
Players with `securemod:admin.bypass` (OP 2 by default) can, and every bypass is written to the audit log.

**The block says "Locked by ?".**
The data is corrupt or comes from a newer Secure Mod version: only admins can access it. Check the server log.

**How do I uninstall?**
See [Admin guide → Uninstalling](admin-guide.md#uninstalling-without-losing-anything).
