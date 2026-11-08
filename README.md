# TRADIX

دفترچه معاملات، آموزش و ساعت سشن‌های فارکس. **سیگنال نمی‌دهد و معامله انجام نمی‌دهد.**
Forex trade journal, learning hub and live session clock. It never trades and never gives signals.

## Stack

| Layer | Tech | Where |
|---|---|---|
| App + UI | Kotlin, Jetpack Compose (RTL, Persian first) | `app/` |
| Fast core | Rust (JNI): session maths, trade statistics | `rust/` |
| Animation kit | Dart / Flutter: globe, glow numbers | `flutter_fx/` |
| CI | GitHub Actions: `cargo test` + `assembleDebug` | `.github/workflows/ci.yml` |

## Screens

Home dashboard, sessions (spinning globe or 24h timeline, overlap map), add trade (lot size + profit/loss amount),
performance (equity candles daily/weekly/monthly/yearly + 5-week heatmap), strategies, education
(text / image / video and the 4 mixed combinations), calendar (trades + sample news), More menu with
`# هشتگ‌ها`, detailed settings.

## Build

1. Open in Android Studio (Koala+), let it sync. No Gradle wrapper is committed: use Studio's Gradle or run
   `gradle wrapper --gradle-version 8.9` once.
2. Optional, for the Rust core: `cargo install cargo-ndk`, set `ANDROID_NDK_HOME`, then `./rust/build-android.sh`.
   Without the `.so` the app uses the identical Kotlin fallback (`NativeCore.isNative` tells which one is active).
3. Dart animation kit: `cd flutter_fx && flutter run`. It is a standalone module for now and is not yet embedded in the
   Android app (the Compose globe in `Globe.kt` implements the same effect natively).

## Status / honest notes

- Core journal data now persists locally in an atomic SQLite snapshot; trade, strategy, lesson, tag and settings state are restored on app launch.
- Synthetic trade fixtures were removed. The quick-add symbol list and domain validator are restricted to supported fiat FX pairs (no BTC, gold or index symbols).
- Detailed trade model fields now include optional entry/exit, stop-loss/take-profit, costs, notes, account reference and planned risk. The current quick-add screen still records a manually entered realized P&L; full price/fill-based entry UI remains to be wired.
- A first Forex calculation domain is present with pair-aware pip size, explicit quote-to-account conversion, net costs and R-multiple calculation; JUnit tests cover representative cases.
- Calendar-aware daily/weekly/monthly/yearly bucket boundaries replace rough 30/365-day approximations.
- Session timezone/DST integration into the existing globe/timeline, normalized multi-account screens, complete mixed-media lesson editor, news provider, scheduled reminders, import/export, and complete Persian/English localization remain to be implemented and validated.
- The current UI/theme components remain unchanged. Run the Android and Rust CI jobs for the latest PR commit before considering this branch ready.
