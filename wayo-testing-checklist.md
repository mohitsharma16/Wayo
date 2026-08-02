# Wayo — testing checklist before we proceed

Test on a **real device**, not just the emulator, wherever compass/GPS/camera are involved — sensor behavior on the emulator is simulated and won't tell you much about real-world accuracy.

---

## 1. Core functional flow

The thing that has to work end to end, in order:

| # | Test | How to do it |
|---|---|---|
| 1.1 | Fresh install, first launch | Uninstall completely first (`adb uninstall com.mslabs.wayo`), reinstall, confirm no crash on first open |
| 1.2 | Location permission prompt appears correctly | On first launch with no prior grant, the "Wayo needs your location..." screen should show, not a bare OS dialog with no context |
| 1.3 | Grant permission → capture screen appears | Tap "Allow location access," confirm the OS permission dialog appears, grant it, confirm you land on "Park here" |
| 1.4 | Tap "Park here" | Confirm the app doesn't hang, and it transitions to the compass screen within ~1-2 seconds |
| 1.5 | Compass screen shows correctly | Distance should show something reasonable (not 0m unless you're literally standing on the spot, not a huge random number) |
| 1.6 | Walk away and back | Physically walk 20-30 meters away, confirm distance increases; walk back, confirm it decreases and the arrow updates direction |
| 1.7 | "Found it" clears the spot | Tap it, confirm you're back on the capture screen, not stuck |
| 1.8 | Add a photo | From capture screen, tap "Add a photo," take a picture, confirm the thumbnail shows before you tap "Park here," and again on the compass screen after |
| 1.9 | History screen (paywalled) | Tap the history icon top-right — should show the paywall since nothing's purchased yet |

**How to check for crashes specifically:** keep Android Studio's **Logcat** open while testing (View → Tool Windows → Logcat), filter by your package name (`com.mslabs.wayo`), and watch for red `FATAL EXCEPTION` lines. A crash that doesn't visibly show anything on screen will still show up here.

---

## 2. Permission edge cases

These matter more than they seem — permission handling is one of the most common rejection reasons in Play Console review.

| Test | How to do it |
|---|---|
| Deny permission | On first launch, tap "Deny" instead of "Allow" — confirm the app doesn't crash and stays on the rationale screen (doesn't get stuck in a blank state) |
| Deny, then grant later | Deny once, then go to Android Settings → Apps → Wayo → Permissions → enable Location manually, reopen the app, confirm it now works |
| Revoke permission mid-use | With an active spot saved, go revoke location permission from system settings while the app is backgrounded, then return to the app — confirm it doesn't crash, ideally falls back to the rationale screen |
| Camera permission denial | Deny camera access when tapping "Add a photo" — confirm the app doesn't crash, just skips the photo (Android's `TakePicture` contract should handle this gracefully, but verify) |
| "While using the app" vs "Always" | Android will likely offer "While using the app" as an option — confirm that's sufficient for the app to work (it should be; the app doesn't need background location) |

**How to force-test permission states via adb** (faster than digging through Settings each time):
```
adb shell pm revoke com.mslabs.wayo android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.mslabs.wayo android.permission.ACCESS_FINE_LOCATION
```

---

## 3. Sensor accuracy (the part that's hardest to get right)

| Test | How to do it |
|---|---|
| Compass accuracy outdoors, open sky | Save a spot, walk in a large circle around it holding the phone flat, confirm the arrow consistently points back toward the spot rather than drifting |
| Compass jitter | Hold the phone still — the arrow should be steady, not visibly twitching. If it's twitchy, the low-pass filter isn't smoothing enough |
| Compass near metal/magnets | Test near a car (irony intended), metal railing, or large appliance — compass sensors are notoriously thrown off by nearby magnetic interference. Note whether accuracy visibly degrades |
| Screen rotation while navigating | Rotate the phone from portrait to landscape while on the compass screen — the arrow's bearing calculation should still point correctly (this is what the `axesForDisplayRotation` code handles) |
| GPS accuracy indoors/underground | **This is the real stress test.** Save a spot at the deepest level of an actual parking garage if you can, walk to the exit and back, see how the app behaves when GPS signal is weak or lost entirely |
| GPS signal loss mid-navigation | While navigating back, walk into an elevator or stairwell (signal-blocking areas) and back out — confirm the app doesn't freeze or show a stale/frozen arrow without explanation |

**How to simulate GPS conditions without traveling:** Android Studio's emulator has a **Extended Controls → Location** panel where you can manually set/move coordinates for basic logic testing — but this won't test real sensor noise or garage signal loss. For that, you need the real-world test above.

---

## 4. Visual / UI testing

