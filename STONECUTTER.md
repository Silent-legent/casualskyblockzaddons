# Stonecutter cheat sheet

How this mod supports several Minecraft versions from one codebase.
Keep this in the repo root (not inside `src/`).

> Unsure about a comment syntax? The official docs are at https://stonecutter.kikugie.dev/wiki/

---

## The idea in 30 seconds

- All code lives in **one** `src/` folder.
- Each Minecraft version is a "project" with its own small settings file.
- Where versions differ, the code has special `//?` comments. Stonecutter turns the right half **on** and the other half **off** when you switch versions.
- Switching rewrites the actual files in `src/`. That is normal.

---

## Current setup (update this when it changes)

| Thing | Value |
|---|---|
| Versions | 26.1.2, 26.2, 26.3 |
| `vcsVersion` (what git stores) | 26.1.2 (the lowest) |
| Per-version settings | `versions/<version>/gradle.properties` |
| Shared settings | `gradle.properties` (root) |
| Version list | `settings.gradle.kts` |

---

## Daily rules (read these first)

1. **Switch versions with the Gradle task only.** Gradle window > `stonecutter` > *Set active project to X*. Never hand-edit the `/* */` markers to "switch".
2. **Before every commit, switch back to the `vcsVersion` (the lowest) and build.**
3. **A clean build does not mean it works.** Run the game once per version and test the features that changed.
4. **Mixin targets** (like `"renderLevel"`) are only checked when the game loads, not when you build. Check the log for Mixin errors.
5. Build a specific version: `./gradlew :26.2:build`. Jars end up in `versions/<version>/build/libs/`.

---

## Add a new version (example: 26.4)

- [ ] **1. Make** `versions/26.4/gradle.properties`:
  ```
  minecraft_version=
  minecraft_dependency=~26.4
  loader_version=
  fabric_api_version=
  skyblock_api_version=
  skyblock_api_capability=26.4
  ```
  - `minecraft_version`, `loader_version`, `fabric_api_version`: from the Modrinth Fabric API versions page.
  - `skyblock_api_version` and `skyblock_api_capability`: from the Skyblock API README. It has a table of which capability name belongs to which Minecraft version.
  - `minecraft_dependency` is the range written into `fabric.mod.json`. `~26.4` means "any 26.4.x".
- [ ] **2. Add** `"26.4"` to `versions(...)` in `settings.gradle.kts`.
- [ ] **3. Reload Gradle**, then *Set active project to 26.4*.
- [ ] **4. Build** (`./gradlew :26.4:build`) and read the errors.
- [ ] **5. Fix each error** with a comment block (see "Comment syntax" below).
- [ ] **6. Run the game** on 26.4 and check the log for Mixin errors.
- [ ] **7. Switch back to the `vcsVersion`**, build it (and the others), then commit.

If an old block already says `>=26.2` and 26.4 changes that same code again, split it so 26.4 gets its own branch:
```kotlin
//? if >=26.4 {
/*codeFor264()
*///?} else if >=26.2 {
codeFor262()
//?} else {
/*codeForOlder()
*///?}
```
(Which half is wrapped in `/* */` depends on the active version. Stonecutter handles that when you switch.)

---

## Remove the oldest version (example: 26.1.2)

The oldest version is always the `vcsVersion`, so this has a few more steps.

- [ ] **1.** Set the active project to the **new** lowest version (26.2) first.
- [ ] **2.** In `settings.gradle.kts`: remove `"26.1.2"` from `versions(...)` and change `vcsVersion` to `"26.2"`.
- [ ] **3.** Put `26.2` in the `.sc_active_version` file.
- [ ] **4.** Delete the folder `versions/26.1.2/`.
- [ ] **5.** Reload Gradle and build every remaining version.
- [ ] **6. (Optional cleanup)** Any block that says `//? if >=26.2 {` is now always true. Keep the new code, delete the old half and the `//?` markers. If you skip this, nothing breaks, it's just dead code.
- [ ] **7.** Update the "Current setup" table above.

---

## Comment syntax

### New code vs old code (the main one)
```kotlin
//? if >=26.2 {
/*newCode()
*///?} else {
oldCode()
//?}
```
The half that does **not** belong to the active version is inside `/* */`.
In the example above, the active version is below 26.2, so the old code is live.

### Only for some versions, no else
```kotlin
//? if >=26.2 {
/*extraThing()
*///?}
```

### Imports
Same thing, just put the import lines inside the block. Blocks with only imports are fine:
```kotlin
//? if <26.2 {
import net.minecraft.client.renderer.ShapeRenderer
//?}
```

### Class fields or constructor parameters
Same pattern inside the class. See `RenderEvent` for a working example.

### A range, or one exact version
```kotlin
//? if >=26.1 <26.3 {
//?}
//? 26.2 {
//?}
```

### Java files (Mixins)
Identical syntax. See `LevelRendererMixin` for a working example.

---

## Which tool for which problem

| Situation | Use |
|---|---|
| A few lines differ | a `//? if` block right there |
| Same difference used in many places | one small helper object with the `//?` inside it (like `McCompat`), then call it everywhere |
| A class got renamed, behavior is the same | `replacements.string` (see the wiki), it does a find-and-replace per version |
| Different Mixin target per version | wrap the whole `@Inject` method, plus the imports it needs |
| Different Fabric API / loader / Skyblock API numbers | `versions/<version>/gradle.properties`, not in code |

Tip: only wrap the part that really differs. Keep shared code outside the block.

---

## Quick error guide

| Error | Usually means |
|---|---|
| `Could not get unknown property 'x' for project ':26.2'` | A line (or the whole file) is missing in `versions/26.2/gradle.properties`. Check the folder name matches the version exactly. |
| `Stonecutter requires at least Gradle X` | Edit `gradle/wrapper/gradle-wrapper.properties` by hand (`distributionUrl`). Gradle can't fix itself before it loads settings. |
| Code looks greyed out or commented | Normal. That's the other versions' half. |
| Error only on one version | That version's half of the block is wrong. Switch to it and read the error. |
| Compiles but features are broken | Run the game, check the log for Mixin errors, test the render features. |
| Can't find a class on a new version | In IntelliJ type `Minecraft.getInstance().` and read the autocomplete list. The rename is usually visible there. |

---

## Files that matter

| File | What it does |
|---|---|
| `settings.gradle.kts` | Lists the versions and the `vcsVersion` |
| `stonecutter.gradle.kts` | The controller. Rarely needs changes. |
| `.sc_active_version` | Remembers the active version. Don't edit by hand. |
| `build.gradle.kts` | One build script shared by all versions |
| `gradle.properties` | Shared values (Java version, mod version, Cloth Config) |
| `versions/<ver>/gradle.properties` | Per-version values (Minecraft, Fabric API, loader, Skyblock API) |
| `src/main/resources/fabric.mod.json` | Uses `${minecraft_dependency}` from the per-version file |
