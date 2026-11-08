package com.tradenex.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tradenex.app.MainActivity
import com.tradenex.app.R
import org.json.JSONArray
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyReviewWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("tradix_weekly_planner", Context.MODE_PRIVATE)
        val settings = applicationContext.getSharedPreferences("tradix_local_settings", Context.MODE_PRIVATE)
        val reminderOn = settings.getBoolean("reminderOn", true)
        val hour = settings.getInt("reminderHour", 22).coerceIn(0, 23)
        val today = LocalDate.now()
        if (reminderOn && today.dayOfWeek.value in 1..5) {
            val raw = prefs.getString("plans", null)
            val plan = runCatching {
                val rows = JSONArray(raw ?: "[]")
                (0 until rows.length()).map { rows.getJSONObject(it) }.firstOrNull { it.optString("date") == today.toString() }
            }.getOrNull()
            val count = plan?.optInt("tradeCount", 0) ?: 0
            val reason = plan?.optString("noTradeReason").orEmpty()
            val confirmed = plan?.optBoolean("confirmed", false) ?: false
            if (count == 0 && (!confirmed || reason.isBlank())) notifyReview()
        }
        ReminderScheduler.schedule(applicationContext)
        return Result.success()
    }

    private fun notifyReview() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(ReminderScheduler.CHANNEL, "مرور روزانه معاملات", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val intent = PendingIntent.getActivity(applicationContext, 1201, Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("امروز معامله‌ای نداشتی؟")
            .setContentText("وارد TRADIX شو و دلیل نداشتن معامله را در برنامه هفتگی ثبت کن.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("اگر بازار ستاپ مناسبی نداشت یا تصمیم گرفتی معامله نکنی، دلیلش را ثبت کن تا گزارش هفتگی دقیق‌تری داشته باشی."))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        manager.notify(todayNotificationId(), notification)
    }

    private fun todayNotificationId() = LocalDate.now().dayOfYear + 7000
}

object ReminderScheduler {
    const val CHANNEL = "tradix_daily_review"

    fun schedule(context: Context, reminderOn: Boolean? = null, reminderHour: Int? = null) {
        val manager = WorkManager.getInstance(context)
        val settings = context.getSharedPreferences("tradix_local_settings", Context.MODE_PRIVATE)
        if (reminderOn != null || reminderHour != null) {
            settings.edit()
                .putBoolean("reminderOn", reminderOn ?: settings.getBoolean("reminderOn", true))
                .putInt("reminderHour", (reminderHour ?: settings.getInt("reminderHour", 22)).coerceIn(0, 23))
                .apply()
        }
        if (!settings.getBoolean("reminderOn", true)) {
            manager.cancelUniqueWork("tradix_daily_review")
            return
        }
        val hour = settings.getInt("reminderHour", 22).coerceIn(0, 23)
        val now = LocalTime.now()
        var target = LocalDate.now().atTime(hour, 0)
        if (!target.isAfter(java.time.LocalDateTime.now())) target = target.plusDays(1)
        val delay = Duration.between(java.time.LocalDateTime.now(), target).toMillis().coerceAtLeast(60_000)
        val request = OneTimeWorkRequestBuilder<DailyReviewWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        manager.enqueueUniqueWork("tradix_daily_review", ExistingWorkPolicy.REPLACE, request)
    }
}
