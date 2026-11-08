import 'dart:math' as math;
import 'package:flutter/material.dart';

/// Spinning dotted globe with pulsing city markers.
class GlobeFx extends StatefulWidget {
  const GlobeFx({super.key, this.openSessions = const {2}});

  /// Indexes of open sessions: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
  final Set<int> openSessions;

  @override
  State<GlobeFx> createState() => _GlobeFxState();
}

class _GlobeFxState extends State<GlobeFx> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 40))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => CustomPaint(
        painter: _GlobePainter(_c.value, widget.openSessions),
        child: const SizedBox.expand(),
      ),
    );
  }
}

class _City {
  const _City(this.lat, this.lon, this.color, this.index);
  final double lat, lon;
  final Color color;
  final int index;
}

const _cities = [
  _City(-33.9, 151.2, Color(0xFF4DA3FF), 0),
  _City(35.7, 139.7, Color(0xFFB58CFF), 1),
  _City(51.5, -0.1, Color(0xFFF7CF6B), 2),
  _City(40.7, -74.0, Color(0xFFFF6B7A), 3),
];

class _GlobePainter extends CustomPainter {
  _GlobePainter(this.t, this.open);
  final double t;
  final Set<int> open;

  static const double _tilt = 0.35;

  Offset? _project(double latDeg, double lonDeg, double r, Offset c) {
    final lat = latDeg * math.pi / 180;
    final lon = (lonDeg - t * 360 + 20) * math.pi / 180;
    final x = math.cos(lat) * math.sin(lon);
    final y0 = math.sin(lat);
    final z0 = math.cos(lat) * math.cos(lon);
    final y = y0 * math.cos(_tilt) - z0 * math.sin(_tilt);
    final z = y0 * math.sin(_tilt) + z0 * math.cos(_tilt);
    if (z <= 0) return null;
    return Offset(c.dx + x * r, c.dy - y * r);
  }

  @override
  void paint(Canvas canvas, Size size) {
    final r = size.shortestSide * 0.42;
    final c = size.center(Offset.zero);
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..shader = const RadialGradient(
          colors: [Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)],
        ).createShader(Rect.fromCircle(center: c, radius: r)),
    );
    final grid = Paint()
      ..color = const Color(0x337FB8FF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8;
    for (var lat = -60; lat <= 60; lat += 30) {
      final path = Path();
      var started = false;
      for (var lon = -180; lon <= 180; lon += 6) {
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p == null) {
          started = false;
          continue;
        }
        started ? path.lineTo(p.dx, p.dy) : path.moveTo(p.dx, p.dy);
        started = true;
      }
      canvas.drawPath(path, grid);
    }
    final dot = Paint()..color = const Color(0xAA7FE3D0);
    for (var lat = -60; lat <= 70; lat += 5) {
      for (var lon = -180; lon < 180; lon += 5) {
        if (!_isLand(lat.toDouble(), lon.toDouble())) continue;
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p != null) canvas.drawCircle(p, 1.3, dot);
      }
    }
    final pulse = (t * 22) % 1;
    for (final city in _cities) {
      final p = _project(city.lat, city.lon, r, c);
      if (p == null) continue;
      final isOpen = open.contains(city.index);
      if (isOpen) {
        canvas.drawCircle(
          p,
          6 + 16 * pulse,
          Paint()
            ..color = city.color.withOpacity(0.6 * (1 - pulse))
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
      }
      canvas.drawCircle(p, isOpen ? 5 : 3.5, Paint()..color = city.color.withOpacity(isOpen ? 1 : 0.6));
    }
    canvas.drawCircle(
      c,
      r + 3,
      Paint()
        ..color = const Color(0x554DA3FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6),
    );
  }

  // Very coarse land boxes (lat/lon): enough for a stylised dotted globe.
  bool _isLand(double lat, double lon) {
    const boxes = [
      [15, 70, -165, -55], // North America
      [-55, 12, -80, -35], // South America
      [36, 70, -10, 40], // Europe
      [-35, 35, -17, 50], // Africa
      [10, 70, 40, 145], // Asia
      [-38, -12, 114, 153], // Australia
    ];
    for (final b in boxes) {
      if (lat >= b[0] && lat <= b[1] && lon >= b[2] && lon <= b[3]) return true;
    }
    return false;
  }

  @override
  bool shouldRepaint(covariant _GlobePainter old) => old.t != t || old.open != open;
}

/// Number that counts up to [value] with a glow.
class GlowNumber extends StatelessWidget {
  const GlowNumber({super.key, required this.value, this.suffix = '', this.color = const Color(0xFF2BE8A8)});
  final double value;
  final String suffix;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: value),
      duration: const Duration(milliseconds: 1200),
      curve: Curves.easeOutCubic,
      builder: (_, v, __) => Text(
        '${v.toStringAsFixed(2)}$suffix',
        style: TextStyle(
          fontSize: 44,
          fontWeight: FontWeight.w800,
          color: color,
          shadows: [Shadow(color: color.withOpacity(0.5), blurRadius: 22)],
        ),
      ),
    );
  }
}

