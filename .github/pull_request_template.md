## What does this change?

## How was it tested?
- [ ] `./gradlew :26.1.2:runGameTest :26.2:runGameTest :26.3:runGameTest`
- [ ] Unit tests (`test`) on every version
- [ ] Manually in singleplayer / dedicated server (which versions?)

## Checklist
- [ ] Authorization stays server-side
- [ ] No `@Overwrite` / `@Redirect`; new mixins use `require = 0`, `securemod$` handlers and are listed in `MIXINS.md`
- [ ] New protections have a gametest
- [ ] `CHANGELOG.md` updated (mark **⚠ BREAKING** changes to configs or worlds)
