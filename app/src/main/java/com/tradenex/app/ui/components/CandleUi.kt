package com.tradenex.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.Tx

/*
 * The "Candle" theme in Compose. Colour is the market direction, shape is the candle structure.
 * The Dart twin of everything here lives in flutter_fx/lib/candle_fx.dart; keep both in sync.
 */

/** Tone index (0 green, 1 red, 2 gold, 3 sky blue) as a colour of the active palette. */
fun candleToneColor(tone: Int): Color = when (tone) {
    0 -> Tx.Green
    1 -> Tx.Red
    2 -> Tx.Gold
    else -> Tx.Blue
}

/** Open / closed state of the candle menu (opened by [CandleHamburger]). */
object CandleMenu {
    var open: Boolean by mutableStateOf(false)

    fun toggle() { open = !open }

    fun close() { open = false }
}

private fun DrawScope.candleBar(cx: Float, cy: Float, w: Float, h: Float, degrees: Float, color: Color) {
    rotate(degrees, Offset(cx, cy)) {
        drawRoundRect(color, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), CornerRadius(h / 2f))
    }
}

/** Menu button that is itself a candle: wick, body, wick. Opening turns the wicks into a green / red cross. */
@Composable
fun CandleHamburger(modifier: Modifier = Modifier) {
    val open = CandleMenu.open
    val p by animateFloatAsState(if (open) 1f else 0f, tween(320), label = "hamburger")
    val tone by animateColorAsState(candleToneColor(ThemeController.candleTone), tween(600), label = "hamburgerTone")
    val tile = RoundedCornerShape(16.dp)
    val wickGold = Tx.Gold
    val crossGreen = Tx.Green
    val crossRed = Tx.Red
    Box(
        modifier
            .size(44.dp)
            .clip(tile)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, lerp(tone, crossRed, p).copy(alpha = 0.5f), tile)
            .semantics { contentDescription = if (open) "بستن منو" else "منو" }
            .clickable(role = Role.Button) { CandleMenu.toggle() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val c = center
            if (p < 1f) candleBar(c.x, c.y, 18.dp.toPx(), 7.dp.toPx(), 0f, tone.copy(alpha = 1f - p))
            val dy = 8.dp.toPx() * (1f - p)
            val h = 2.4.dp.toPx()
            candleBar(c.x, c.y - dy, lerpF(8.dp.toPx(), 22.dp.toPx(), p), h, 45f * p, lerp(wickGold, crossGreen, p))
            candleBar(c.x, c.y + dy, lerpF(12.dp.toPx(), 22.dp.toPx(), p), h, -45f * p, lerp(wickGold, crossRed, p))
        }
    }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

private val moreRoutes = setOf(
    "more", "sessions", "strategies", "calendar", "settings", "tags", "accounts", "trades",
    "weekly", "weekly_reminder", "tx_ai", "tx_ai_legacy",
)

/**
 * Horizontal glass candle used as the bottom navigation of the Candle theme.
 * Gold wick with end caps across the full width, glass body tinted by the current tone with a coloured shadow,
 * and a gold doji in the middle that adds a trade. Routes match the classic bar.
 */
@Composable
fun CandleNavBar(current: String, onNavigate: (String) -> Unit) {
    val tone by animateColorAsState(candleToneColor(ThemeController.candleTone), tween(600), label = "candleNavTone")
    val highMotion = AppState.settings.animations >= 2
    val t = rememberInfiniteTransition(label = "candleNav")
    val sweep by t.animateFloat(
        initialValue = 0f,
        targetValue = if (highMotion) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "sweep",
    )
    val pulse by t.animateFloat(
        initialValue = 1f,
        targetValue = if (highMotion) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "dojiPulse",
    )
    val glow = Tx.glowLevel
    val wick = Tx.Gold
    val body = RoundedCornerShape(32.dp)
    val doji = RoundedCornerShape(20.dp)

    Box(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(92.dp),
    ) {
        // gold wick across the whole bar, end caps like a candle's high and low
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2f
            drawLine(
                Brush.horizontalGradient(listOf(wick, wick.copy(alpha = 0.15f), wick.copy(alpha = 0.15f), wick)),
                Offset(1.5f, y), Offset(size.width - 1.5f, y), 2.dp.toPx(), StrokeCap.Round,
            )
            val cap = 13.dp.toPx()
            drawLine(wick, Offset(1.5f, y - cap), Offset(1.5f, y + cap), 3.dp.toPx(), StrokeCap.Round)
            drawLine(wick, Offset(size.width - 1.5f, y - cap), Offset(size.width - 1.5f, y + cap), 3.dp.toPx(), StrokeCap.Round)
        }
        // coloured shadow under the body
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .height(36.dp)
                .drawBehind {
                    drawOval(
                        Brush.radialGradient(listOf(tone.copy(alpha = (0.6f * glow).coerceIn(0f, 0.9f)), Color.Transparent)),
                        Offset.Zero,
                        size,
                    )
                },
        )
        Row(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .height(80.dp)
                .clip(body)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), tone.copy(alpha = 0.10f), tone.copy(alpha = 0.22f))))
                .border(1.dp, tone.copy(alpha = 0.55f), body)
                .drawWithContent {
                    drawContent()
                    if (highMotion) {
                        val x = -size.width * 0.3f + size.width * 1.6f * sweep
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                                startX = x,
                                endX = x + size.width * 0.3f,
                            ),
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            CandleTab("خانه", Icons.Rounded.Home, current == "home", tone) { onNavigate("home") }
            CandleTab("گزارش‌ها", Icons.Rounded.BarChart, current == "perf", tone) { onNavigate("perf") }
            Box(
                Modifier
                    .scale(pulse)
                    .size(width = 60.dp, height = 48.dp)
                    .shadow(14.dp, doji, ambientColor = Tx.Gold, spotColor = Tx.Gold)
                    .clip(doji)
                    .background(Brush.linearGradient(listOf(Tx.GoldHi, Color(0xFFF2B43C), Tx.GoldDeep)))
                    .clickable(role = Role.Button) { onNavigate("add") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "ثبت معامله جدید", tint = Tx.FabIcon, modifier = Modifier.size(28.dp))
            }
            CandleTab("آموزش", Icons.Rounded.MenuBook, current == "edu", tone) { onNavigate("edu") }
            CandleTab("بیشتر", Icons.Rounded.GridView, current in moreRoutes, tone) { onNavigate("more") }
        }
    }
}

