# TRADIX

دفترچه معاملات، آموزش و ساعت سشن‌های فارکس. **سیگنال نمی‌دهد و معامله انجام نمی‌دهد.**
Forex trade journal, learning hub and live session clock. It never trades and never gives signals.

## Stack

| Layer | Tech | Where |
|---|---|---|
| App + UI | Kotlin, Jetpack Compose (RTL, Persian first) | `app/` |
| Fast core | Rust (JNI): session maths, trade statistics | `rust/` |
| Animation kit | Dart / Flutter: globe, glow numbers | `flutter_fx/` |
| CI | GitHub Actions: `cargo test` + `testDebugUnitTest` + `assembleDebug` | `.github/workflows/ci.yml` |

## Screens

Home dashboard, trade history (filters, edit/delete, CSV export), manual trade entry (manual P&L or price-based FX calculation),
multi-account profiles (real/demo/prop, base currency, active account), performance (equity candles and heatmaps),
strategies (create/edit/delete and version snapshots), DST-aware sessions (globe/timeline), education,
calendar (personal trades + sample news), tags, and settings.

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
- Trade records now carry optional entry/exit, stop-loss/take-profit, commission, swap, fees, notes, account reference, date, and strategy-version metadata. Quick-add supports manual realized P&L or price-derived Forex P&L.
- The calculation engine uses pair-aware pip sizes, explicit quote-to-account conversion, costs, and optional R multiple. Unit tests cover major-pair, JPY, short-side, invalid-conversion, and FX-only validation cases.
- Multiple account profiles can be created, selected, and archived. Home, performance, strategy comparisons, calendar, tags, trade history, and CSV exports are scoped to the selected account.
- Trade history supports BUY/SELL/WIN/LOSS filters, edit/delete with confirmation, and account-scoped CSV export.
- Strategy edits create revision snapshots and increment the active version; new trades retain the strategy version used.
- Sydney, Tokyo, London, and New York session state and overlap use IANA market timezones with daylight-saving transitions, while the existing globe/timeline components are retained.
- Still outstanding: normalized Room schema and migrations (current store is a SQLite snapshot), full open/partial-fill trade lifecycle, full strategy revision history UI, complete block-based mixed-media academy, live economic-calendar provider, local no-trade reminders, backup/import/export, and complete Persian/English localization.
- Existing theme tokens, colors, reusable components, and navigation styling were preserved; screens were extended additively. Run the latest Android unit tests/build and Rust tests before considering this branch release-ready.
