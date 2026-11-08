package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.TxAiEngine
import com.tradenex.app.data.TxAiSlice
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.theme.Tx

@Composable
fun TxAiScreen(nav: NavController) {
    val report = TxAiEngine.analyze(AppState.visibleTrades.toList(), AppState.selectedAccount, AppState.settings.riskPercent)
    fun money(v: Double) = Analytics.money(v)
    Page(title = "TX AI", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("شناخت شخصیت معاملاتی", color = Tx.Gold, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("تحلیل بر پایه معاملات واقعی حساب «" + AppState.selectedAccount.name + "»؛ این بخش سیگنال خرید یا فروش صادر نمی‌کند.", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("سود/زیان خالص", money(report.net), if (report.net >= 0) Tx.Green else Tx.Red, Modifier.weight(1f))
                        StatTile("نرخ برد", (report.winRate * 100).toInt().toString() + "%", Tx.Gold, Modifier.weight(1f))
                        StatTile("معاملات", report.totalTrades.toString(), Tx.Blue, Modifier.weight(1f))
                    }
                    Text("پروفایل رفتاری فعلی: " + report.personality, color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(report.bestSession?.let { "سشن برتر فعلی: " + it.label + " · " + it.trades + " معامله" } ?: "برای تشخیص سشن برتر، زمان بازشدن معامله لازم است.", color = Tx.Text, fontSize = 12.sp)
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("الگوی عملکرد و سشن‌ها")
                    Text("سشن‌ها از زمان بازشدن معامله و ساعت محلی بازارها استخراج شده‌اند.", color = Tx.Muted, fontSize = 10.sp)
                    if (report.sessions.isEmpty()) EmptyAi("زمان بازشدن معاملات ثبت نشده است.")
                    report.sessions.forEach { AiSliceRow(it, money(it.net)) }
                    Text("بهترین تایم‌فریم‌ها", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.timeframes.isEmpty()) EmptyAi("از این نسخه به بعد تایم‌فریم هنگام ثبت معامله ذخیره می‌شود.")
                    report.timeframes.take(5).forEach { AiSliceRow(it, money(it.net)) }
                    Text("شرایط بازار", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.marketConditions.isEmpty()) EmptyAi("هنوز شرایط بازار برای معامله‌ها ثبت نشده است.")
                    report.marketConditions.forEach { AiSliceRow(it, money(it.net)) }
                    report.bestStrategy?.let { Text("استراتژی شاخص: " + it.label, color = Tx.Gold, fontSize = 12.sp) }
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("تحلیل روزانه و هفتگی")
                    Text("روزهای معاملاتی اخیر", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    report.days.takeLast(14).forEach { AiSliceRow(it, money(it.net)) }
                    Text("الگوی عملکرد در روزهای هفته", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.weekdays.isEmpty()) EmptyAi("داده روزانه کافی نیست.")
                    report.weekdays.forEach { AiSliceRow(it, money(it.net)) }
                    Text("هفته‌های اخیر · شروع هفته دوشنبه", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.weeks.isEmpty()) EmptyAi("داده هفتگی کافی نیست.")
                    report.weeks.takeLast(8).forEach { AiSliceRow(it, money(it.net)) }
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("شبیه‌سازی تصمیم‌های جایگزین")
                    Text("اگر معاملات دارای نشانه خارج از پلن حذف می‌شدند", color = Tx.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("تعداد", report.outsideStrategyCount.toString(), Tx.Red, Modifier.weight(1f))
                        StatTile("سود/زیان آن‌ها", money(report.outsideStrategyNet), if (report.outsideStrategyNet >= 0) Tx.Green else Tx.Red, Modifier.weight(1f))
                    }
                    Text("اگر ریسک همه معاملات نصف می‌شد (با فرض ثابت‌ماندن نسبت سود و زیان)", color = Tx.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("واقعی", money(report.allNet), Tx.Text, Modifier.weight(1f))
                        StatTile("ریسک ۵۰٪", money(report.halfRiskNet), Tx.Gold, Modifier.weight(1f))
                    }
                    Text("سود/زیان کل اگر معاملات خارج از پلن حذف می‌شد: " + money(report.withoutOutsideStrategyNet), color = if (report.withoutOutsideStrategyNet >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("تغییر سناریوی ریسک نصف: " + money(report.halfRiskDelta), color = if (report.halfRiskDelta >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("این شبیه‌سازی بک‌تست نیست؛ اسلیپیج، مارجین و تغییر رفتار اجرای سفارش را مدل نمی‌کند. تشخیص خارج از پلن بر اساس تگ‌های رفتاری یا خالی‌بودن نام استراتژی است.", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
        Reveal(4) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("پیشرفت ماهانه · " + report.scores.monthLabel)
                    Text("امتیاز ۰ تا ۱۰۰؛ خط تیره یعنی داده کافی موجود نیست.", color = Tx.Muted, fontSize = 10.sp)
                    ScoreRow("نظم معاملاتی", report.scores.discipline, "نبود تگ‌های FOMO، Revenge و Overtrading")
                    ScoreRow("مدیریت ریسک", report.scores.risk, "نیازمند ثبت ریسک برنامه‌ریزی‌شده")
                    ScoreRow("پایبندی به استراتژی", report.scores.strategy, "بر اساس ثبت نام استراتژی؛ نه اثبات اجرای قوانین")
                    ScoreRow("کیفیت تصمیم‌گیری", report.scores.decision, "کامل‌بودن ورود، خروج، حدضرر و استراتژی")
                    Text("نمونه این ماه: " + report.scores.sampleSize + " معامله", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
        Reveal(5) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("پیشنهادهای TX AI")
                    report.insights.forEachIndexed { i, insight -> Text((i + 1).toString() + ". " + insight, color = Tx.Text, fontSize = 11.sp) }
                    Text("تایم‌فریم و شرایط بازار از معاملات جدیدی که این فیلدها در آن‌ها تکمیل شده استخراج می‌شوند؛ معاملات قدیمی فاقد این اطلاعات‌اند.", color = Tx.Gold, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun AiSliceRow(slice: TxAiSlice, net: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(slice.label, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(slice.trades.toString() + " معامله · برد " + (slice.winRate * 100).toInt() + "% · میانگین " + String.format(java.util.Locale.US, "%.2f", slice.average), color = Tx.Muted, fontSize = 10.sp)
        }
        Text(net, color = if (slice.net >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreRow(label: String, score: Int?, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Tx.Text, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(score?.toString() ?: "—", color = score?.let { if (it >= 70) Tx.Green else Tx.Gold } ?: Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
            if (score != null) Box(Modifier.fillMaxWidth(score / 100f).height(6.dp).background(Tx.Gold, RoundedCornerShape(4.dp)))
        }
        Text(detail, color = Tx.Muted, fontSize = 9.sp)
    }
}

@Composable
private fun EmptyAi(text: String) { Text(text, color = Tx.Muted, fontSize = 11.sp) }
