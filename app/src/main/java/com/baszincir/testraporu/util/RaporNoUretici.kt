package com.baszincir.testraporu.util

import java.util.Calendar

/**
 * Rapor No formatı: [Yıl(4)] + [000 sabit] + [Ay(2) Gün(2)]
 * Örn: 21.04.2026 -> 20260000421   (2026 + 000 + 0421)
 */
object RaporNoUretici {
    fun uret(tarihMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = tarihMillis }
        val yil = cal.get(Calendar.YEAR)
        val ay = cal.get(Calendar.MONTH) + 1
        val gun = cal.get(Calendar.DAY_OF_MONTH)
        val ayGun = String.format("%02d%02d", ay, gun)
        return "$yil" + "000" + ayGun
    }

    const val PARTI_NO = "000.1"
}
