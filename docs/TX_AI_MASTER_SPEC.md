# TRADIX — Journal & TX AI Completion Blueprint

Status: implementation blueprint for the isolated feature branch. A capability is not considered complete merely because it appears in this document; each item must have a working data path, empty/error states, tests, and acceptance evidence.

## Product contract

- Local-first, offline-capable, Persian-first and RTL-aware.
- Account isolation is mandatory for every query, chart, export, and behavioral episode.
- Never fabricate fills, intratrade OHLC, prices, news, broker sync, or psychological certainty.
- No trade execution, copy trading, signals, or broker passwords.
- Missing data is represented as unavailable, not zero or inferred.
- Preserve current Tx theme tokens, components, navigation conventions, and unrelated screens.
- All work stays on `feature/tradix-forex-journal`; `main` is not a target for implementation commits.

## Reference product patterns reviewed

- TradeZella: broad journal workflow, reports, strategy templates, replay and backtesting.
- TraderSync: mobile journal, broker imports, replay and AI-assisted review.
- Edgewonk: psychology, mistake tracking and process-oriented review.
- TradesViz: broad analytics and customizable reporting.
- Tradervue: trade history, reports, tags and sharing.
- MyTradeBook, TradeLedger and Trade Ledger Pro: open-source design references for local journaling, calendar drill-down and trade analytics. Their code and visual identity are not to be copied.

## Feature inventory and acceptance gates

### A. Journal foundation
- Multiple isolated accounts; real/demo/prop metadata and base currency.
- Complete trade lifecycle: open, partial close, multiple fills, final close, fees, swap, commission and manual corrections.
- Immutable execution/fill records; edits retain an audit trail.
- CSV import wizard: mapping, preview, validation, duplicate detection, account selection, rollback and import summary.
- CSV/XLSX/PDF exports with filters, date range, timezone and account scope.
- Encrypted backup/restore, integrity check, conflict preview and user-controlled deletion.
- Search, saved filters, tags, attachments, screenshots and notes.
- No broker password storage. Any future broker read-only connector must be separately consented and revokeable.

Acceptance: restart-safe data, schema migration tests, import idempotency, export round-trip and cross-account isolation tests.

### B. Performance analytics
- Net/gross P&L, win rate, profit factor, expectancy, average win/loss, average R, payoff ratio, max drawdown, recovery factor and streaks.
- Equity curve and trade-based equity candles; market OHLC is only shown when sourced from real market data.
- Calendar P&L heatmap, hour-of-day, weekday, session, symbol, strategy/version, timeframe and market-condition analysis.
- Profit concentration, hold-time and exit efficiency; MAE/MFE only when actual excursion data exists.
- Confidence/sample labels and minimum sample gates on every comparative insight.
- Filters combine account, date, symbol, direction, session, strategy, tags, emotion and result.

Acceptance: every metric has a documented formula, stable sorting, sample-size disclosure and deterministic unit tests.

### C. TX AI Command Center
- Edge Score assembled from explicitly defined components; show component values and data coverage.
- Six-axis radar: Win Rate, Profit Factor, Expectancy, Consistency, Discipline and Session Edge.
- Monthly Process Score: Discipline, Plan Adherence, Risk Recording and Decision Quality.
- Tilt indicator is an evidence summary, never a clinical or probabilistic diagnosis.
- Best/worst session and setup only when sample gates pass.
- Daily/weekly/monthly narratives generated from local deterministic rules; every statement links to supporting trades and fields.

### D. Edge Explorer
- Multi-dimensional filters and saved views.
- Pair/session/strategy comparison matrix.
- Hour × weekday × outcome heatmap (15-minute granularity only with sufficient timestamps).
- Risk/reward scatter using recorded risk and realized result; unknown risk is excluded and counted as missing.
- Comparison mode between strategies and immutable strategy versions.

### E. Behavioral Lab
- Revenge Trading episodes with trigger trade, previous losing trade, time gap, risk/size changes, explicit plan violation, tags, emotion and loss streak.
- Overtrading after win/loss, early exit, widened stop, size-up after loss, session drift and overconfidence flags.
- Each card has Detection, Evidence, Linked Trades, Cost and Suggested Action sections.
- Evidence score is not a probability. User tags are distinct from algorithmic evidence.
- User review states: Confirmed by user / Not revenge / Unsure; review changes are auditable and do not silently rewrite trade data.
- Episode IDs prevent duplicate counting. Episode-level P&L and account-level cost are separately calculated to avoid double counting.
- Personal rules: after N losses, after risk increase, after daily loss cap, and user-defined conditions; soft warnings only.

