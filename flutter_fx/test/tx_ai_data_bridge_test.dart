import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_data_bridge.dart';

void main() {
  test('snapshot keeps only selected-account trades', () {
    final snapshot = TxAiAccountSnapshot.fromMap({
      'accountId': 'account-1',
      'accountName': 'Main',
      'currency': 'USD',
      'trades': [
        {
          'id': 1,
          'accountId': 'account-1',
          'symbol': 'EURUSD',
          'isBuy': true,
          'pnl': 25.5,
          'lot': 0.1,
          'strategy': 'ICT',
          'tags': ['London'],
          'emotion': 'Calm',
          'strategyFollowed': true,
        },
        {
          'id': 2,
          'accountId': 'account-2',
          'symbol': 'XAUUSD',
          'isBuy': false,
          'pnl': 80,
          'lot': 0.2,
        },
      ],
    });

    expect(snapshot.accountId, 'account-1');
    expect(snapshot.trades, hasLength(1));
    expect(snapshot.trades.single.id, 1);
    expect(snapshot.realizedPnls, [25.5]);
    expect(snapshot.trades.single.strategyFollowed, isTrue);
  });

  test('invalid or non-finite trade values are not fabricated', () {
    final snapshot = TxAiAccountSnapshot.fromMap({
      'accountId': 'account-1',
      'trades': [
        {
          'id': 1,
          'accountId': 'account-1',
          'symbol': 'EURUSD',
          'isBuy': true,
          'pnl': double.infinity,
          'lot': 0.1,
        },
        {
          'id': 2,
          'accountId': 'account-1',
          'symbol': 'XAUUSD',
          'isBuy': false,
          'pnl': 4.0,
          'lot': 0.0,
        },
      ],
    });

    expect(snapshot.trades, isEmpty);
  });

  test('missing selected account id is rejected', () {
    expect(
      () => TxAiAccountSnapshot.fromMap({'trades': []}),
      throwsFormatException,
    );
  });
}