| Test | How to do it |
|---|---|
| Dark mode | Toggle system dark mode (Settings → Display → Dark theme, or Quick Settings tile) with the app open — confirm colors switch correctly and nothing is unreadable (e.g. dark text on dark background) |
| Light mode | Same, toggled back to light — confirm the mist/teal palette looks right |
| Edge-to-edge display | Look specifically at the top status bar and bottom navigation bar area — content should extend behind them tastefully (not have a jarring white/black bar), and no buttons or text should be *hidden* underneath the system bars |
| Font scaling | Settings → Display → Font size → increase to largest — reopen the app, confirm text doesn't overflow its container or get cut off |
| Different screen sizes | Test on at least one small phone and one large phone/tablet if you have access; if not, use Android Studio's Layout Inspector or emulator device profiles (Pixel 4a small vs. Pixel Tablet large) |
| Predictive back gesture | On Android 14+, swipe back from the edge slowly (don't release) — you should see a preview animation. This is a Navigation 3 / system integration point worth confirming works, not just a hard instant back |
| Pulsing "Park here" animation | Confirm the halo animation around the button is smooth, not stuttery |
| Compass dial rendering | Confirm the tick marks, needle, and glow render cleanly — check this in both light and dark mode since colors are theme-dependent |

**How to check edge-to-edge specifically:** Android Studio's **Layout Inspector** (Tools → Layout Inspector while the app is running) lets you see the actual rendered view bounds and confirm nothing's obscured by system bars.

---

## 5. Data persistence

| Test | How to do it |
|---|---|
| Survive app backgrounding | Save a spot, press Home (don't force-close), reopen the app — spot should still be active |
| Survive force-stop | Save a spot, go to Settings → Apps → Wayo → Force stop, reopen — spot should still be there (Room persists to disk, this should work) |
| Survive device restart | Save a spot, restart the phone, reopen the app — same check |
| Photo persists correctly | Save a spot with a photo attached, force-stop and reopen, confirm the photo still displays (not a broken image icon) |

---

## 6. Billing flow (requires Play Console setup first)

You can't fully test this until you've uploaded a build to at least Internal Testing and created the `unlock_full_access` product — but once you have:

| Test | How to do it |
|---|---|
| Add license tester | Play Console → Setup → License testing → add your Google account email |
| Purchase flow | Tap "Unlock full access" on the paywall, confirm the real Google Play purchase sheet appears with the correct price |
| Purchase completes | Complete a test purchase (license testers aren't charged), confirm the paywall disappears and history becomes accessible |
| Restore on reinstall | Uninstall and reinstall the app while signed into the same Google account, confirm the purchase is automatically detected on next launch (via `queryExistingPurchases`) without needing to buy again |
| "Found it" now keeps history | With the purchase unlocked, save a spot, tap "Found it," confirm it now appears in History instead of disappearing (this only happens post-purchase, per the free/paid logic) |

---

## 7. Accessibility

| Test | How to do it |
|---|---|
| TalkBack screen reader | Settings → Accessibility → TalkBack → enable. Navigate the whole app using only swipes and double-taps (no direct touch). Confirm the compass, buttons, and photo all have sensible spoken descriptions, not silence or "unlabeled button" |
| Touch target size | Confirm all buttons are comfortably tappable, especially the small icon buttons (History, back arrow) |

---

## 8. Performance / stability

| Test | How to do it |
|---|---|
| Battery drain check | Leave the compass screen open and active for 10-15 minutes, check Settings → Battery → Battery usage to see if Wayo shows unusually high drain (continuous GPS + sensor polling will use some battery — the question is whether it's *reasonable*, not zero) |
| Memory leak check | Navigate back and forth between Home and History repeatedly (20+ times), then check Android Studio's **Profiler** (View → Tool Windows → Profiler) for memory that keeps climbing and never comes back down — that would indicate the sensor/location listeners aren't being cleaned up properly |

---

## 9. Device/OS coverage

Since `minSdk` is 26 and `targetSdk` is 37, ideally test on at least:
- One older device or emulator image around Android 8-10 (API 26-29) — oldest supported
- One mid-range device around Android 12-13 (API 31-33)
- One current device on Android 15-17 (API 35-37) — this is where edge-to-edge enforcement and predictive back actually matter most

If you don't have physical devices spanning that range, Android Studio's **Device Manager** lets you create virtual devices at each API level — sensor/GPS fidelity will be lower than real hardware, but it'll catch obvious layout or crash issues per-version.

---

## What to send me once you've tested

When you share how it looks/works, the most useful things to include are:
1. **Screenshots or screen recording** of the actual flow (capture → compass → found it)
2. **Which specific device and Android version** you tested on
3. **Anything that felt off** — even vague ("the arrow felt laggy," "text looked cramped") is useful, I can dig into the likely cause
4. **Any crash/error you hit** — if Logcat showed a red exception, paste the relevant lines

That's enough for me to diagnose and fix specific issues rather than guessing.