Acceptance: planned quick re-entry is not automatically revenge; missing data is never inferred; episodes are account-scoped and reproducible; user corrections persist.

### F. Counterfactual Studio
- Exclude explicit off-plan trades, reduce risk, selected session/strategy only, or exclude trades after N consecutive losses.
- Actual vs scenario equity, delta and affected trade list.
- Every scenario lists assumptions, excluded trades and limitations.
- No scenario is described as a backtest unless it replays real historical market data and execution assumptions.

### G. Process Coach & planning
- Daily plan, risk cap, goal, checklist, mood, focus, notes and no-trade reason.
- Weekly autopsy and monthly score timeline.
- Process commitments and review completion.
- Planned rest days suppress reminders and are not discipline failures.
- Soft cooling-off prompts; never block broker actions.

### H. Trade replay and backtesting
- Journal replay from user-saved screenshots and event notes is supported without invented candles.
- Market replay/backtesting requires a licensed or otherwise permitted historical market-data source, timezone/session handling, spread, commission, slippage and execution model.
- Replay cursor, speed controls, annotations, before/after review and strategy-rule checklist.
- Clearly label journal-only replay vs market-data replay.

### I. Insight Feed and reporting
- Daily, weekly and monthly insight cards with evidence, metric, action, pin/save and drill-down.
- PDF/CSV/XLSX reports; optional image share.
- QR share includes only user-selected non-sensitive summaries; never credentials or raw account identifiers.
- Optional voice notes are local and user-controlled.

### J. Academy, news and integrations
- Academy supports mixed ordered content blocks, folders, tags, search, favorites, progress, quizzes and trade/strategy links.
- Economic calendar uses a permitted provider or user import; source, actual/forecast/previous, impact and updated timestamp are mandatory.
- No brittle scraping, fabricated sample events or unsupported "live" claims.
- Future read-only broker/MetaTrader adapters must expose capability/permission checks, sync status, reconciliation and revoke controls.

## Target technical architecture

- Flutter/Dart owns the TX AI module UI and its interaction model. The current Flutter package is a standalone demo and must be embedded through a deliberately tested Android host integration before claiming UI migration complete.
- Rust owns deterministic, testable heavy analytics and scenario calculations with stable input/output schemas.
- Dart owns Persian narrative composition and presentation-level orchestration.
- JavaScript Canvas is reserved for advanced interactive visualizations where Flutter's native chart layer is insufficient; it is not a requirement for every chart.
- C is not a parallel business-logic layer. Use C only for a specific native dependency or stable FFI boundary with measurable benefit.
- Kotlin remains a minimal Android host/bridge until the Flutter module is integrated; existing Compose screens remain untouched during staged migration.
- Local storage migration must preserve the existing SQLite snapshot before introducing normalized, versioned tables. Never discard user data during migration.

## Current branch status at blueprint creation

- Existing: local Forex journal, multiple accounts, strategy snapshots, CSV export, calendar, sessions, education library, weekly planner, reminders, TX AI descriptive analytics and counterfactual basics.
- Newly added in this branch batch: deterministic Revenge Trading evidence analyzer, account-scoped episode output, linked trigger/previous-loss trade presentation in TX AI, and unit tests.
- Not yet complete: persistent user review corrections, episode filters/drill-down screen, remaining Behavioral Lab detectors, Edge Explorer, full Counterfactual Studio, full Command Center radar, Flutter embedding, advanced replay/backtesting, normalized encrypted storage, backup/restore, live economic calendar and broker integrations.
- Do not mark the product release-ready until latest CI, migration tests, Persian RTL smoke tests and physical-device install/launch checks pass.

## Delivery order

1. Stabilize journal data model, persistence and migration safety.
2. Complete Behavioral Lab episode model, review persistence and all linked-trade drill-downs.
3. Complete Command Center and Edge Explorer with deterministic Rust analytics and formula tests.
4. Complete Counterfactual Studio and Process Coach.
5. Embed Flutter TX AI UI without changing existing theme or unrelated Compose routes.
6. Add replay/backtesting only after market data and execution model are real.
7. Add export/backup, localization/accessibility and final device validation.