/// Six-axis TX AI radar. Values are normalized 0..100 and never inferred by the widget.
class TxRadar extends StatelessWidget {
  const TxRadar({
    super.key,
    required this.values,
    this.labels = const ['Win Rate', 'Profit Factor', 'Expectancy', 'Consistency', 'Discipline', 'Session Edge'],
    this.onAxisTap,
  }) : assert(values.length == 6), assert(labels.length == 6);

  final List<double?> values;
  final List<String> labels;
  final ValueChanged<int>? onAxisTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 300,
      child: LayoutBuilder(builder: (context, constraints) {
        final side = math.min(constraints.maxWidth, 300.0);
        return Center(
          child: SizedBox(
            width: side,
            height: side,
            child: Stack(
              children: [
                Positioned.fill(child: CustomPaint(painter: _TxRadarPainter(values))),
                for (var i = 0; i < 6; i++)
                  _radarLabel(i, side, labels[i], values[i], onAxisTap),
              ],
            ),
          ),
        );
      }),
    );
  }

  Widget _radarLabel(int index, double side, String label, double? value, ValueChanged<int>? onTap) {
    final angle = -math.pi / 2 + index * math.pi / 3;
    final center = side / 2;
    final radius = side * .43;
    final x = center + math.cos(angle) * radius;
    final y = center + math.sin(angle) * radius;
    return Positioned(
      left: (x - 43).clamp(0.0, side - 86).toDouble(),
      top: (y - 18).clamp(0.0, side - 36).toDouble(),
      width: 86,
      height: 36,
      child: GestureDetector(
        onTap: onTap == null ? null : () => onTap(index),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: const Color(0xDD0A1730),
            borderRadius: BorderRadius.circular(9),
            border: Border.all(color: const Color(0x334CCBFF)),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(label, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 9)),
              Text(value == null ? '—' : value.toStringAsFixed(0),
                style: const TextStyle(color: Color(0xFF55D9F5), fontWeight: FontWeight.w800, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }
}

class _TxRadarPainter extends CustomPainter {
  _TxRadarPainter(this.values);
  final List<double?> values;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final radius = size.shortestSide * .29;
    Offset point(int i, double r) {
      final a = -math.pi / 2 + i * math.pi / 3;
      return c + Offset(math.cos(a) * r, math.sin(a) * r);
    }
    final grid = Paint()..color = const Color(0x335A8AB8)..style = PaintingStyle.stroke..strokeWidth = 1;
    for (final level in [.25, .5, .75, 1.0]) {
      final path = Path();
      for (var i = 0; i < 6; i++) {
        final p = point(i, radius * level);
        i == 0 ? path.moveTo(p.dx, p.dy) : path.lineTo(p.dx, p.dy);
      }
      path.close();
      canvas.drawPath(path, grid);
    }
    for (var i = 0; i < 6; i++) {
      canvas.drawLine(c, point(i, radius), grid);
    }
    if (values.every((v) => v == null)) return;
    final shape = Path();
    for (var i = 0; i < 6; i++) {
      final p = point(i, radius * ((values[i] ?? 0).clamp(0, 100).toDouble() / 100));
      i == 0 ? shape.moveTo(p.dx, p.dy) : shape.lineTo(p.dx, p.dy);
    }
    shape.close();
    canvas.drawPath(shape, Paint()..color = const Color(0x3355D9F5)..style = PaintingStyle.fill);
    canvas.drawPath(shape, Paint()..color = const Color(0xFF55D9F5)..style = PaintingStyle.stroke..strokeWidth = 2.2);
    for (var i = 0; i < 6; i++) {
      final v = values[i];
      if (v == null) continue;
      final p = point(i, radius * (v.clamp(0, 100).toDouble() / 100));
      canvas.drawCircle(p, 3.5, Paint()..color = const Color(0xFFFFD36E));
    }
  }

  @override
  bool shouldRepaint(covariant _TxRadarPainter old) => old.values != values;
}

/// Evidence-first insight card. The caller supplies the metric and source-backed explanation.
class TxInsightCard extends StatelessWidget {
  const TxInsightCard({
    super.key, required this.title, required this.description, required this.metric,
    this.actionLabel = 'Deep Dive', this.onAction, this.isPinned = false, this.onPin,
    this.severity = TxInsightSeverity.info,
  });
  final String title, description, metric, actionLabel;
  final VoidCallback? onAction, onPin;
  final bool isPinned;
  final TxInsightSeverity severity;

  @override
  Widget build(BuildContext context) {
    final accent = switch (severity) {
      TxInsightSeverity.positive => const Color(0xFF36D6A0),
      TxInsightSeverity.warning => const Color(0xFFFF7B83),
      TxInsightSeverity.info => const Color(0xFF55D9F5),
    };
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: const Color(0xFF0B1730),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: accent.withOpacity(.35)),
        boxShadow: [BoxShadow(color: accent.withOpacity(.08), blurRadius: 22)],
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(width: 7, height: 7, decoration: BoxDecoration(color: accent, shape: BoxShape.circle)),
          const SizedBox(width: 8),
          Expanded(child: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 14))),
          IconButton(onPressed: onPin, icon: Icon(isPinned ? Icons.push_pin : Icons.push_pin_outlined, color: isPinned ? const Color(0xFFFFD36E) : Colors.white54, size: 18)),
        ]),
        Text(metric, style: TextStyle(color: accent, fontSize: 22, fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        Text(description, style: const TextStyle(color: Color(0xFFB7C9E8), height: 1.5, fontSize: 12)),
        if (onAction != null) Align(
          alignment: AlignmentDirectional.centerEnd,
          child: TextButton(onPressed: onAction, child: Text(actionLabel, style: TextStyle(color: accent))),
        ),
      ]),
    );
  }
}

