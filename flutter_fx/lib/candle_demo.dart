import 'package:flutter/material.dart';

import 'candle_fx.dart';

/// Run with: flutter run -t lib/candle_demo.dart
///
/// Standalone preview of the Candle theme kit: candle hamburger (top-left), glass candle nav bar,
/// candle menu and the auto / manual colour control.
void main() => runApp(const CandleDemo());

class CandleDemo extends StatefulWidget {
  const CandleDemo({super.key});

  @override
  State<CandleDemo> createState() => _CandleDemoState();
}

class _CandleDemoState extends State<CandleDemo> {
  final CandleController _controller = CandleController(todayPnl: 98.9);
  bool _menuOpen = false;
  int _tab = 0;

  static const _items = [
    CandleNavItem('خانه', Icons.home_rounded),
    CandleNavItem('گزارش‌ها', Icons.bar_chart_rounded),
    CandleNavItem('آموزش', Icons.menu_book_rounded),
    CandleNavItem('بیشتر', Icons.grid_view_rounded),
  ];

  static const _entries = [
    CandleMenuEntry(id: 'tx_ai', title: 'TX AI', subtitle: 'تحلیل توضیح‌پذیر معاملات', tone: CandleTone.sky, bodyTop: 0.22, bodyHeight: 0.5, badge: 'جدید'),
    CandleMenuEntry(id: 'weekly', title: 'برنامه هفتگی', subtitle: 'انضباط و مرور روزانه', tone: CandleTone.gold, bodyTop: 0.32, bodyHeight: 0.36),
    CandleMenuEntry(id: 'edu', title: 'آموزش', subtitle: 'درس‌ها و اشتراک با QR', tone: CandleTone.green, bodyTop: 0.15, bodyHeight: 0.62),
    CandleMenuEntry(id: 'calendar', title: 'تقویم', subtitle: 'معاملات شخصی و اخبار', tone: CandleTone.sky, bodyTop: 0.42, bodyHeight: 0.32),
    CandleMenuEntry(id: 'strategies', title: 'استراتژی‌ها', subtitle: 'نسخه‌ها و مقایسه', tone: CandleTone.gold, bodyTop: 0.18, bodyHeight: 0.3),
    CandleMenuEntry(id: 'tags', title: 'برچسب‌ها', subtitle: 'احساس، اشتباه و الگو', tone: CandleTone.red, bodyTop: 0.34, bodyHeight: 0.42),
    CandleMenuEntry(id: 'settings', title: 'تنظیمات', subtitle: 'تم‌ها، حساب‌ها، آیکون، اعلان', tone: CandleTone.green, bodyTop: 0.3, bodyHeight: 0.3),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      builder: (context, child) => Directionality(textDirection: TextDirection.rtl, child: child!),
      home: Scaffold(
        backgroundColor: CandlePalette.ink,
        body: AnimatedBuilder(
          animation: _controller,
          builder: (context, _) => SafeArea(
            child: Stack(
              children: [
                Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                      child: Row(
                        children: [
                          const Expanded(
                            child: Text('TRADIX', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: 2, color: CandlePalette.goldHi)),
                          ),
                          // RTL: the last child of the Row is the top-left corner.
                          CandleHamburger(
                            open: _menuOpen,
                            tone: _controller.tone,
                            onPressed: () => setState(() => _menuOpen = !_menuOpen),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: ListView(
                        padding: const EdgeInsets.all(16),
                        children: [
                          const Text('رنگ بدنه ناوبری', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: CandlePalette.text)),
                          const SizedBox(height: 12),
                          CandleThemePanel(
                            controller: _controller,
                            onPreviewPnl: (v) => _controller.todayPnl = v,
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                      child: CandleNavBar(
                        items: _items,
                        currentIndex: _tab,
                        tone: _controller.tone,
                        onSelected: (i) => setState(() => _tab = i),
                        onAdd: () {},
                      ),
                    ),
                  ],
                ),
                if (_menuOpen) ...[
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () => setState(() => _menuOpen = false),
                      child: ColoredBox(color: Colors.black.withOpacity(0.6)),
                    ),
                  ),
                  PositionedDirectional(
                    top: 64,
                    start: 16,
                    end: 16,
                    child: CandleMenuPanel(
                      entries: _entries,
                      footer: 'TRADIX سیگنال نمی‌دهد و معامله انجام نمی‌دهد.',
                      onSelected: (_) => setState(() => _menuOpen = false),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
