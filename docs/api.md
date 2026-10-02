# Public API

Other mods can respect Secure Mod protections without depending on it. Check `FabricLoader.getInstance().isModLoaded("securemod")` first and keep the calls in a separate class.

```java
import com.takumistudios.securemod.api.SecureModApi;
import com.takumistudios.securemod.api.SecureModEvents;

// Queries (server side; never load chunks)
boolean locked   = SecureModApi.isProtected(level, pos);
boolean canUse   = SecureModApi.canAccess(player, pos);   // true if not protected
boolean canBreak = SecureModApi.canBreak(player, pos);    // true if not protected
Optional<UUID> owner = SecureModApi.getOwner(level, pos);

// Events
SecureModEvents.ACCESS_DENIED.register((player, level, pos, owner) -> { /* ... */ });
SecureModEvents.BLOCK_LOCKED.register((player, level, pos) -> { /* ... */ });
SecureModEvents.BLOCK_UNLOCKED.register((player, level, pos) -> { /* player may be null */ });
```

Example for a quarry/storage mod: before extracting from a block, call `SecureModApi.isProtected(level, pos)` and skip it if it returns `true`, or use `canAccess` with the player who owns your machine.

Classes in `com.takumistudios.securemod.api` follow semantic versioning: they only change in a major version.
