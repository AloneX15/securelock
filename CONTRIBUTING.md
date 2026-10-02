# Contributing to Secure Mod

Thanks for helping! A few rules keep Secure Mod safe and compatible.

## Setup
- JDK 25 and IntelliJ IDEA ≥ 2025.3 (Gradle sync imports every Minecraft version through Stonecutter).
- `./gradlew :26.3:runClient` / `:26.3:runServer` to play, `:26.3:runGameTest` to run the bypass suite.
- The active version in the IDE is set in `stonecutter.gradle.kts` (`stonecutter active "26.3"`).

## Golden rules
1. **Authorization only on the server.** The client sends intents; the server validates distance, dimension, block and permission.
2. **Never crash and never break another mod.** Every event handler and mixin hook goes through `try/catch` and fails *safe* (a protected block stays closed). See section 20 of the plan and [MIXINS.md](MIXINS.md).
3. **Prefer Fabric API events over mixins.** If you need a mixin: MixinExtras (`@WrapOperation`, `@ModifyExpressionValue`, `@WrapWithCondition`, `@ModifyReturnValue`), `require = 0`, handlers prefixed `securemod$`, and only one call into `SecureModHooks`. `@Overwrite` and `@Redirect` are not accepted.
4. **Never load chunks to check a protection.**
5. **Keep `security/` free of Minecraft classes** so it is unit-testable and identical across versions.
6. Every bypass that gets fixed becomes a gametest in `src/gametest`.

## Multi-version code
Use Stonecutter conditional comments where the Minecraft API changes:
```java
//? if >=26.2 {
minecraft.gui.setScreen(screen);
//?} else {
/*minecraft.setScreen(screen);
*///?}
```
Before opening a PR run, for **every** version:
```
./gradlew :26.1.2:test :26.1.2:runGameTest :26.2:test :26.2:runGameTest :26.3:test :26.3:runGameTest
```

## Translations
Language files are in `src/main/resources/assets/securemod/lang/`. `en_us.json` is the reference; copy it and translate the values.

## Pull requests
- One feature or fix per PR, with a line in `CHANGELOG.md` under "Unreleased".
- Describe how you tested it (which versions, singleplayer or dedicated server).
- Security fixes: please coordinate through a private advisory first (see [SECURITY.md](SECURITY.md)).
