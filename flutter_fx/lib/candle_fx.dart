import 'dart:math' as math;
import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

/// The "Candle" theme: colour is the market direction, shape is the candle structure.
///
/// Green = profit, red = loss, gold = brand / break-even / ceilings, sky blue = live things.
/// Mirrors `candlePalette` in `Theme.kt` and `candle_tone` in `rust/src/fx.rs`.
enum CandleTone { green, red, gold, sky }

/// How the nav-bar body colour is chosen.
enum CandleColorMode { auto, manual }

class CandlePalette {
  const CandlePalette._();

  static const ink = Color(0xFF06080C);
  static const text = Color(0xFFEEF2F7);
  static const muted = Color(0xFFA9B3C3);

  static const green = Color(0xFF19D68A);
  static const greenHi = Color(0xFF5DFFC0);
  static const greenDeep = Color(0xFF0FB874);

  static const red = Color(0xFFFF4D5E);
  static const redHi = Color(0xFFFF7D8A);
  static const redDeep = Color(0xFFE03348);

  static const gold = Color(0xFFF2C14E);
  static const goldHi = Color(0xFFFFE08A);
  static const goldDeep = Color(0xFFC98A14);

  static const sky = Color(0xFF5CC8FF);
  static const skyHi = Color(0xFF9FE0FF);
  static const skyDeep = Color(0xFF2A8FC4);

  static Color of(CandleTone t) => switch (t) {
        CandleTone.green => green,
        CandleTone.red => red,
        CandleTone.gold => gold,
        CandleTone.sky => sky,
      };

  static Color hi(CandleTone t) => switch (t) {
        CandleTone.green => greenHi,
        CandleTone.red => redHi,
        CandleTone.gold => goldHi,
        CandleTone.sky => skyHi,
      };

  static Color deep(CandleTone t) => switch (t) {
        CandleTone.green => greenDeep,
        CandleTone.red => redDeep,
        CandleTone.gold => goldDeep,
        CandleTone.sky => skyDeep,
      };

  static String labelFa(CandleTone t) => switch (t) {
        CandleTone.green => 'سبز',
        CandleTone.red => 'قرمز',
        CandleTone.gold => 'طلایی',
        CandleTone.sky => 'آبی',
      };
}

/// Same rule as `candle_tone` in Rust: profit green, loss red, break-even (within [flatBand]) gold.
/// A non-finite P&L is treated as break-even.
CandleTone candleToneFor(double pnl, {double flatBand = 0.5}) {
  if (!pnl.isFinite) return CandleTone.gold;
  final band = flatBand.isFinite ? flatBand.abs() : 0.0;
  if (pnl > band) return CandleTone.green;
  if (pnl < -band) return CandleTone.red;
  return CandleTone.gold;
}

/// Holds the colour mode (auto from today's P&L, or a manually picked tone).
class CandleController extends ChangeNotifier {
  CandleController({
    CandleColorMode mode = CandleColorMode.auto,
    CandleTone manual = CandleTone.green,
    double todayPnl = 0,
  })  : _mode = mode,
        _manual = manual,
        _todayPnl = todayPnl;

  CandleColorMode _mode;
  CandleTone _manual;
  double _todayPnl;

  CandleColorMode get mode => _mode;
  CandleTone get manual => _manual;
  double get todayPnl => _todayPnl;

  /// The tone the UI should use right now.
  CandleTone get tone => _mode == CandleColorMode.auto ? candleToneFor(_todayPnl) : _manual;

  set mode(CandleColorMode v) {
    if (v == _mode) return;
    _mode = v;
    notifyListeners();
  }

  set manual(CandleTone v) {
    if (v == _manual) return;
    _manual = v;
    notifyListeners();
  }

  set todayPnl(double v) {
    if (v == _todayPnl) return;
    _todayPnl = v;
    notifyListeners();
  }
}

