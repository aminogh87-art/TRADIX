//! TRADENEX native core: session maths and trade statistics.
//! Exposed to Kotlin through JNI (see app/.../core/NativeCore.kt).
//! Every function has a pure-Kotlin fallback, so the app still runs without this library.

use jni::objects::{JDoubleArray, JObject};
use jni::sys::jint;
use jni::JNIEnv;

/// Session table in UTC minutes of the day: (start, end).
/// Index: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
const SESSIONS: [(i32, i32); 4] = [(1320, 420), (0, 540), (480, 1020), (780, 1320)];

fn is_open(minute: i32, start: i32, end: i32) -> bool {
    if start < end {
        minute >= start && minute < end
    } else {
        minute >= start || minute < end
    }
}

/// Bit i is set when session i is open.
pub fn session_mask(minute: i32) -> i32 {
    let m = minute.rem_euclid(1440);
    let mut mask = 0;
    for (i, (s, e)) in SESSIONS.iter().enumerate() {
        if is_open(m, *s, *e) {
            mask |= 1 << i;
        }
    }
    mask
}

/// Minutes during which both sessions are open in one day.
pub fn overlap_minutes(a: usize, b: usize) -> i32 {
    if a >= 4 || b >= 4 {
        return 0;
    }
    (0..1440)
        .filter(|m| {
            is_open(*m, SESSIONS[a].0, SESSIONS[a].1) && is_open(*m, SESSIONS[b].0, SESSIONS[b].1)
        })
        .count() as i32
}

/// Minutes until the given session flips between open and closed.
pub fn minutes_to_change(minute: i32, session: usize) -> i32 {
    if session >= 4 {
        return 0;
    }
    let (s, e) = SESSIONS[session];
    let now = minute.rem_euclid(1440);
    let open = is_open(now, s, e);
    for step in 1..=1440 {
        let m = (now + step) % 1440;
        if is_open(m, s, e) != open {
            return step;
        }
    }
    0
}

/// Deterministic performance metrics derived only from realized trade P&L.
/// Output: [net, win_rate, profit_factor, max_drawdown, expectancy, avg_win, avg_loss_abs,
/// recovery_factor, largest_win_share, largest_loss_share].
/// Ratios with undefined denominators are returned as 0; callers must display sample counts.
pub fn performance_metrics(pnls: &[f64]) -> [f64; 10] {
    if pnls.is_empty() {
        return [0.0; 10];
    }
    let mut net = 0.0;
    let mut gross_win = 0.0;
    let mut gross_loss = 0.0;
    let mut wins = 0usize;
    let mut win_sum = 0.0;
    let mut loss_sum = 0.0;
    let mut largest_win: f64 = 0.0;
    let mut largest_loss: f64 = 0.0;
    let mut equity = 0.0;
    let mut peak: f64 = 0.0;
    let mut max_dd: f64 = 0.0;

    for value in pnls.iter().copied().filter(|v| v.is_finite()) {
        net += value;
        if value > 0.0 {
            wins += 1;
            win_sum += value;
            gross_win += value;
            largest_win = largest_win.max(value);
        } else if value < 0.0 {
            loss_sum += value.abs();
            gross_loss += value.abs();
            largest_loss = largest_loss.max(value.abs());
        }
        equity += value;
        peak = peak.max(equity);
        max_dd = max_dd.min(equity - peak);
    }
    let count = pnls.iter().filter(|v| v.is_finite()).count();
    if count == 0 { return [0.0; 10]; }
    let pf = if gross_loss > 0.0 { gross_win / gross_loss } else if gross_win > 0.0 { 99.0 } else { 0.0 };
    let avg_win = if wins > 0 { win_sum / wins as f64 } else { 0.0 };
    let losses = count.saturating_sub(wins + pnls.iter().filter(|v| **v == 0.0).count());
    let avg_loss = if losses > 0 { loss_sum / losses as f64 } else { 0.0 };
    let recovery = if max_dd < 0.0 { net / max_dd.abs() } else { 0.0 };
    [
        net,
        wins as f64 / count as f64,
        pf,
        max_dd,
        net / count as f64,
        avg_win,
        avg_loss,
        recovery,
        if gross_win > 0.0 { largest_win / gross_win } else { 0.0 },
        if gross_loss > 0.0 { largest_loss / gross_loss } else { 0.0 },
    ]
}

