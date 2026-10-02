# Public API

Other mods can respect SecureLock protections without depending on it. Check `FabricLoader.getInstance().isModLoaded("securelock")` first and keep the calls in a separate class.

```java
import com.alonex15.securelock.api.SecureLockApi;
import com.alonex15.securelock.api.SecureLockEvents;

// Queries (server side; never load chunks)
boolean locked   = SecureLockApi.isProtected(level, pos);
boolean canUse   = SecureLockApi.canAccess(player, pos);   // true if not protected
boolean canBreak = SecureLockApi.canBreak(player, pos);    // true if not protected
Optional<UUID> owner = SecureLockApi.getOwner(level, pos);

// Events
SecureLockEvents.ACCESS_DENIED.register((player, level, pos, owner) -> { /* ... */ });
SecureLockEvents.BLOCK_LOCKED.register((player, level, pos) -> { /* ... */ });
SecureLockEvents.BLOCK_UNLOCKED.register((player, level, pos) -> { /* player may be null */ });
```

Example for a quarry/storage mod: before extracting from a block, call `SecureLockApi.isProtected(level, pos)` and skip it if it returns `true`, or use `canAccess` with the player who owns your machine.

Classes in `com.alonex15.securelock.api` follow semantic versioning: they only change in a major version.
