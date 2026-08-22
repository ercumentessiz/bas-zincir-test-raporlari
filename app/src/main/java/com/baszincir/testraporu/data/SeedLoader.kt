package com.baszincir.testraporu.data

import android.content.Context
import com.baszincir.testraporu.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Uygulama içine gömülü örnek ürün/müşteri verilerini (res/raw) okuyup Firestore'a yazar.
 * Node.js veya harici bir araca gerek kalmadan, uygulamayı ilk açtığınızda tek dokunuşla
 * 18 ürünü ve müşteri listesini veritabanına yükleyebilmeniz için eklendi.
 */
object SeedLoader {

    fun urunleriOku(context: Context): List<Product> {
        context.resources.openRawResource(R.raw.products_seed).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                val tip = object : TypeToken<List<Product>>() {}.type
                return Gson().fromJson(reader, tip)
            }
        }
    }

    fun musterileriOku(context: Context): List<Customer> {
        context.resources.openRawResource(R.raw.customers_seed).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                val tip = object : TypeToken<List<Customer>>() {}.type
                val ham: List<Customer> = Gson().fromJson(reader, tip)
                // customers_seed.json'da id alanı yok; FirestoreRepository.musteriKaydet
                // id boşsa otomatik üretiyor, burada da aynı mantığı kullanmak için
                // isim bazlı basit bir id türetiyoruz (tekrar yüklemede kopya oluşmasın diye).
                return ham.map { it.copy(id = slugify(it.ad)) }
            }
        }
    }

    private fun slugify(s: String): String {
        val trMap = mapOf('ç' to 'c', 'ğ' to 'g', 'ı' to 'i', 'ö' to 'o', 'ş' to 's', 'ü' to 'u')
        val kucuk = s.lowercase().map { trMap[it] ?: it }.joinToString("")
        return kucuk.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(120)
    }
}
