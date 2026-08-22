package com.baszincir.testraporu.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baszincir.testraporu.data.Customer
import com.baszincir.testraporu.data.Product
import com.baszincir.testraporu.data.Rapor
import com.baszincir.testraporu.pdf.PdfGenerator
import com.baszincir.testraporu.pdf.RaporGirdisi
import com.baszincir.testraporu.ui.components.SearchableDropdown
import com.baszincir.testraporu.util.KopmaYukuRastgeleleyici
import com.baszincir.testraporu.util.RaporNoUretici
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportScreen(vm: ReportViewModel) {
    val context = LocalContext.current

    var secilenUrun by remember { mutableStateOf<Product?>(null) }
    var secilenMusteri by remember { mutableStateOf<Customer?>(null) }
    var tarihMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var miktarDeger by remember { mutableStateOf("") }
    var miktarBirim by remember { mutableStateOf("Mt.") }
    var adet by remember { mutableStateOf("") }
    var ekAciklama by remember { mutableStateOf("") }

    val dateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Yeni Test Raporu", style = MaterialTheme.typography.titleLarge)

        SearchableDropdown(
            baslik = "Ürün (Zincir) Seç",
            ogeler = vm.urunler,
            secili = secilenUrun,
            etiketVer = { it.urunAdi },
            onSecim = { secilenUrun = it }
        )

        SearchableDropdown(
            baslik = "Firma Adı Seç",
            ogeler = vm.musteriler,
            secili = secilenMusteri,
            etiketVer = { it.ad },
            onSecim = { secilenMusteri = it }
        )

        OutlinedTextField(
            value = dateFmt.format(Date(tarihMillis)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tarih") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = tarihMillis }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d)
                            tarihMillis = c.timeInMillis
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) { Text("Seç") }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = miktarDeger,
                onValueChange = { miktarDeger = it },
                label = { Text("Miktar") },
                modifier = Modifier.weight(1f)
            )
            var birimAcik by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = miktarBirim,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Birim") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { birimAcik = true }) { Text("Seç") }
                    }
                )
                DropdownMenu(expanded = birimAcik, onDismissRequest = { birimAcik = false }) {
                    listOf("Mt.", "Kg.").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { miktarBirim = it; birimAcik = false })
                    }
                }
            }
        }

        OutlinedTextField(
            value = adet,
            onValueChange = { adet = it },
            label = { Text("Adet (opsiyonel)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ekAciklama,
            onValueChange = { ekAciklama = it },
            label = { Text("Açıklamaya eklenecek ek metin (opsiyonel)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            onClick = {
                val urun = secilenUrun
                val musteri = secilenMusteri
                if (urun == null || musteri == null || miktarDeger.isBlank()) {
                    Toast.makeText(context, "Lütfen ürün, firma ve miktarı doldurun.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                try {
                    // Kopma yükü değerleri burada üretilir; hem PDF'te hem geçmiş kayıtta AYNI değerler kullanılır.
                    val kopmaBuAn = KopmaYukuRastgeleleyici.uygula(urun.kopmaYukuTablosu)
                    val girdi = RaporGirdisi(
                        product = urun,
                        firmaAdi = musteri.ad,
                        tarihMillis = tarihMillis,
                        miktarDeger = miktarDeger,
                        miktarBirim = miktarBirim,
                        adet = adet.ifBlank { null },
                        ekAciklama = ekAciklama,
                        sabitKopmaYuku = kopmaBuAn
                    )
                    val dosya = PdfGenerator.olustur(context, girdi)
                    val uri = PdfGenerator.paylasUri(context, dosya)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Raporu Paylaş / Kaydet"))

                    // Raporu "Geçmiş Raporlar" listesine kaydet (aynı kopma yükü değerleriyle)
                    vm.raporKaydet(
                        Rapor(
                            urunId = urun.id,
                            urunAdi = urun.urunAdi,
                            firmaAdi = musteri.ad,
                            tarihMillis = tarihMillis,
                            raporNo = RaporNoUretici.uret(tarihMillis),
                            miktarDeger = miktarDeger,
                            miktarBirim = miktarBirim,
                            adet = adet,
                            ekAciklama = ekAciklama,
                            kopmaYukuTablosu = kopmaBuAn,
                            olusturulmaMillis = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Rapor oluşturulurken bir hata oldu: ${e.message ?: e.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PDF Raporu Oluştur")
        }
    }
}
