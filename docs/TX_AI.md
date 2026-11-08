# TX AI, trade analytics, and Persian calendar

## Scope and navigation

TX AI is available from the app's More / hamburger-style menu and uses only the selected account's saved journal trades. It is an explainable local analytics engine; it does not send data to an external AI provider, issue trade signals, or execute orders.

## Behavioral profile and breakdowns

The report groups recorded trades by:
- Forex session, using each session's IANA timezone and daylight-saving rules
- weekday, calendar day, and Monday-start trading week
- recorded timeframe and market condition
- recorded emotional state and symbol
- named strategy

A best-pattern insight is withheld until at least five matching trades exist. A missing timestamp, timeframe, market condition, emotion, or compliance value is not guessed or backfilled.

## Counterfactual scenarios

### Excluding off-plan trades

A trade is classified as off-plan only when the user explicitly marks it as outside the plan or records a behavior tag such as FOMO, Revenge, No-plan, or Off-strategy. A blank strategy name alone is not treated as proof of a rule violation.

The hypothetical result is the sum of P&L for trades not classified as off-plan. The report also displays the difference from actual recorded P&L.

### Reducing risk by 50%

The illustrative reduced-risk result is 50% of recorded net P&L. This assumes P&L scales linearly with position size. It is not a market replay or broker execution simulation and does not model margin, slippage, liquidity, or changed trader behavior.

## Monthly progress scores

Each score is independent and may be withheld when data coverage is insufficient.

| Score | Method |
|---|---|
| Discipline | Requires at least five monthly trades and behavioral tags on at least 70% of them; score is reduced by recorded FOMO, Revenge, Overtrading, No-plan, or Off-strategy flags. |
| Risk management | Requires at least three trades with a recorded planned cash risk; checks planned risk against the account's configured risk budget. |
| Strategy adherence | Requires at least five monthly trades, at least three explicit compliance records, and at least 70% coverage; score is the share marked as following the plan. |
| Decision quality | Share of trades with entry, exit, stop-loss, and strategy recorded; requires at least five monthly trades. |

The six-month trend includes months with no trades; missing data is shown as unavailable, not as zero performance.

## Trade-based equity candles

The performance chart now creates one candle for every recorded trade in the selected period. Each candle opens at equity before that trade and closes at equity after its recorded P&L. Since the journal does not contain intratrade OHLC or tick data, the wick uses only the two observable equity values; no intratrade high/low is invented. The chart shows up to the latest 60 trades with compact candle bodies.

## Heatmap and Jalali calendar

The performance heatmap scales color intensity against the largest absolute daily P&L in its recent window instead of fixed account-currency thresholds. Its day labels use Jalali dates.

The trade calendar uses Jalali month boundaries, Persian date labels, Saturday-first week layout, month navigation, and a selectable day detail panel listing that day's saved trades.

## Inspiration from public GitHub projects

The feature review used public project documentation as design references, not as copied implementation:
- [MyTradeBook](https://github.com/najamaltaf88/MyTradeBook): account-scoped journaling, psychology context, strategy review, heatmaps, and coaching workflows.
- [TradeLedger](https://github.com/manju0912/tradeledger): calendar P&L heatmap, symbol breakdowns, and drill-down from analytics to trade records.
- [Trade Ledger Pro](https://github.com/TheCodeWiz/Trade-Ledger-pro): drawdown, risk/reward, daily/weekly/monthly performance views, and coaching concepts.

## Explicit limitations

- The economic-news tab has no live provider configured. The UI does not display fabricated sample events; a trusted news feed/API integration is still required.
- No intratrade OHLC is available from manual journal entries. A future broker/MT connector could supply that data.
- TX AI is local deterministic analytics, not an LLM-based coach. This keeps current analysis offline and reproducible.
- Historical trades without explicit strategy compliance remain unclassified; no inferred compliance score is created.