@Composable
private fun CandleTab(label: String, icon: ImageVector, selected: Boolean, tone: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val fg by animateColorAsState(if (selected) tone else Tx.Muted, tween(250), label = "tabFg")
    Column(
        Modifier
            .size(width = 56.dp, height = 58.dp)
            .clip(shape)
            .background(if (selected) tone.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) tone.copy(alpha = 0.55f) else Color.Transparent, shape)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** A destination in the candle menu; [top] / [height] are the candle body's position and size (0..1). */
private class CandleMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val tone: Int,
    val top: Float,
    val height: Float,
    val badge: String? = null,
)

private val menuItems = listOf(
    CandleMenuItem("tx_ai", "TX AI", "تحلیل توضیح‌پذیر معاملات", 3, 0.22f, 0.50f, "جدید"),
    CandleMenuItem("weekly", "برنامه هفتگی", "انضباط و مرور روزانه", 2, 0.32f, 0.36f),
    CandleMenuItem("edu", "آموزش", "درس‌ها و اشتراک با QR", 0, 0.15f, 0.62f),
    CandleMenuItem("calendar", "تقویم", "معاملات شخصی و اخبار", 3, 0.42f, 0.32f),
    CandleMenuItem("strategies", "استراتژی‌ها", "نسخه‌ها و مقایسه", 2, 0.18f, 0.30f),
    CandleMenuItem("tags", "برچسب‌ها", "احساس، اشتباه و الگو", 1, 0.34f, 0.42f),
    CandleMenuItem("sessions", "سشن‌ها", "ساعت بازارها و هم‌پوشانی", 3, 0.26f, 0.40f),
    CandleMenuItem("trades", "معاملات", "تاریخچه، فیلتر و CSV", 0, 0.30f, 0.34f),
    CandleMenuItem("accounts", "حساب‌ها", "واقعی، دمو و پراپ", 2, 0.38f, 0.28f),
    CandleMenuItem("calculator", "ماشین‌حساب", "حجم، پیپ و ریسک", 1, 0.20f, 0.46f),
    CandleMenuItem("settings", "تنظیمات", "تم‌ها، آیکون، اعلان و داده", 0, 0.30f, 0.30f),
)

/** Scrim + glass panel opened by [CandleHamburger]. Place it last inside the screen's root Box. */
@Composable
fun CandleMenuOverlay(onNavigate: (String) -> Unit) {
    BackHandler(enabled = CandleMenu.open) { CandleMenu.close() }
    AnimatedVisibility(visible = CandleMenu.open, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { CandleMenu.close() },
        )
    }
    val shape = RoundedCornerShape(28.dp)
    AnimatedVisibility(
        visible = CandleMenu.open,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 6 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 8 },
        modifier = Modifier.statusBarsPadding().padding(top = 64.dp, start = 14.dp, end = 14.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF151B24), Color(0xFF0B0F15))))
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            menuItems.forEach { item ->
                CandleMenuRow(item) {
                    CandleMenu.close()
                    onNavigate(item.route)
                }
            }
            Text(
                "TRADIX سیگنال نمی‌دهد و معامله انجام نمی‌دهد.",
                color = Tx.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CandleMenuRow(item: CandleMenuItem, onClick: () -> Unit) {
    val color = candleToneColor(item.tone)
    val tile = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(tile)
                .background(color.copy(alpha = 0.14f))
                .border(1.dp, color.copy(alpha = 0.4f), tile),
            contentAlignment = Alignment.Center,
        ) {
            CandleGlyph(color, item.top, item.height)
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Tx.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(item.subtitle, color = Tx.Muted, fontSize = 12.sp)
        }
        if (item.badge != null) {
            val badgeShape = RoundedCornerShape(8.dp)
            Text(
                item.badge,
                color = Tx.Blue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(badgeShape)
                    .background(Tx.Blue.copy(alpha = 0.14f))
                    .border(1.dp, Tx.Blue.copy(alpha = 0.4f), badgeShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** One candle glyph: gold wick plus a rounded body at [top] with [height] (fractions of the glyph). */
@Composable
fun CandleGlyph(color: Color, top: Float, height: Float, modifier: Modifier = Modifier) {
    val wick = Tx.Gold
    Canvas(modifier.size(width = 20.dp, height = 28.dp)) {
        val cx = size.width / 2f
        drawLine(wick, Offset(cx, 1.dp.toPx()), Offset(cx, size.height - 1.dp.toPx()), 1.6.dp.toPx(), StrokeCap.Round)
        val bodyW = size.width * 0.7f
        drawRoundRect(
            color,
            Offset(cx - bodyW / 2f, size.height * top),
            Size(bodyW, size.height * height),
            CornerRadius(bodyW * 0.3f),
        )
    }
}
