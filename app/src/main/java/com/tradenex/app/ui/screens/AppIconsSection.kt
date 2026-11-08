package com.tradenex.app.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.R
import com.tradenex.app.ui.theme.Tx

/** One launcher icon the user can pick. [id] matches the manifest alias `.icon.Alias_<id>`. */
data class AppIcon(val id: String, val title: String, @DrawableRes val preview: Int)

object AppIcons {
    const val DEFAULT_ID = "bull"

    val all = listOf(
        AppIcon("bull", "گاو طلایی (اصلی)", R.drawable.icon_img_bull),
        AppIcon("bullbear", "نبرد گاو و خرس", R.drawable.icon_img_bullbear),
        AppIcon("stone", "کندل سنگی", R.drawable.icon_img_stone),
        AppIcon("gold_t", "تی طلایی", R.drawable.icon_img_gold_t),
        AppIcon("twin_t", "تی دوقطبی", R.drawable.icon_img_twin_t),
        AppIcon("orbit_t", "مدار طلایی", R.drawable.icon_img_orbit_t),
        AppIcon("tehran_flag", "تهران و پرچم", R.drawable.icon_img_tehran_flag),
        AppIcon("tehran_gold", "تهران طلایی", R.drawable.icon_img_tehran_gold),
        AppIcon("milad", "برج میلاد", R.drawable.icon_img_milad),
        AppIcon("nex_orbit", "نکس مداری", R.drawable.icon_img_nex_orbit),
        AppIcon("nex_globe", "نکس جهانی", R.drawable.icon_img_nex_globe),
        AppIcon("nex_star", "نکس ستاره‌ای", R.drawable.icon_img_nex_star),
    )

    private fun component(context: Context, id: String) = ComponentName(context.packageName, "com.tradenex.app.icon.Alias_$id")

    /** Reads the real state from the system; only the default alias counts as enabled when nothing was changed yet. */
    fun current(context: Context): String {
        val pm = context.packageManager
        val enabled = all.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon.id))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon.id == DEFAULT_ID
                else -> false
            }
        }
        return enabled?.id ?: DEFAULT_ID
    }

    /** Enables the new alias first, then disables the others, so the launcher never loses the app. */
    fun apply(context: Context, id: String) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, id), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        all.filter { it.id != id }.forEach {
            pm.setComponentEnabledSetting(component(context, it.id), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}

/** Settings group "آیکون‌ها": preview grid, tap to switch the launcher icon. */
@Composable
fun AppIconsSection() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppIcons.current(context)) }
    SettingGroup("آیکون‌ها") {
        Text(
            "آیکون برنامه را عوض کن. بسته به لانچر گوشی، تغییر آیکون روی صفحه‌ی اصلی چند ثانیه طول می‌کشد و ممکن است برنامه یک بار بسته شود.",
            color = Tx.Muted, fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        AppIcons.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val on = icon.id == selected
                    val shape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier.weight(1f).clickable {
                            if (!on) {
                                runCatching { AppIcons.apply(context, icon.id) }
                                    .onSuccess {
                                        selected = icon.id
                                        Toast.makeText(context, "آیکون «${icon.title}» فعال شد", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure { Toast.makeText(context, "تغییر آیکون ممکن نشد", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(androidx.compose.ui.graphics.Color.Black)
                                .border(if (on) 2.dp else 1.dp, if (on) Tx.Gold else Tx.Muted.copy(alpha = 0.3f), shape),
                        ) {
                            Image(painterResource(icon.preview), icon.title, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        }
                        Text(
                            icon.title, color = if (on) Tx.Gold else Tx.Text, fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                        )
                        if (on) Text("فعال", color = Tx.Green, fontSize = 10.sp)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