/// C ABI for Dart FFI. Returns 0 on success, -1 for invalid pointers/length.
/// The caller owns both buffers and guarantees they are valid for the supplied lengths.
#[no_mangle]
pub unsafe extern "C" fn tx_ai_performance_metrics(
    pnls_ptr: *const f64,
    pnls_len: usize,
    output_ptr: *mut f64,
    output_len: usize,
) -> i32 {
    if output_ptr.is_null() || output_len < 10 || (pnls_len > 0 && pnls_ptr.is_null()) {
        return -1;
    }
    let pnls = if pnls_len == 0 {
        &[]
    } else {
        std::slice::from_raw_parts(pnls_ptr, pnls_len)
    };
    let metrics = performance_metrics(pnls);
    std::ptr::copy_nonoverlapping(metrics.as_ptr(), output_ptr, metrics.len());
    0
}

/// [net, win_rate (0..1), profit_factor, max_drawdown (<= 0)]
pub fn stats(pnls: &[f64]) -> [f64; 4] {
    if pnls.is_empty() {
        return [0.0, 0.0, 0.0, 0.0];
    }
    let mut net = 0.0;
    let mut gain = 0.0;
    let mut loss = 0.0;
    let mut wins = 0usize;
    let mut equity = 0.0;
    let mut peak = 0.0;
    let mut dd = 0.0;
    for p in pnls {
        net += p;
        if *p > 0.0 {
            gain += p;
            wins += 1;
        } else {
            loss += -p;
        }
        equity += p;
        if equity > peak {
            peak = equity;
        }
        if equity - peak < dd {
            dd = equity - peak;
        }
    }
    let pf = if loss > 0.0 {
        gain / loss
    } else if gain > 0.0 {
        99.0
    } else {
        0.0
    };
    [net, wins as f64 / pnls.len() as f64, pf, dd]
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nSessionMask<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
) -> jint {
    session_mask(minute)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nOverlapMinutes<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    a: jint,
    b: jint,
) -> jint {
    overlap_minutes(a.max(0) as usize, b.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nMinutesToChange<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
    session: jint,
) -> jint {
    minutes_to_change(minute, session.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nStats<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    pnls: JDoubleArray<'l>,
) -> JDoubleArray<'l> {
    let len = env.get_array_length(&pnls).unwrap_or(0) as usize;
    let mut buf = vec![0f64; len];
    let _ = env.get_double_array_region(&pnls, 0, &mut buf);
    let s = stats(&buf);
    match env.new_double_array(4) {
        Ok(out) => {
            let _ = env.set_double_array_region(&out, 0, &s);
            out
        }
        Err(_) => unsafe { JDoubleArray::from_raw(std::ptr::null_mut()) },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn london_ny_overlap_is_four_hours() {
        assert_eq!(overlap_minutes(2, 3), 240);
    }

    #[test]
    fn sydney_wraps_midnight() {
        assert_eq!(session_mask(23 * 60) & 1, 1);
        assert_eq!(session_mask(8 * 60) & 1, 0);
    }

    #[test]
    fn performance_metrics_are_deterministic_and_explainable() {
        let m = performance_metrics(&[100.0, -50.0, 150.0, -100.0]);
        assert_eq!(m[0], 100.0);
        assert_eq!(m[1], 0.5);
        assert!((m[2] - 250.0 / 150.0).abs() < 1e-9);
        assert_eq!(m[3], -100.0);
        assert_eq!(m[4], 25.0);
        assert_eq!(m[5], 125.0);
        assert_eq!(m[6], 75.0);
        assert_eq!(m[7], 1.0);
    }

    #[test]
    fn performance_metrics_ignore_non_finite_values() {
        let m = performance_metrics(&[10.0, f64::NAN, f64::INFINITY, -5.0]);
        assert_eq!(m[0], 5.0);
        assert_eq!(m[1], 0.5);
        assert_eq!(m[4], 2.5);
    }

    #[test]
    fn empty_performance_metrics_are_zero() {
        assert_eq!(performance_metrics(&[]), [0.0; 10]);
    }

    #[test]
    fn c_abi_returns_structured_metrics() {
        let input = [10.0, -4.0, 6.0];
        let mut output = [0.0; 10];
        let status = unsafe {
            tx_ai_performance_metrics(input.as_ptr(), input.len(), output.as_mut_ptr(), output.len())
        };
        assert_eq!(status, 0);
        assert_eq!(output[0], 12.0);
        assert!((output[1] - 2.0 / 3.0).abs() < 1e-9);
    }

    #[test]
    fn c_abi_rejects_invalid_output_buffer() {
        let input = [10.0];
        let mut output = [0.0; 4];
        let status = unsafe {
            tx_ai_performance_metrics(input.as_ptr(), input.len(), output.as_mut_ptr(), output.len())
        };
        assert_eq!(status, -1);
    }

    #[test]
    fn stats_basic() {
        let s = stats(&[100.0, -50.0, 150.0, -100.0]);
        assert_eq!(s[0], 100.0);
        assert_eq!(s[1], 0.5);
        assert!((s[2] - 250.0 / 150.0).abs() < 1e-9);
        assert_eq!(s[3], -100.0);
    }
}
