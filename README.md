# Wayo — Android Studio project

A complete Android Studio project. Kotlin + Jetpack Compose (Material 3) + **Navigation 3** + Room (local-only) + Google Play Billing (one-time purchase). Targets **Android 17 (API 37)** with edge-to-edge enabled.

## How to open it

1. Unzip this folder anywhere on your machine.
2. Open Android Studio → **Open** → select the unzipped `Wayo` folder (the one containing `settings.gradle.kts`).
3. Let Gradle sync. If the Gradle wrapper jar is missing (it isn't included as a binary in this zip), Android Studio will offer to regenerate it, or run `gradle wrapper --gradle-version 9.2` once from a terminal with Gradle installed.
4. Run on a physical device if possible — compass and GPS behave much more realistically on real hardware than the emulator.

## What changed in this update, and why

**Material 3** — was already correct in the original build (Scaffold, TopAppBar, MaterialTheme, dynamic color). No changes needed here.

**Navigation 3** — the original version used a hand-rolled `sealed class Screen` with manual `remember { mutableStateOf(...) }` state. That's not Navigation 3. This version uses the real library:
- Routes are `@Serializable data object` types implementing `NavKey` (see `ui/navigation/NavRoutes.kt`)
- `rememberNavBackStack(Home)` creates a real, saveable back stack
- `NavDisplay` + `entryProvider { entry<Home> {...} entry<History> {...} }` replaces the manual `when` block in `MainActivity`
- Navigating is just list operations now (`backStack.add(History)`, `backStack.removeLastOrNull()`)

**Edge-to-edge / Android 17** — the original manifest and Activity never called `enableEdgeToEdge()` and targeted an older SDK. Now:
- `compileSdk` / `targetSdk` bumped to 37 (Android 17, released June 2026)
- `enableEdgeToEdge()` called in `MainActivity.onCreate()` before `setContent`
- No `windowOptOutEdgeToEdgeEnforcement` — that opt-out is disabled once you target API 36+, so there's no legitimate way around it, and this app doesn't need one anyway since Material 3's `Scaffold`/`TopAppBar` already handle system bar insets correctly by default

## Things I want to flag honestly rather than overstate

I don't have a real Android SDK or Gradle in the environment I built this in, so **none of this has been compiled**. Everything below is accurate as of my most recent search, but Gradle sync in Android Studio is the real first test:

- **AGP 9.1.1 and Kotlin 2.2.10** are current, confirmed versions as of mid-2026. AGP's 9.x line has been making structural changes to how it integrates with Kotlin — if Android Studio's Upgrade Assistant suggests further changes on first sync (e.g. around the Kotlin plugin setup), that's expected; accept its suggestions rather than fighting them.
- **The KSP version string** (`2.2.10-1.0.29`) follows the standard `<kotlin-version>-<ksp-version>` pattern, but I can't guarantee that exact suffix is published — if Gradle can't resolve it, check the KSP releases page on GitHub (google/ksp) for the closest match to Kotlin 2.2.10.
- **The Gradle wrapper jar itself isn't in this zip** (it's a binary file). Android Studio will regenerate it automatically, or you can run it manually — see step 3 above.
- **Navigation 3's exact API surface has shifted across recent releases** (some early versions required an explicit generic on `rememberNavBackStack`, newer ones don't). If you get a compile error on that line specifically, it's the most likely spot to need a small adjustment based on whatever navigation3 version Gradle actually resolves.

Given all that: **treat first Gradle sync as the real checkpoint**, not this message. If something doesn't resolve, it's almost certainly one of the four items above, not the app architecture itself.

## UI design pass — what changed and why

The first version was functionally complete but visually generic (default Material blue, a stock icon just rotated for the compass, flat cards). This pass addresses that directly:

- **Deliberate palette, dynamic color off by default** — teal/coral against a near-black navy in dark mode (the app is plausibly opened in a dim parking garage, so dark-first makes sense), a lighter mist palette in light mode. `dynamicColor` defaults to `false` in `WayoTheme` on purpose: Material You would override this palette with wallpaper-derived colors and flatten the intentional design. Pass `true` if you'd rather match the system theme.
- **Custom-drawn compass dial** (`ui/components/CompassDial.kt`) — a real `Canvas` drawing: tick marks every 30°, emphasized cardinal points, a gradient-glow needle with a spring-based (slightly bouncy) rotation animation instead of a linear tween. This replaces the plain rotated stock icon entirely.
- **"Park here" button** — gradient-filled circle, drop shadow tinted with the primary color, a soft infinite pulsing halo to invite the tap, and haptic feedback on press.
- **Type scale** — tight negative letter-spacing on large display text (the distance readout), clear weight contrast between headline/title/body. See the note in `Type.kt` on why this doesn't use a downloadable Google Font.
- **Elevated, tonal cards throughout** — the compass readout sits in a `surfaceVariant` card, history rows have real elevation and a tonal icon badge when there's no photo, empty/paywall states get a colored icon badge instead of a bare icon.
- **Smooth state transitions** — `AnimatedContent` cross-fades between the permission/capture/compass states on the home screen instead of an abrupt swap.



1. In Play Console, create a **managed product** (one-time) with the exact ID `unlock_full_access`, or change `PRODUCT_ID` in `BillingManager.kt` to match.
2. Upload a signed build to at least Internal Testing — Play Billing won't return real product details otherwise.
3. Add your own account as a license tester in Play Console.

## Other things worth knowing before submission

- **Android 17 also enforces adaptive UI on large screens** (tablets/foldables can no longer be pillarboxed). This app's simple, centered UI already adapts fine — nothing to fix, just worth knowing the constraint exists if you add screens later.
- **Launcher icon is a placeholder** — swap the vector drawables or use Android Studio's Image Asset Studio before submitting.
- **Privacy policy is still required** at submission due to location + camera permissions.

## Project structure

```
app/src/main/java/com/mslabs/wayo/
├── MainActivity.kt        Navigation 3 host + enableEdgeToEdge()
├── data/                  Room entity, DAO, database, repository
├── location/              FusedLocationProviderClient wrapper
├── sensor/                Rotation-vector compass sensor with smoothing
├── util/                  Bearing/distance math, photo file helpers
├── billing/               Google Play Billing wrapper
└── ui/
    ├── MainViewModel.kt
    ├── navigation/        NavKey route definitions (Navigation 3)
    ├── screens/           HomeScreen (capture + compass), HistoryScreen (paywalled)
    ├── components/        Shared photo thumbnail composable
    └── theme/             Material 3 theme, color, typography
```
