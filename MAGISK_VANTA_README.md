# Magisk Vanta — Liquid Glass fork

Base: official `topjohnwu/Magisk` source.

## What's done

- `app/apk/.../ui/glass/backdrop/` — full Liquid Glass rendering engine
  ported (44 files, ~5450 lines). Originally `Kyant0/backdrop` +
  `Kyant0/AndroidLiquidGlass` (Apache-2.0), vendored via the Convx project.
  License headers kept intact in every file — required by Apache-2.0.
- `ui/glass/GlassEffect.kt` — the public API (`Modifier.liquidGlass(...)`,
  `LocalGlassEffectConfig`, `LocalAppBackdrop`).
- `ui/glass/GlassCard.kt` — drop-in replacement for `material3.Card`.
- `ui/glass/GlassSwitch.kt` / `GlassSwitchCompat` — drop-in replacement for
  `material3.Switch`, same signature (colors/thumbContent accepted and
  ignored — see the file's doc comment for why).
- `ui/glass/LiquidTabBar.kt` — floating glass pill bottom nav, replaces the
  stock `ShortNavigationBar` in `MainScreen.kt`.
- `MainScreen.kt` — provides `LocalAppBackdrop` app-wide via
  `CompositionLocalProvider`, wraps the pager content with
  `Modifier.layerBackdrop(...)` so every glass surface anywhere in the app
  samples live content.
- **Every screen's `Card`/`Switch` swapped for the glass versions** — Home,
  Settings (all sub-cards + every settings toggle via the shared
  `SettingsSwitch` in `SettingsComponents.kt`), Superuser (list + detail),
  Modules, Log, Deny list, Install dialog, Su request prompt. Verified: zero
  remaining `androidx.compose.material3.Card`/`Switch` imports anywhere
  under `ui/`.
- App label changed to "Magisk Vanta" in
  `app/shared/src/main/AndroidManifest.xml`.

## What's NOT done

- App icon — still the stock Magisk icon.
- The glass engine's `GlassEffectConfig` currently uses hardcoded defaults
  (vibrancy, blur radius, lens amount, etc. — see the data class in
  `GlassEffect.kt`). Convx exposed these as user-adjustable settings backed
  by DataStore; that settings UI wasn't ported, so right now the look is
  fixed at sane iOS-like defaults. Easy to add as a new section in
  `SettingsScreen.kt` later if you want it tunable.
- Not compiled/tested — I don't have Android SDK/NDK network access in my
  sandbox (see below). First build will surface anything that doesn't quite
  line up.

## Building

Magisk is **not** a plain `./gradlew assembleRelease` project — it has
native C/C++ components (magiskboot, magiskd, magiskpolicy) built via NDK
through a custom Python build script, alongside the Gradle/Kotlin app
module. You need:

- JDK 17
- Android SDK (compileSdk per `app/gradle/libs.versions.toml`) + NDK
- Python 3.6+

```bash
git clone --recurse-submodules <this-repo>
cd Magisk
export ANDROID_SDK_ROOT=/path/to/sdk
./build.py ndk      # downloads/installs the NDK the first time
./build.py all      # or `./build.py app` for just the Kotlin/Compose app
```

Output APKs land in `out/`.

### Option A — GitHub Actions (recommended)

Push this to your own GitHub repo and reuse Magisk's own CI workflow
(`.github/workflows/`) as a base — it already knows how to install the NDK
and run `build.py` in a clean Ubuntu runner with plenty of RAM/disk, free on
a public repo. No phone battery/storage spent, no local Android SDK setup.

### Option B — Termux (on-device)

Doable, but budget for it being heavy:
- `pkg install openjdk-17 python`
- Android SDK cmdline-tools + NDK still need fetching (several GB) via
  `sdkmanager` — needs your phone's normal internet.
- Compose compilation is RAM-hungry; if the Gradle daemon OOMs, lower
  `org.gradle.jvmargs` in `gradle.properties`.
- A `proot-distro` Ubuntu inside Termux can be more forgiving with SDK/NDK
  setup than bare Termux, at the cost of an extra virtualization layer.

### Option C — Android Studio (PC)

Simplest if you have access to one — bundles JDK+SDK+NDK setup and full
Google Maven access.

## Signing

Untouched from stock Magisk: set `keyStore`/`keyStorePass`/`keyAlias`/
`keyPass` in a `config.prop` (see `config.prop.sample`) to sign with your
own key, or leave unset for a debug-signed build.
