import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/candle_fx.dart';
import 'package:tradenex_fx/gold_fx.dart';

Widget host(Widget child) => MaterialApp(
      home: Directionality(
        textDirection: TextDirection.rtl,
        child: Scaffold(body: Align(alignment: Alignment.bottomCenter, child: child)),
      ),
    );

const items = [
  CandleNavItem('خانه', Icons.home_rounded),
  CandleNavItem('گزارش‌ها', Icons.bar_chart_rounded),
  CandleNavItem('آموزش', Icons.menu_book_rounded),
  CandleNavItem('بیشتر', Icons.grid_view_rounded),
];

void main() {
  group('candleToneFor', () {
    test('profit is green, loss is red, break-even is gold', () {
      expect(candleToneFor(98.9), CandleTone.green);
      expect(candleToneFor(-64.2), CandleTone.red);
      expect(candleToneFor(0), CandleTone.gold);
    });

    test('flat band is symmetric and inclusive', () {
      expect(candleToneFor(0.5), CandleTone.gold);
      expect(candleToneFor(-0.5), CandleTone.gold);
      expect(candleToneFor(0.51), CandleTone.green);
      expect(candleToneFor(-0.51, flatBand: -0.5), CandleTone.red);
    });

    test('non-finite pnl is break-even', () {
      expect(candleToneFor(double.nan), CandleTone.gold);
      expect(candleToneFor(double.infinity), CandleTone.gold);
      expect(candleToneFor(5, flatBand: double.nan), CandleTone.green);
    });
  });

  group('CandleController', () {
    test('auto follows today pnl, manual ignores it', () {
      final c = CandleController(todayPnl: -10);
      expect(c.tone, CandleTone.red);
      c.mode = CandleColorMode.manual;
      c.manual = CandleTone.sky;
      expect(c.tone, CandleTone.sky);
      c.todayPnl = 50;
      expect(c.tone, CandleTone.sky);
      c.mode = CandleColorMode.auto;
      expect(c.tone, CandleTone.green);
    });

    test('notifies only on real changes', () {
      final c = CandleController();
      var n = 0;
      c.addListener(() => n++);
      c.mode = CandleColorMode.auto;
      c.todayPnl = 0;
      expect(n, 0);
      c.todayPnl = 5;
      c.mode = CandleColorMode.manual;
      expect(n, 2);
    });
  });

  test('candle theme tokens are registered', () {
    final t = TxTokens.of(TxThemeId.candle);
    expect(t.bg, const Color(0xFF06080C));
    expect(t.gilded, isFalse);
    expect(TxTokens.of(TxThemeId.gold).gilded, isTrue);
    expect(TxTokens.of(TxThemeId.standard).gilded, isFalse);
  });

  testWidgets('nav bar reports taps on tabs and on the add button', (tester) async {
    final handle = tester.ensureSemantics();
    int? picked;
    var added = 0;
    await tester.pumpWidget(host(CandleNavBar(
      items: items,
      currentIndex: 0,
      onSelected: (i) => picked = i,
      onAdd: () => added++,
    )));
    await tester.pump(const Duration(milliseconds: 700));

    await tester.tap(find.text('آموزش'));
    expect(picked, 2);
    await tester.tap(find.bySemanticsLabel('ثبت معامله جدید'));
    expect(added, 1);
    handle.dispose();
  });

  testWidgets('hamburger toggles through its callback', (tester) async {
    final handle = tester.ensureSemantics();
    var taps = 0;
    await tester.pumpWidget(host(CandleHamburger(open: false, onPressed: () => taps++)));
    await tester.tap(find.bySemanticsLabel('منو'));
    expect(taps, 1);

    await tester.pumpWidget(host(CandleHamburger(open: true, onPressed: () => taps++)));
    await tester.pump(const Duration(milliseconds: 400));
    await tester.tap(find.bySemanticsLabel('منو'));
    expect(taps, 2);
    handle.dispose();
  });

  testWidgets('menu panel returns the tapped entry id', (tester) async {
    String? id;
    await tester.pumpWidget(host(SingleChildScrollView(
      child: CandleMenuPanel(
        entries: const [
          CandleMenuEntry(id: 'tx_ai', title: 'TX AI', subtitle: 'تحلیل', tone: CandleTone.sky, badge: 'جدید'),
          CandleMenuEntry(id: 'edu', title: 'آموزش', subtitle: 'درس‌ها', tone: CandleTone.green),
        ],
        onSelected: (v) => id = v,
      ),
    )));
    await tester.tap(find.text('آموزش'));
    expect(id, 'edu');
    expect(find.text('جدید'), findsOneWidget);
  });

  testWidgets('theme panel switches between auto and manual', (tester) async {
    final c = CandleController(todayPnl: 10);
    await tester.pumpWidget(host(CandleThemePanel(controller: c, onPreviewPnl: (v) => c.todayPnl = v)));

    expect(find.text('سبز'), findsNothing);
    await tester.tap(find.text('ضرر'));
    await tester.pump();
    expect(c.tone, CandleTone.red);

    await tester.tap(find.text('دستی'));
    await tester.pump();
    expect(find.text('آبی'), findsOneWidget);
    await tester.tap(find.text('آبی'));
    await tester.pump();
    expect(c.tone, CandleTone.sky);
  });
}
