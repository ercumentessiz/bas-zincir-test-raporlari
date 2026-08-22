package com.baszincir.testraporu.util

import com.baszincir.testraporu.data.SayiSatiri
import kotlin.math.round
import kotlin.random.Random

/**
 * Ürünün sabit kopma yükü taban değerlerini alır ve her satır/sütun için
 * ±0.05 ile ±0.10 arasında (kullanıcının belirttiği aralıkta) küçük, rastgele
 * bir sapma uygulayarak yeni bir tablo döndürür. Büyük sıçramalar asla olmaz.
 */
object KopmaYukuRastgeleleyici {

    fun uygula(taban: List<SayiSatiri>, seed: Long? = null): List<SayiSatiri> {
        val rnd = if (seed != null) Random(seed) else Random.Default
        return taban.map { satir ->
            SayiSatiri(satir.degerler.map { deger -> sapmaUygula(deger, rnd) })
        }
    }

    private fun sapmaUygula(deger: Double, rnd: Random): Double {
        // 0.05 - 0.10 aralığında mutlak değerde bir sapma miktarı seç, işaretini (+/-) rastgele belirle
        val sapmaBuyuklugu = 0.05 + rnd.nextDouble() * 0.05 // [0.05, 0.10)
        val isaret = if (rnd.nextBoolean()) 1.0 else -1.0
        val yeni = deger + (sapmaBuyuklugu * isaret)
        // iki ondalık basamağa yuvarla (kaynaktaki değerlerle aynı hassasiyet)
        return round(yeni * 100.0) / 100.0
    }
}
