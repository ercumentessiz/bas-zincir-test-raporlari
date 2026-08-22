package com.baszincir.testraporu.data

/** Zincir ölçüleri tablosundaki tek bir satır (d, t, b1, b2 gibi). */
data class OlcuSatiri(
    val urun: Double = 0.0,
    val numune: List<Double> = emptyList()
)

data class ZincirOlculeri(
    val d: OlcuSatiri = OlcuSatiri(),
    val t: OlcuSatiri = OlcuSatiri(),
    val b1: OlcuSatiri = OlcuSatiri(),
    val b2: OlcuSatiri = OlcuSatiri()
)

/**
 * Alt tablodaki (Kopma Yükü / Sertlik) tek bir satırı temsil eder.
 * Firestore diziler içinde doğrudan başka bir dizi barındırmayı desteklemediği için
 * (nested arrays not supported), her satırı bu obje ile "map" içine sarıyoruz:
 * List<List<Double>> yerine List<SayiSatiri> kullanılıyor.
 */
data class SayiSatiri(
    var degerler: List<Double> = emptyList()
)

/** Genel amaçlı, hücreleri metin olan bir tablo satırı (örn. Kimyasal Analiz). */
data class MetinSatiri(
    var hucreler: List<String> = emptyList()
)

/**
 * Bir ürün (zincir) tipini ve o ürüne ait, HİÇ DOKUNULMAYACAK sabit teknik verileri tutar.
 * (Zincir ölçüleri, kimyasal analiz — kullanıcı isteğine göre bu alanlara raporda dokunulmuyor,
 * ama admin ekranından düzeltme/ekleme yapılabilsin diye Firestore'da düzenlenebilir tutuyoruz.)
 */
data class Product(
    var id: String = "",
    var urunAdi: String = "",                 // Dropdown'da görünen isim: "8 mm G-80 Zincir"
    var kategori: String = "G80",              // "G80" | "KALIBRE" | "CEKI"  -> gruplama ve açıklama metni için
    var parcaAdiSatir1: String = "",           // PDF'teki "PARÇA ADI" 1. satır
    var parcaAdiSatir2: String = "",           // PDF'teki "PARÇA ADI" 2. satır
    var zincirOlculeri: ZincirOlculeri = ZincirOlculeri(),
    var ilkBoyu: Double = 0.0,
    var sonBoyu: Double = 0.0,
    var denemeYuku: Double = 0.0,
    var kopmaYukuTekli: Double = 0.0,          // SON BOYU satırındaki tekil "KOPMA YÜKÜ (kN)" değeri — sabit, hiç değişmez
    var malzeme: String = "",
    var standart: String = "",
    var kimyasalAnalizTablosu: List<MetinSatiri> = emptyList(),
    // Kopma yükü taban değerleri: her rapor üretiminde ±0.05/±0.10 aralığında hafifçe oynatılır.
    var kopmaYukuTablosu: List<SayiSatiri> = emptyList(),
    // Sadece G-80 ürünlerinde dolu olur, hiç değiştirilmez (sabit).
    var sertlikTablosu: List<SayiSatiri> = emptyList(),
    var sertlikBirimi: String = "Hv / HRC",
    var kopmaBirimi: String = "Ton/kn",
    var aciklamaEk: String = ""                // Kullanıcının açıklamanın altına ekleyebileceği serbest metin (varsayılan boş)
) {
    companion object {
        const val KATEGORI_G80 = "G80"
        const val KATEGORI_KALIBRE = "KALIBRE"
        const val KATEGORI_CEKI = "CEKI"

        // Sertlik değeri olan ve G-80 tipi açıklama metni kullanılan kategoriler.
        val G80_ACIKLAMA_KATEGORILERI = setOf(KATEGORI_G80, KATEGORI_CEKI)
    }
}