enum TxInsightSeverity { positive, warning, info }

class TxBehavioralBadge extends StatelessWidget {
  const TxBehavioralBadge({super.key, required this.label, required this.evidenceScore, this.confirmedByUser = false});
  final String label;
  final int evidenceScore;
  final bool confirmedByUser;

  @override
  Widget build(BuildContext context) {
    final color = confirmedByUser ? const Color(0xFFFFD36E)
        : evidenceScore >= 45 ? const Color(0xFFFF7B83) : const Color(0xFF55D9F5);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(color: color.withOpacity(.10), borderRadius: BorderRadius.circular(30), border: Border.all(color: color.withOpacity(.45))),
      child: Text('$label · ${confirmedByUser ? "تأیید کاربر" : "شواهد $evidenceScore/100"}',
        style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w700)),
    );
  }
}

/// Interactive 7 x 24 heatmap. Null cells are unavailable, not zero-result trades.
class TxHeatmap extends StatelessWidget {
  const TxHeatmap({super.key, required this.cells, this.onCellTap});
  final List<List<double?>> cells;
  final void Function(int weekday, int hour)? onCellTap;

  @override
  Widget build(BuildContext context) {
    assert(cells.length == 7 && cells.every((r) => r.length == 24));
    return Column(children: [
      for (var day = 0; day < 7; day++)
        Row(children: [
          SizedBox(width: 25, child: Text(['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'][day], style: const TextStyle(color: Colors.white70, fontSize: 10))),
          for (var hour = 0; hour < 24; hour++)
            Expanded(child: GestureDetector(
              onTap: onCellTap == null ? null : () => onCellTap!(day, hour),
              child: Container(
                height: 18, margin: const EdgeInsets.all(1),
                decoration: BoxDecoration(
                  color: _heatColor(cells[day][hour]),
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            )),
        ]),
    ]);
  }

  Color _heatColor(double? value) {
    if (value == null) return const Color(0xFF18243A);
    final intensity = (value.abs() / 100).clamp(0.08, .9).toDouble();
    return value >= 0
        ? Color.lerp(const Color(0xFF123C4B), const Color(0xFF28D6B0), intensity)!
        : Color.lerp(const Color(0xFF4A2637), const Color(0xFFFF6678), intensity)!;
  }
}

/// Scenario control only emits a parameter; scenario P&L must be computed by the engine.
class TxCounterfactualSlider extends StatelessWidget {
  const TxCounterfactualSlider({super.key, required this.value, required this.onChanged, this.label = 'Risk multiplier'});
  final double value;
  final ValueChanged<double> onChanged;
  final String label;

  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [
      Expanded(child: Text(label, style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 12))),
      Text('${(value * 100).round()}%', style: const TextStyle(color: Color(0xFFFFD36E), fontWeight: FontWeight.w800)),
    ]),
    Slider(value: value.clamp(.1, 2).toDouble(), min: .1, max: 2, divisions: 19, onChanged: onChanged,
      activeColor: const Color(0xFF55D9F5), inactiveColor: const Color(0xFF263A58)),
  ]);
}


class TxProcessTimeline extends StatelessWidget {
  const TxProcessTimeline({super.key, required this.months});
  final List<TxProcessMonth> months;

  @override
  Widget build(BuildContext context) {
    if (months.isEmpty) {
      return const Text('هنوز داده ماهانه کافی ثبت نشده است.', style: TextStyle(color: Color(0xFF91A5C4), fontSize: 12));
    }
    return Column(children: [
      for (final month in months)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 7),
          child: Row(children: [
            SizedBox(width: 64, child: Text(month.label, style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 10))),
            Expanded(child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                minHeight: 8,
                value: month.score == null ? 0 : month.score!.clamp(0, 100).toDouble() / 100,
                backgroundColor: const Color(0xFF1B2A43),
                valueColor: AlwaysStoppedAnimation(month.score == null ? const Color(0xFF52627A) : const Color(0xFFFFD36E)),
              ),
            )),
            SizedBox(width: 38, child: Text(month.score?.toString() ?? '—', textAlign: TextAlign.end,
              style: const TextStyle(color: Color(0xFFFFD36E), fontWeight: FontWeight.w800, fontSize: 12))),
          ]),
        ),
    ]);
  }
}

class TxProcessMonth {
  const TxProcessMonth(this.label, this.score);
  final String label;
  /// Null means insufficient evidence, not zero.
  final int? score;
}
