package com.baszincir.testraporu.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.baszincir.testraporu.data.Product
import com.baszincir.testraporu.data.SayiSatiri
import com.baszincir.testraporu.util.KopmaYukuRastgeleleyici
import com.baszincir.testraporu.util.RaporNoUretici
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class RaporGirdisi(
    val product: Product,
    val firmaAdi: String,
    val tarihMillis: Long,
    val miktarDeger: String,   // örn. "900"
    val miktarBirim: String,   // "Mt." veya "Kg."
    val adet: String? = null,  // opsiyonel
    val ekAciklama: String = "",
    // Geçmiş bir raporu yeniden açarken, o an kaydedilmiş kopma yükü değerleri buradan verilir;
    // null ise (yeni rapor oluştururken) taban değerlerden rastgele üretilir.
    val sabitKopmaYuku: List<SayiSatiri>? = null
)

/**
 * A4 sayfa (595 x 842 pt) üzerine, orijinal "TEST VE ÖLÇÜ KONTROL RAPORU" şablonunu
 * çerçeveli/kutulu tablolarla birebire yakın şekilde yeniden çizer.
 */
object PdfGenerator {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 34f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))

    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
        isAntiAlias = true
    }

    fun olustur(context: Context, girdi: RaporGirdisi): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val p = girdi.product

        var y = 26f
        val left = MARGIN
        val right = MARGIN + CONTENT_W

        // ---- ANTET ----
        val logoResId = context.resources.getIdentifier("antet_logo", "drawable", context.packageName)
        if (logoResId != 0) {
            val bmp = BitmapFactory.decodeResource(context.resources, logoResId)
            val destH = CONTENT_W * (bmp.height.toFloat() / bmp.width.toFloat())
            canvas.drawBitmap(bmp, null, RectF(left, y, right, y + destH), null)
            y += destH + 8f
        } else {
            y += 8f
        }

        val tarihStr = dateFmt.format(girdi.tarihMillis)
        val raporNo = RaporNoUretici.uret(girdi.tarihMillis)

        // ---- Ortak sütun ölçüleri (başlık/bilgi/ilk boyu satırları bu değerlerle hizalanacak) ----
        val col1W = 130f
        val col3W = 160f
        val col2W = CONTENT_W - col1W - col3W

        // ---- Başlık satırı: [TEST VE ÖLÇÜ KONTROL RAPORU] [TARİH] ----
        val baslikH = 24f
        val tarihColW = col3W
        drawCellBorder(canvas, left, y, right - tarihColW, y + baslikH)
        drawCellBorder(canvas, right - tarihColW, y, right, y + baslikH)
        drawCentered(canvas, "TEST VE ÖLÇÜ KONTROL RAPORU", (left + right - tarihColW) / 2f, y + baslikH / 2f + 4f, boldPaint(12f, true))
        // Tarih, kutunun sol kenarına yaslı (ortalı değil)
        labelValue(canvas, "TARİH:", tarihStr, right - tarihColW + 6f, y + baslikH / 2f + 3f)
        y += baslikH

        // ---- Bilgi tablosu: PARÇA ADI | FİRMA ADI / MİKTAR | RAPOR NO / PARTİ NO ----
        val bilgiUstH = 38f   // FİRMA ADI / RAPOR NO satırı - firma adı 2 satıra sarılabildiği için geniş
        val bilgiAltH = 18f   // MİKTAR / PARTİ NO satırı
        val bilgiH = bilgiUstH + bilgiAltH
        val x1 = left
        val x2 = left + col1W
        val x3 = x2 + col2W
        drawCellBorder(canvas, x1, y, x2, y + bilgiH)
        drawCellBorder(canvas, x2, y, x3, y + bilgiUstH)
        drawCellBorder(canvas, x2, y + bilgiUstH, x3, y + bilgiH)
        drawCellBorder(canvas, x3, y, right, y + bilgiUstH)
        drawCellBorder(canvas, x3, y + bilgiUstH, right, y + bilgiH)

        labelValue(canvas, "PARÇA ADI:", p.parcaAdiSatir1, x1 + 4f, y + 15f)
        canvas.drawText(p.parcaAdiSatir2, x1 + 4f, y + 29f, normalPaint(8.5f))

        canvas.drawText("FİRMA ADI:", x2 + 4f, y + 10f, boldPaint(7.6f))
        drawWrapped(canvas, girdi.firmaAdi, x2 + 4f, y + 20f, col2W - 8f, normalPaint(7.6f), maxLines = 2, lineGap = 9.3f)

        val miktarText = "${girdi.miktarDeger} ${girdi.miktarBirim}" + (girdi.adet?.let { "   (Adet: $it)" } ?: "")
        labelValue(canvas, "MİKTAR:", miktarText, x2 + 4f, y + bilgiUstH + 13f)

        // Rapor No / Parti No, kutunun sol kenarına yaslı (ortalı değil)
        labelValue(canvas, "RAPOR NO:", raporNo, x3 + 6f, y + 14f)
        labelValue(canvas, "PARTİ NO:", RaporNoUretici.PARTI_NO, x3 + 6f, y + bilgiUstH + 13f)
        y += bilgiH + 6f

        // ---- Zincir Ölçüleri tablosu ----
        // 7 sütun: [resim] [ürün ölç. etiket] [ürün ölç. değer] [numune etiket] [numune1] [numune2] [numune3]
        // NOT: teknik çizim eninize göre geniş, boyunca kısa bir resim — bu yüzden onu büyütmenin
        // asıl anahtarı SÜTUN GENİŞLİĞİ değil, SATIR YÜKSEKLİĞİdir (aşağıda zHeaderH/zRowH artırıldı).
        val zHeaderH = 18f
        val zRowH = 26f
        val zRows = 4
        val zTableH = zHeaderH + zRowH * zRows
        val picColW = 148f
        val urunLblColW = 38f
        val urunValColW = 40f
        val numLblColW = 24f
        val numuneColW = (CONTENT_W - picColW - urunLblColW - urunValColW - numLblColW) / 3f

        val cx0 = left
        val cx1 = cx0 + picColW
        val cx2 = cx1 + urunLblColW
        val cx3 = cx2 + urunValColW
        val cx4 = cx3 + numLblColW
        val cx5 = cx4 + numuneColW
        val cx6 = cx5 + numuneColW
        val cx7 = right

        // dış çerçeve
        drawCellBorder(canvas, cx0, y, cx7, y + zTableH)
        // Ana grup sınırları (Zincir Ölç. / Ürün Ölç. / Numune Ölç.) tüm tabloyu boydan boya keser
        listOf(cx1, cx3).forEach { canvas.drawLine(it, y, it, y + zTableH, borderPaint) }
        // Alt sütun ayraçları (etiket/değer, numune1/2/3) SADECE veri satırlarında —
        // başlık satırının içinden geçip birleşik başlık yazısını bölmesin diye başlığın altından başlar
        listOf(cx2, cx4, cx5, cx6).forEach { canvas.drawLine(it, y + zHeaderH, it, y + zTableH, borderPaint) }
        // başlık satırı alt çizgisi
        canvas.drawLine(cx0, y + zHeaderH, cx7, y + zHeaderH, borderPaint)
        // veri satırları arası çizgiler (resim sütunu hariç, birleşik)
        for (i in 1 until zRows) {
            val ly = y + zHeaderH + zRowH * i
            canvas.drawLine(cx1, ly, cx7, ly, borderPaint)
        }

        // "ZİNCİR ÖLÇÜLERİ" başlığı sadece resim sütununun üstünde (kutuya tam ortalı),
        // "ÜRÜN ÖLÇÜLERİ" başlığı etiket + değer sütununu (2 sütun) birlikte kapsar,
        // "NUMUNE ÖLÇÜLERİ" başlığı etiket + 3 değer sütununu (4 sütun) birlikte kapsar.
        drawCenteredBoth(canvas, "ZİNCİR ÖLÇÜLERİ", cx0, y, cx1, y + zHeaderH, boldPaint(7.5f, true))
        drawCenteredBoth(canvas, "ÜRÜN ÖLÇÜLERİ", cx1, y, cx3, y + zHeaderH, boldPaint(7.5f, true))
        drawCenteredBoth(canvas, "NUMUNE ÖLÇÜLERİ", cx3, y, cx7, y + zHeaderH, boldPaint(7.5f, true))

        // Gerçek teknik çizim (d/t/b1/b2 ölçü okları) — kullanıcının verdiği resim, büyütülmüş
        drawChainDiagram(context, canvas, cx0 + 3f, y + zHeaderH + 2f, picColW - 6f, zTableH - zHeaderH - 4f)

        val urunEtiketleri = listOf("d", "t", "b1(Min)", "b2 (Max)")
        val numuneEtiketleri = listOf("d", "t", "b1", "b2")
        val satirlar = listOf(p.zincirOlculeri.d, p.zincirOlculeri.t, p.zincirOlculeri.b1, p.zincirOlculeri.b2)
        satirlar.forEachIndexed { i, satir ->
            val ry = y + zHeaderH + zRowH * i + zRowH / 2f + 3f
            drawCentered(canvas, urunEtiketleri[i], (cx1 + cx2) / 2f, ry, normalPaint(7.3f))
            drawCentered(canvas, fmt(satir.urun), (cx2 + cx3) / 2f, ry, normalPaint(8f))
            drawCentered(canvas, numuneEtiketleri[i], (cx3 + cx4) / 2f, ry, normalPaint(7.3f))
            val numCols = listOf(cx4 to cx5, cx5 to cx6, cx6 to cx7)
            for (k in 0 until 3) {
                val v = satir.numune.getOrNull(k)
                if (v != null) {
                    drawCentered(canvas, fmt(v), (numCols[k].first + numCols[k].second) / 2f, ry, normalPaint(8f))
                }
            }
        }
        y += zTableH + 6f

        // ---- İlk Boyu / Deneme Yükü / Malzeme  +  Son Boyu / Kopma Yükü / Standart ----
        // Sütun çizgileri, estetik bütünlük için yukarıdaki bilgi tablosunun (PARÇA ADI/FİRMA ADI/RAPOR NO)
        // aynı dikey çizgileriyle (x2, x3) hizalanıyor.
        val infoRowH = 26f
        val c1 = x2
        val c2 = x3
        drawCellBorder(canvas, left, y, c1, y + infoRowH)
        drawCellBorder(canvas, c1, y, c2, y + infoRowH)
        drawCellBorder(canvas, c2, y, right, y + infoRowH)
        labelValueInline(canvas, "İLK BOYU:", fmt(p.ilkBoyu), left + 4f, y + infoRowH / 2f + 3f)
        labelValueInline(canvas, "DENEME YÜKÜ (kN):", fmt(p.denemeYuku), c1 + 4f, y + infoRowH / 2f + 3f)
        labelValueBox(canvas, "MALZEME:", p.malzeme, c2 + 4f, y, right - c2 - 8f, infoRowH)
        y += infoRowH

        drawCellBorder(canvas, left, y, c1, y + infoRowH)
        drawCellBorder(canvas, c1, y, c2, y + infoRowH)
        drawCellBorder(canvas, c2, y, right, y + infoRowH)
        labelValueInline(canvas, "SON BOYU:", fmt(p.sonBoyu), left + 4f, y + infoRowH / 2f + 3f)
        labelValueInline(canvas, "KOPMA YÜKÜ (kN):", fmt(p.kopmaYukuTekli), c1 + 4f, y + infoRowH / 2f + 3f)
        labelValueBox(canvas, "STANDART:", p.standart, c2 + 4f, y, right - c2 - 8f, infoRowH)
        y += infoRowH + 6f

        // ---- Malzemenin Kimyasal Analizi ----
        val kimyaBaslikH = 14f
        drawCellBorder(canvas, left, y, right, y + kimyaBaslikH)
        drawCentered(canvas, "MALZEMENİN KİMYASAL ANALİZİ", (left + right) / 2f, y + kimyaBaslikH / 2f + 3f, boldPaint(8.5f, true))
        y += kimyaBaslikH

        val kimyaRowH = 13f
        for (satir in p.kimyasalAnalizTablosu) {
            val n = satir.hucreler.size.coerceAtLeast(1)
            val colW = CONTENT_W / n
            drawCellBorder(canvas, left, y, right, y + kimyaRowH)
            for (i in 0 until n) {
                val cx = left + colW * i
                if (i > 0) canvas.drawLine(cx, y, cx, y + kimyaRowH, borderPaint)
                val txt = satir.hucreler.getOrElse(i) { "" }
                canvas.drawText(txt, cx + 3f, y + kimyaRowH / 2f + 3f, normalPaint(7.8f))
            }
            y += kimyaRowH
        }
        y += 4f
        canvas.drawText("Bu Sertifika EN 10204 3.1 'e Uyumludur.", left, y + 8f, normalPaint(8f))
        y += 18f

        // ---- Açıklamalar ----
        val aciklamaMetni = aciklamaUret(p) + if (girdi.ekAciklama.isNotBlank()) "\n${girdi.ekAciklama}" else ""
        val aciklamaSatirlari = mutableListOf<String>()
        aciklamaMetni.split("\n").forEach { parca ->
            aciklamaSatirlari += wrapText(parca, normalPaint(8.3f), CONTENT_W - 8f)
        }
        val aciklamaH = 14f + aciklamaSatirlari.size * 11f
        drawCellBorder(canvas, left, y, right, y + aciklamaH)
        canvas.drawText("AÇIKLAMALAR:", left + 4f, y + 11f, boldPaint(8.5f))
        var ay = y + 23f
        for (satir in aciklamaSatirlari) {
            canvas.drawText(satir, left + 4f, ay, normalPaint(8.3f))
            ay += 11f
        }
        y += aciklamaH + 6f

        // ---- Alt tablo: Tarih / (Sertlik) / Kopma Yükü ----
        val sertlikVar = p.kategori in Product.G80_ACIKLAMA_KATEGORILERI && p.sertlikTablosu.isNotEmpty()
        val yeniKopma = girdi.sabitKopmaYuku ?: KopmaYukuRastgeleleyici.uygula(p.kopmaYukuTablosu)
        val satirSayisi = maxOf(yeniKopma.size, p.sertlikTablosu.size, 1)

        val altBaslikH = 14f
        val altSatirH = 12f
        val altTablosuH = altBaslikH + altSatirH * satirSayisi
        val tarihColW2 = 70f

        drawCellBorder(canvas, left, y, right, y + altTablosuH)
        canvas.drawLine(left + tarihColW2, y, left + tarihColW2, y + altTablosuH, borderPaint)
        canvas.drawLine(left, y + altBaslikH, right, y + altBaslikH, borderPaint)

        val sertlikColsX: List<Float>
        val kopmaColsX: List<Float>
        if (sertlikVar) {
            val sertlikW = (CONTENT_W - tarihColW2) * 0.45f
            val kopmaW = (CONTENT_W - tarihColW2) - sertlikW
            val sertlikStart = left + tarihColW2
            val kopmaStart = sertlikStart + sertlikW
            canvas.drawLine(kopmaStart, y, kopmaStart, y + altTablosuH, borderPaint)
            drawCentered(canvas, "SERTLİK (${p.sertlikBirimi})", sertlikStart + sertlikW / 2f, y + altBaslikH / 2f + 3f, boldPaint(7.5f, true))
            drawCentered(canvas, "KOPMA YÜKÜ (${p.kopmaBirimi})", kopmaStart + kopmaW / 2f, y + altBaslikH / 2f + 3f, boldPaint(7.5f, true))
            sertlikColsX = listOf(sertlikStart, sertlikStart + sertlikW / 3f, sertlikStart + sertlikW * 2f / 3f, sertlikStart + sertlikW)
            kopmaColsX = listOf(kopmaStart, kopmaStart + kopmaW / 3f, kopmaStart + kopmaW * 2f / 3f, kopmaStart + kopmaW)
            for (i in 1..2) {
                canvas.drawLine(sertlikColsX[i], y + altBaslikH, sertlikColsX[i], y + altTablosuH, borderPaint)
                canvas.drawLine(kopmaColsX[i], y + altBaslikH, kopmaColsX[i], y + altTablosuH, borderPaint)
            }
        } else {
            val kopmaStart = left + tarihColW2
            val kopmaW = CONTENT_W - tarihColW2
            drawCentered(canvas, "KOPMA YÜKÜ (${p.kopmaBirimi})", kopmaStart + kopmaW / 2f, y + altBaslikH / 2f + 3f, boldPaint(8f, true))
            sertlikColsX = emptyList()
            val n = (yeniKopma.maxOfOrNull { it.degerler.size } ?: 3).coerceAtLeast(1)
            kopmaColsX = (0..n).map { kopmaStart + kopmaW * it / n }
            for (i in 1 until n) {
                canvas.drawLine(kopmaColsX[i], y + altBaslikH, kopmaColsX[i], y + altTablosuH, borderPaint)
            }
        }
        drawCentered(canvas, "TARİH", left + tarihColW2 / 2f, y + altBaslikH / 2f + 3f, boldPaint(7.5f, true))

        for (i in 0 until satirSayisi) {
            val rowTopY = y + altBaslikH + altSatirH * i
            if (i > 0) canvas.drawLine(left, rowTopY, right, rowTopY, borderPaint)
            val ry = rowTopY + altSatirH / 2f + 3f
            drawCentered(canvas, tarihStr, left + tarihColW2 / 2f, ry, normalPaint(7.5f))
            if (sertlikVar && i < p.sertlikTablosu.size) {
                val vals = p.sertlikTablosu[i].degerler
                for (k in vals.indices) {
                    if (k + 1 < sertlikColsX.size) {
                        drawCentered(canvas, fmt(vals[k]), (sertlikColsX[k] + sertlikColsX[k + 1]) / 2f, ry, normalPaint(7.5f))
                    }
                }
            }
            if (i < yeniKopma.size) {
                val vals = yeniKopma[i].degerler
                for (k in vals.indices) {
                    if (k + 1 < kopmaColsX.size) {
                        drawCentered(canvas, fmt(vals[k]), (kopmaColsX[k] + kopmaColsX[k + 1]) / 2f, ry, normalPaint(7.5f))
                    }
                }
            }
        }
        y += altTablosuH + 10f

        // ---- Alt bilgi / imza blokları (sabit, dar aralıklı) ----
        canvas.drawText(
            "BU DOKÜMAN BAŞ ZİNCİR SAN. VE TİC. A.Ş.'YE AİTTİR. İZİNSİZ KULLANILAMAZ. KOPYA EDİLEMEZ.",
            left, y, normalPaint(7f)
        )
        y += 9f
        canvas.drawText("ÜÇÜNCÜ ŞAHISLARA DEVREDİLEMEZ. HER HAKKI MAHFUZDUR.", left, y, normalPaint(7f))
        y += 14f

        val sigColW = CONTENT_W / 3f
        val sig1 = left
        val sig2 = left + sigColW
        val sig3 = left + sigColW * 2f
        val sigHeaderH = 14f
        val sigRoleH = 11f
        val sigNameH = 13f
        val sigImzaH = 11f
        val sigTotalH = sigHeaderH + sigRoleH + sigNameH + sigImzaH

        drawCellBorder(canvas, left, y, right, y + sigTotalH)
        canvas.drawLine(sig2, y, sig2, y + sigTotalH, borderPaint)
        canvas.drawLine(sig3, y, sig3, y + sigTotalH, borderPaint)
        canvas.drawLine(left, y + sigHeaderH, right, y + sigHeaderH, borderPaint)

        drawCentered(canvas, "HAZIRLAYAN", sig1 + sigColW / 2f, y + sigHeaderH / 2f + 3f, boldPaint(8.5f, true))
        drawCentered(canvas, "KONTROL EDEN", sig2 + sigColW / 2f, y + sigHeaderH / 2f + 3f, boldPaint(8.5f, true))
        drawCentered(canvas, "ONAYLAYAN", sig3 + sigColW / 2f, y + sigHeaderH / 2f + 3f, boldPaint(8.5f, true))

        val roleY = y + sigHeaderH + sigRoleH - 2f
        drawCentered(canvas, "TESTİ YAPAN", sig1 + sigColW / 2f, roleY, normalPaint(7.5f))
        drawCentered(canvas, "ÜRETİM MÜDÜRÜ", sig2 + sigColW / 2f, roleY, normalPaint(7.5f))
        drawCentered(canvas, "ŞİRKET MÜDÜRÜ", sig3 + sigColW / 2f, roleY, normalPaint(7.5f))

        val nameY = y + sigHeaderH + sigRoleH + sigNameH - 3f
        drawCentered(canvas, "Savaş Alabıçak", sig1 + sigColW / 2f, nameY, normalPaint(8.3f))
        drawCentered(canvas, "Ferhat Demir", sig2 + sigColW / 2f, nameY, normalPaint(8.3f))
        drawCentered(canvas, "Nadir Baş", sig3 + sigColW / 2f, nameY, normalPaint(8.3f))

        val imzaY = y + sigTotalH - 3f
        drawCentered(canvas, "e-İmza", sig1 + sigColW / 2f, imzaY, normalPaint(7.5f))
        drawCentered(canvas, "e-İmza", sig2 + sigColW / 2f, imzaY, normalPaint(7.5f))
        drawCentered(canvas, "e-İmza", sig3 + sigColW / 2f, imzaY, normalPaint(7.5f))

        doc.finishPage(page)

        val dosyaAdi = "${p.urunAdi} Test Raporu - ${girdi.firmaAdi}.pdf"
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, dosyaAdi)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun paylasUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun aciklamaUret(p: Product): String {
        return if (p.kategori in Product.G80_ACIKLAMA_KATEGORILERI) {
            val olcuAdi = p.parcaAdiSatir1
            "İmalat sonrası yapılan ölçü kontrollerde $olcuAdi zincirin mekanik ölçü kontrolünün standart " +
                "değerlere uygun olduğu görülmüş olup yapılan koparma çekme testi sonrasında zincirin kaynak " +
                "harici bölgeden standart değerler üzerinde koptuğu görülmüştür."
        } else {
            "Bu test raporu müşteri talebi üzerine stokta bulunan zincir üzerinden yapılmış olup yapılan ölçü " +
                "kontrollerde zincirin standart değerlere uygun olduğu yapılan koparma testinde zincir kaynak " +
                "harici bölgeden standart değer üzerinde koptuğu görülmüştür."
        }
    }

    // ---------- çizim yardımcıları ----------

    private fun drawCellBorder(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawRect(x1, y1, x2, y2, borderPaint)
    }

    private fun drawCentered(canvas: Canvas, text: String, cx: Float, baselineY: Float, paint: Paint) {
        val p = Paint(paint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(text, cx, baselineY, p)
    }

    /** Metni verilen kutunun hem yatayda hem dikeyde tam ortasına yazar; kutuya sığmazsa
     *  otomatik olarak (kelime sınırından) 2 satıra böler. */
    private fun drawCenteredBoth(canvas: Canvas, text: String, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        val p = Paint(paint).apply { textAlign = Paint.Align.CENTER }
        val cx = (x1 + x2) / 2f
        val cy = (y1 + y2) / 2f
        val maxWidth = (x2 - x1) - 4f
        if (p.measureText(text) <= maxWidth) {
            val fm = p.fontMetrics
            val baseline = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(text, cx, baseline, p)
        } else {
            val satirlar = wrapText(text, p, maxWidth).take(2)
            val lineH = p.textSize + 1.5f
            var by = cy - lineH / 2f + p.textSize / 2f
            for (s in satirlar) {
                canvas.drawText(s, cx, by, p)
                by += lineH
            }
        }
    }

    private fun labelValue(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        val lp = boldPaint(8f)
        canvas.drawText(label, x, y, lp)
        if (value.isNotEmpty()) {
            val lw = lp.measureText(label)
            canvas.drawText(value, x + lw + 3f, y, normalPaint(8.3f))
        }
    }

    private fun labelValueInline(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        val lp = boldPaint(7.5f)
        canvas.drawText(label, x, y, lp)
        val lw = lp.measureText(label)
        canvas.drawText(value, x + lw + 3f, y, normalPaint(7.8f))
    }

    private fun drawWrapped(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint, maxLines: Int, lineGap: Float = paint.textSize + 2f) {
        val lines = wrapText(text, paint, maxWidth).take(maxLines)
        var yy = y
        for (l in lines) {
            canvas.drawText(l, x, yy, paint)
            yy += lineGap
        }
    }

    /** Etiket + değeri tek satıra sığdırmaya çalışır; sığmazsa etiketi üst satıra,
     *  değeri (gerekirse 2 satıra sarılmış) alt satır(lar)a yazar — taşmayı önler. */
    private fun labelValueBox(canvas: Canvas, label: String, value: String, x: Float, yTop: Float, maxWidth: Float, rowH: Float) {
        val lp = boldPaint(7.5f)
        val vp = normalPaint(7.6f)
        val tekSatirW = lp.measureText(label) + 3f + vp.measureText(value)
        if (tekSatirW <= maxWidth) {
            canvas.drawText(label, x, yTop + rowH / 2f + 3f, lp)
            canvas.drawText(value, x + lp.measureText(label) + 3f, yTop + rowH / 2f + 3f, vp)
        } else {
            canvas.drawText(label, x, yTop + 9f, lp)
            val satirlar = wrapText(value, vp, maxWidth).take(2)
            var vy = yTop + 18f
            for (s in satirlar) {
                canvas.drawText(s, x, vy, vp)
                vy += 8.5f
            }
        }
    }

    /** Kullanıcının sağladığı gerçek teknik çizimi (d/t/b1/b2 ölçü oklu) resim olarak çizer,
     *  en-boy oranını koruyarak verilen kutuya ortalar. */
    private fun drawChainDiagram(context: Context, canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val resId = context.resources.getIdentifier("zincir_diyagram", "drawable", context.packageName)
        if (resId == 0) return
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
        val boxRatio = w / h
        val drawW: Float
        val drawH: Float
        if (bmpRatio > boxRatio) {
            drawW = w
            drawH = w / bmpRatio
        } else {
            drawH = h
            drawW = h * bmpRatio
        }
        val left = x + (w - drawW) / 2f
        val top = y + (h - drawH) / 2f
        canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), null)
    }

    private fun fmt(v: Double): String {
        if (v == 0.0) return "0"
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(Locale("tr"), "%.2f", v)
    }

    private fun boldPaint(size: Float, center: Boolean = false) = Paint().apply {
        color = Color.BLACK
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
    }

    private fun normalPaint(size: Float) = Paint().apply {
        color = Color.BLACK
        textSize = size
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val test = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(test) > maxWidth) {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(w)
            } else {
                current = StringBuilder(test)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
