import 'dart:ffi';
import 'dart:io';

import 'package:ffi/ffi.dart';

typedef _NativeMetrics = Int32 Function(
  Pointer<Double>, UintPtr, Pointer<Double>, UintPtr,
);
typedef _DartMetrics = int Function(
  Pointer<Double>, int, Pointer<Double>, int,
);

/// Structured result from the deterministic Rust analytics engine.
class TxRustMetrics {
  const TxRustMetrics({
    required this.net,
    required this.winRate,
    required this.profitFactor,
    required this.maxDrawdown,
    required this.expectancy,
    required this.averageWin,
    required this.averageLoss,
    required this.recoveryFactor,
    required this.largestWinShare,
    required this.largestLossShare,
    required this.sampleSize,
  });

  final double net;
  final double winRate;
  final double profitFactor;
  final double maxDrawdown;
  final double expectancy;
  final double averageWin;
  final double averageLoss;
  final double recoveryFactor;
  final double largestWinShare;
  final double largestLossShare;
  final int sampleSize;
}

/// Dart-to-Rust bridge. The host app must package the matching
/// libtradenex_core.so for each Android ABI before constructing this class.
class TxRustAnalytics {
  TxRustAnalytics._(this._library)
      : _calculate = _library.lookupFunction<_NativeMetrics, _DartMetrics>(
          'tx_ai_performance_metrics',
        );

  final DynamicLibrary _library;
  final _DartMetrics _calculate;

  static TxRustAnalytics openAndroid() {
    if (!Platform.isAndroid) {
      throw UnsupportedError('Rust analytics library is currently packaged for Android only.');
    }
    return TxRustAnalytics._(DynamicLibrary.open('libtradenex_core.so'));
  }

  static TxRustAnalytics? tryOpenAndroid() {
    try {
      return openAndroid();
    } on Object {
      return null;
    }
  }

  TxRustMetrics calculate(List<double> realizedPnls) {
    final input = calloc<Double>(realizedPnls.isEmpty ? 1 : realizedPnls.length);
    final output = calloc<Double>(10);
    try {
      for (var i = 0; i < realizedPnls.length; i++) {
        input[i] = realizedPnls[i];
      }
      final status = _calculate(input, realizedPnls.length, output, 10);
      if (status != 0) {
        throw StateError('Rust analytics rejected the input buffer (status $status).');
      }
      return TxRustMetrics(
        net: output[0],
        winRate: output[1],
        profitFactor: output[2],
        maxDrawdown: output[3],
        expectancy: output[4],
        averageWin: output[5],
        averageLoss: output[6],
        recoveryFactor: output[7],
        largestWinShare: output[8],
        largestLossShare: output[9],
        sampleSize: realizedPnls.where((v) => v.isFinite).length,
      );
    } finally {
      calloc.free(input);
      calloc.free(output);
    }
  }
}