/// One candle glyph: a gold wick and a rounded body. [top] and [height] are fractions of the glyph height.
class CandleMark extends StatelessWidget {
  const CandleMark({
    super.key,
    required this.color,
    this.wick = CandlePalette.gold,
    this.top = 0.25,
    this.height = 0.45,
    this.size = const Size(18, 28),
    this.hollow = false,
  });

  final Color color;
  final Color wick;
  final double top;
  final double height;
  final Size size;
  final bool hollow;

  @override
  Widget build(BuildContext context) => ExcludeSemantics(
        child: CustomPaint(
          size: size,
          painter: _CandleMarkPainter(color, wick, top, height, hollow),
        ),
      );
}

class _CandleMarkPainter extends CustomPainter {
  _CandleMarkPainter(this.color, this.wick, this.top, this.height, this.hollow);

  final Color color, wick;
  final double top, height;
  final bool hollow;

  @override
  void paint(Canvas canvas, Size size) {
    final cx = size.width / 2;
    canvas.drawLine(
      Offset(cx, 1),
      Offset(cx, size.height - 1),
      Paint()
        ..color = wick
        ..strokeWidth = 1.6
        ..strokeCap = StrokeCap.round,
    );
    final bodyW = size.width * 0.7;
    final body = RRect.fromRectAndRadius(
      Rect.fromLTWH(cx - bodyW / 2, size.height * top, bodyW, size.height * height),
      Radius.circular(bodyW * 0.3),
    );
    canvas.drawRRect(
      body,
      Paint()
        ..color = color
        ..style = hollow ? PaintingStyle.stroke : PaintingStyle.fill
        ..strokeWidth = 1.8,
    );
  }

  @override
  bool shouldRepaint(_CandleMarkPainter old) =>
      old.color != color || old.wick != wick || old.top != top || old.height != height || old.hollow != hollow;
}

/// Top-corner menu button that is itself a candle (wick, body, wick).
/// Opening it turns the two wicks into a green/red cross and fades the body away.
class CandleHamburger extends StatefulWidget {
  const CandleHamburger({
    super.key,
    required this.open,
    required this.onPressed,
    this.tone = CandleTone.green,
    this.semanticLabel = 'منو',
  });

  final bool open;
  final VoidCallback onPressed;
  final CandleTone tone;
  final String semanticLabel;

  @override
  State<CandleHamburger> createState() => _CandleHamburgerState();
}

class _CandleHamburgerState extends State<CandleHamburger> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 320),
    value: widget.open ? 1 : 0,
  );

  @override
  void didUpdateWidget(CandleHamburger old) {
    super.didUpdateWidget(old);
    if (widget.open != old.open) {
      widget.open ? _c.forward() : _c.reverse();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: widget.semanticLabel,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: widget.onPressed,
        child: SizedBox(
          width: 48,
          height: 48,
          child: AnimatedBuilder(
            animation: _c,
            builder: (_, __) => CustomPaint(
              painter: _HamburgerPainter(Curves.easeInOut.transform(_c.value), CandlePalette.of(widget.tone)),
            ),
          ),
        ),
      ),
    );
  }
}

class _HamburgerPainter extends CustomPainter {
  _HamburgerPainter(this.p, this.bodyColor);

  final double p;
  final Color bodyColor;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final tile = RRect.fromRectAndRadius(
      Rect.fromCenter(center: c, width: 44, height: 44),
      const Radius.circular(16),
    );
    canvas.drawRRect(tile, Paint()..color = Colors.white.withOpacity(0.07));
    canvas.drawRRect(
      tile,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1
        ..color = Color.lerp(bodyColor, CandlePalette.red, p)!.withOpacity(0.5),
    );

    void bar(Offset at, double w, double h, double angle, Color color) {
      canvas.save();
      canvas.translate(at.dx, at.dy);
      canvas.rotate(angle);
      canvas.drawRRect(
        RRect.fromRectAndRadius(Rect.fromCenter(center: Offset.zero, width: w, height: h), Radius.circular(h / 2)),
        Paint()..color = color,
      );
      canvas.restore();
    }

