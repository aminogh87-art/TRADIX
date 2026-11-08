import 'package:flutter/services.dart';

/// Safe, account-scoped journal payload exchanged with the Android host.
/// It contains journal fields only; broker credentials and account secrets
/// must never be included in this contract.
class TxAiTradeRecord {
  const TxAiTradeRecord({
    required this.id,
    required this.accountId,
    required this.symbol,
    required this.isBuy,
    required this.pnl,
    required this.lot,
    required this.strategy,
    required this.tags,
    required this.emotion,
    this.openedAtMillis,
    this.closedAtMillis,
    this.plannedRisk,
    this.timeframe,
    this.marketCondition,
    this.strategyFollowed,
  });

  final int id;
  final String accountId;
  final String symbol;
  final bool isBuy;
  final double pnl;
  final double lot;
  final String strategy;
  final List<String> tags;
  final String emotion;
  final int? openedAtMillis;
  final int? closedAtMillis;
  final double? plannedRisk;
  final String? timeframe;
  final String? marketCondition;
  final bool? strategyFollowed;

  static TxAiTradeRecord? fromMap(
    Map<Object?, Object?> map, {
    required String selectedAccountId,
  }) {
    final accountId = map['accountId'];
    if (accountId is! String || accountId != selectedAccountId) return null;
    final id = map['id'];
    final symbol = map['symbol'];
    final isBuy = map['isBuy'];
    final pnl = _finiteNumber(map['pnl']);
    final lot = _finiteNumber(map['lot']);
    if (id is! int || symbol is! String || isBuy is! bool || pnl == null || lot == null || lot <= 0) {
      return null;
    }
    final rawTags = map['tags'];
    final tags = rawTags is List
        ? rawTags.whereType<String>().toList(growable: false)
        : const <String>[];
    return TxAiTradeRecord(
      id: id,
      accountId: accountId,
      symbol: symbol,
      isBuy: isBuy,
      pnl: pnl,
      lot: lot,
      strategy: map['strategy'] is String ? map['strategy'] as String : '',
      tags: tags,
      emotion: map['emotion'] is String ? map['emotion'] as String : '',
      openedAtMillis: map['openedAtMillis'] is int ? map['openedAtMillis'] as int : null,
      closedAtMillis: map['closedAtMillis'] is int ? map['closedAtMillis'] as int : null,
      plannedRisk: _finiteNumber(map['plannedRisk']),
      timeframe: map['timeframe'] is String ? map['timeframe'] as String : null,
      marketCondition: map['marketCondition'] is String ? map['marketCondition'] as String : null,
      strategyFollowed: map['strategyFollowed'] is bool ? map['strategyFollowed'] as bool : null,
    );
  }
}

class TxAiAccountSnapshot {
  const TxAiAccountSnapshot({
    required this.accountId,
    required this.accountName,
    required this.currency,
    required this.trades,
  });

  final String accountId;
  final String accountName;
  final String currency;
  final List<TxAiTradeRecord> trades;

  factory TxAiAccountSnapshot.fromMap(Map<Object?, Object?> map) {
    final accountId = map['accountId'];
    if (accountId is! String || accountId.isEmpty) {
      throw const FormatException('Selected account id is missing.');
    }
    final rawTrades = map['trades'];
    final trades = <TxAiTradeRecord>[];
    if (rawTrades is List) {
      for (final item in rawTrades) {
        if (item is Map) {
          final record = TxAiTradeRecord.fromMap(
            Map<Object?, Object?>.from(item),
            selectedAccountId: accountId,
          );
          if (record != null) trades.add(record);
        }
      }
    }
    return TxAiAccountSnapshot(
      accountId: accountId,
      accountName: map['accountName'] is String ? map['accountName'] as String : '',
      currency: map['currency'] is String ? map['currency'] as String : '',
      trades: List<TxAiTradeRecord>.unmodifiable(trades),
    );
  }

  List<double> get realizedPnls => trades.map((trade) => trade.pnl).toList(growable: false);
}

double? _finiteNumber(Object? value) {
  if (value is! num) return null;
  final number = value.toDouble();
  return number.isFinite ? number : null;
}

/// Host method-channel boundary. The Android host must implement the
/// loadSelectedAccountSnapshot method and return the selected account's journal only.
class TxAiDataBridge {
  TxAiDataBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel('tradenex/tx_ai');

  final MethodChannel _channel;

  Future<TxAiAccountSnapshot> loadSelectedAccountSnapshot() async {
    final payload = await _channel.invokeMapMethod<Object?, Object?>(
      'loadSelectedAccountSnapshot',
    );
    if (payload == null) {
      throw const FormatException('The host returned no TX AI account snapshot.');
    }
    return TxAiAccountSnapshot.fromMap(payload);
  }
}
