package com.baszincir.testraporu.data

/**
 * Oluşturulmuş bir test raporunun geçmiş kaydı. PDF'in kendisi telefonda saklanmaz
 * (paylaşım/indirme anında geçici olarak üretilir) — burada raporun tüm girdileri
 * saklanır ki "Geçmiş Raporlar" ekranından istendiğinde PDF yeniden üretilebilsin.
 *
 * kopmaYukuTablosu: o rapor üretildiği anda rastgele oluşturulan kopma yükü değerleri
 * SABİT olarak burada saklanır — geçmişe dönüp tekrar açıldığında değerler değişmesin diye
 * (ürünün taban değerlerinden her seferinde yeniden rastgele üretilmez).
 */
data class Rapor(
    var id: String = "",
    var urunId: String = "",
    var urunAdi: String = "",
    var firmaAdi: String = "",
    var tarihMillis: Long = 0L,
    var raporNo: String = "",
    var miktarDeger: String = "",
    var miktarBirim: String = "",
    var adet: String = "",
    var ekAciklama: String = "",
    var kopmaYukuTablosu: List<SayiSatiri> = emptyList(),
    var olusturulmaMillis: Long = 0L
)