    // body fades out while opening
    if (p < 1) {
      bar(c, 18, 7, 0, bodyColor.withOpacity(1 - p));
    }
    final dy = 8.0 * (1 - p);
    bar(c.translate(0, -dy), _lerp(8, 22, p), 2.4, math.pi / 4 * p, Color.lerp(CandlePalette.gold, CandlePalette.green, p)!);
    bar(c.translate(0, dy), _lerp(12, 22, p), 2.4, -math.pi / 4 * p, Color.lerp(CandlePalette.gold, CandlePalette.red, p)!);
  }

  @override
  bool shouldRepaint(_HamburgerPainter old) => old.p != p || old.bodyColor != bodyColor;
}

double _lerp(double a, double b, double t) => a + (b - a) * t;

/// A tab in [CandleNavBar].
class CandleNavItem {
  const CandleNavItem(this.label, this.icon);
  final String label;
  final IconData icon;
}

/// Horizontal glass candle used as the bottom navigation.
///
/// Gold wick with end caps runs across the full width, the glass body is tinted by [tone] and casts a
/// coloured shadow, and the gold "doji" in the middle adds a trade. Exactly four [items] are expected:
/// two before and two after the add button.
class CandleNavBar extends StatelessWidget {
  const CandleNavBar({
    super.key,
    required this.items,
    required this.currentIndex,
    required this.onSelected,
    required this.onAdd,
    this.tone = CandleTone.green,
    this.glow = 1.0,
    this.addLabel = 'ثبت معامله جدید',
  }) : assert(items.length == 4, 'CandleNavBar expects exactly four items');

  final List<CandleNavItem> items;
  final int currentIndex;
  final ValueChanged<int> onSelected;
  final VoidCallback onAdd;
  final CandleTone tone;

  /// 0 off .. 1.6 intense, same scale as the app's glow setting.
  final double glow;
  final String addLabel;

  @override
  Widget build(BuildContext context) {
    final target = CandlePalette.of(tone);
    return SizedBox(
      height: 92,
      child: Stack(
        children: [
          const Positioned.fill(child: CustomPaint(painter: _WickPainter())),
          Positioned(
            left: 44,
            right: 44,
            bottom: 4,
            height: 20,
            child: TweenAnimationBuilder<Color?>(
              tween: ColorTween(end: target),
              duration: const Duration(milliseconds: 600),
              builder: (_, c, __) => DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(40),
                  boxShadow: [
                    BoxShadow(
                      color: (c ?? target).withOpacity((0.6 * glow).clamp(0.0, 0.9)),
                      blurRadius: 28,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            left: 28,
            right: 28,
            top: 6,
            bottom: 6,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(32),
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
                child: TweenAnimationBuilder<Color?>(
                  tween: ColorTween(end: target),
                  duration: const Duration(milliseconds: 600),
                  builder: (_, c, __) {
                    final body = c ?? target;
                    return DecoratedBox(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(32),
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [
                            Colors.white.withOpacity(0.16),
                            body.withOpacity(0.10),
                            body.withOpacity(0.22),
                          ],
                          stops: const [0, 0.55, 1],
                        ),
                        border: Border.all(color: body.withOpacity(0.55)),
                      ),
                      child: Row(
                        children: [
                          for (var i = 0; i < 2; i++)
                            Expanded(child: _NavTab(item: items[i], selected: currentIndex == i, color: body, onTap: () => onSelected(i))),
                          SizedBox(width: 72, child: Center(child: _AddDoji(onTap: onAdd, label: addLabel))),
                          for (var i = 2; i < 4; i++)
                            Expanded(child: _NavTab(item: items[i], selected: currentIndex == i, color: body, onTap: () => onSelected(i))),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _NavTab extends StatelessWidget {
  const _NavTab({required this.item, required this.selected, required this.color, required this.onTap});

  final CandleNavItem item;
  final bool selected;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final fg = selected ? color : CandlePalette.muted;
    return Semantics(
      button: true,
      selected: selected,
      label: item.label,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: Center(
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOut,
            width: 56,
            height: 58,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              color: selected ? color.withOpacity(0.18) : Colors.transparent,
              border: Border.all(color: selected ? color.withOpacity(0.55) : Colors.transparent),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(item.icon, size: 22, color: fg),
                const SizedBox(height: 3),
                Text(
                  item.label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 11, color: fg, fontWeight: selected ? FontWeight.w700 : FontWeight.w500),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _AddDoji extends StatelessWidget {
  const _AddDoji({required this.onTap, required this.label});

  final VoidCallback onTap;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: Container(
          width: 60,
          height: 48,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [CandlePalette.goldHi, Color(0xFFF2B43C), CandlePalette.goldDeep],
              stops: [0, 0.55, 1],
            ),
            boxShadow: [BoxShadow(color: CandlePalette.gold.withOpacity(0.7), blurRadius: 18, offset: const Offset(0, 8), spreadRadius: -6)],
          ),
          child: const Icon(Icons.add_rounded, size: 28, color: Color(0xFF1A1204)),
        ),
      ),
    );
  }
}

/// Gold wick across the whole bar with small end caps.
class _WickPainter extends CustomPainter {
  const _WickPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final y = size.height / 2;
    final line = Paint()
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round
      ..shader = const LinearGradient(
        colors: [CandlePalette.gold, Color(0x26F2C14E), Color(0x26F2C14E), CandlePalette.gold],
        stops: [0, 0.12, 0.88, 1],
      ).createShader(Rect.fromLTWH(0, 0, size.width, 1));
    canvas.drawLine(Offset(1.5, y), Offset(size.width - 1.5, y), line);
    final cap = Paint()
      ..color = CandlePalette.gold
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.solid, 2);
    canvas.drawLine(Offset(1.5, y - 13), Offset(1.5, y + 13), cap);
    canvas.drawLine(Offset(size.width - 1.5, y - 13), Offset(size.width - 1.5, y + 13), cap);
  }

  @override
  bool shouldRepaint(_WickPainter old) => false;
}

/// One row of [CandleMenuPanel]: the glyph's body height and colour make every entry recognisable.
class CandleMenuEntry {
  const CandleMenuEntry({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.tone,
    this.bodyTop = 0.25,
    this.bodyHeight = 0.45,
    this.badge,
  });

  final String id;
  final String title;
  final String subtitle;
  final CandleTone tone;
  final double bodyTop;
  final double bodyHeight;
  final String? badge;
}

/// The menu opened by [CandleHamburger]: glass panel, one candle glyph per destination.
class CandleMenuPanel extends StatelessWidget {
  const CandleMenuPanel({super.key, required this.entries, required this.onSelected, this.footer});

  final List<CandleMenuEntry> entries;
  final ValueChanged<String> onSelected;
  final String? footer;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(28),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 22, sigmaY: 22),
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(28),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Colors.white.withOpacity(0.10), Colors.white.withOpacity(0.03)],
            ),
            border: Border.all(color: Colors.white.withOpacity(0.14)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                for (final e in entries) _MenuRow(entry: e, onTap: () => onSelected(e.id)),
                if (footer != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(8, 12, 8, 6),
                    child: Text(
                      footer!,
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 12, height: 1.8, color: CandlePalette.muted),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({required this.entry, required this.onTap});

  final CandleMenuEntry entry;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = CandlePalette.of(entry.tone);
    return Semantics(
      button: true,
      label: '${entry.title}، ${entry.subtitle}',
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 60),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    color: color.withOpacity(0.14),
                    border: Border.all(color: color.withOpacity(0.4)),
                  ),
                  child: CandleMark(
                    color: color,
                    top: entry.bodyTop,
                    height: entry.bodyHeight,
                    size: const Size(20, 28),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(entry.title, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: CandlePalette.text)),
                      Text(entry.subtitle, style: const TextStyle(fontSize: 12, color: CandlePalette.muted)),
                    ],
                  ),
                ),
                if (entry.badge != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      color: CandlePalette.sky.withOpacity(0.14),
                      border: Border.all(color: CandlePalette.sky.withOpacity(0.4)),
                    ),
                    child: Text(entry.badge!, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: CandlePalette.skyHi)),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Settings > Themes block for the Candle theme: auto/manual colour mode plus the four manual tones.
/// In auto mode the tone follows today's P&L; pass [onPreviewPnl] to show buttons that try each case.
class CandleThemePanel extends StatelessWidget {
  const CandleThemePanel({super.key, required this.controller, this.onPreviewPnl});

  final CandleController controller;
  final ValueChanged<double>? onPreviewPnl;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) {
        final auto = controller.mode == CandleColorMode.auto;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                _ModeButton(label: 'خودکار', selected: auto, onTap: () => controller.mode = CandleColorMode.auto),
                const SizedBox(width: 8),
                _ModeButton(label: 'دستی', selected: !auto, onTap: () => controller.mode = CandleColorMode.manual),
              ],
            ),
            const SizedBox(height: 12),
            if (auto) ...[
              const Text(
                'رنگ از نتیجه امروز تعیین می‌شود: سود سبز، ضرر قرمز، سر به سر طلایی.',
                style: TextStyle(fontSize: 13, height: 1.8, color: CandlePalette.muted),
              ),
              if (onPreviewPnl != null) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    for (final v in const [(98.9, 'سود'), (-64.2, 'ضرر'), (0.0, 'سر به سر')])
                      Expanded(
                        child: Padding(
                          padding: const EdgeInsetsDirectional.only(end: 8),
                          child: _ChoiceButton(
                            label: v.$2,
                            color: CandlePalette.of(candleToneFor(v.$1)),
                            selected: candleToneFor(controller.todayPnl) == candleToneFor(v.$1),
                            onTap: () => onPreviewPnl!(v.$1),
                          ),
                        ),
                      ),
                  ],
                ),
              ],
            ] else
              Row(
                children: [
                  for (final t in CandleTone.values)
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsetsDirectional.only(end: 8),
                        child: _ChoiceButton(
                          label: CandlePalette.labelFa(t),
                          color: CandlePalette.of(t),
                          selected: controller.manual == t,
                          onTap: () => controller.manual = t,
                        ),
                      ),
                    ),
                ],
              ),
          ],
        );
      },
    );
  }
}

class _ModeButton extends StatelessWidget {
  const _ModeButton({required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Semantics(
        button: true,
        selected: selected,
        label: label,
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onTap,
          child: Container(
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              gradient: selected ? const LinearGradient(colors: [CandlePalette.goldHi, Color(0xFFE8A92F)]) : null,
              color: selected ? null : Colors.white.withOpacity(0.06),
            ),
            child: Text(
              label,
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: selected ? const Color(0xFF1A1204) : CandlePalette.muted),
            ),
          ),
        ),
      ),
    );
  }
}

class _ChoiceButton extends StatelessWidget {
  const _ChoiceButton({required this.label, required this.color, required this.selected, required this.onTap});

  final String label;
  final Color color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selected,
      label: label,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: Container(
          height: 56,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            color: color.withOpacity(selected ? 0.28 : 0.10),
            border: Border.all(color: color.withOpacity(selected ? 0.95 : 0.4), width: selected ? 2 : 1),
            boxShadow: selected ? [BoxShadow(color: color.withOpacity(0.5), blurRadius: 14)] : null,
          ),
          child: Text(label, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: CandlePalette.text)),
        ),
      ),
    );
  }
}
