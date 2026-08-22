package com.baszincir.testraporu.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baszincir.testraporu.data.Rapor
import com.baszincir.testraporu.pdf.PdfGenerator
import com.baszincir.testraporu.pdf.RaporGirdisi
import java.text.SimpleDateFormat
import java.util.*

/**
 * Daha önce oluşturulmuş raporların listesi. Her satır dokunulduğunda, o raporun
 * kaydedilmiş girdileri (ürün, firma, tarih, miktar, ve o ana özel kopma yükü
 * değerleri) ile PDF yeniden üretilip paylaşım menüsü açılır — değerler ilk
 * oluşturulduğu haliyle AYNI kalır, yeniden rastgele üretilmez.
 */
@Composable
fun HistoryScreen(vm: ReportViewModel) {
    val context = LocalContext.current
    var arama by remember { mutableStateOf("") }

    val dateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }

    val filtreli = remember(arama, vm.raporlar) {
        if (arama.isBlank()) vm.raporlar
        else vm.raporlar.filter {
            it.urunAdi.contains(arama, ignoreCase = true) || it.firmaAdi.contains(arama, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Geçmiş Raporlar (${vm.raporlar.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = arama,
            onValueChange = { arama = it },
            label = { Text("Ürün veya firma adına göre ara") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (vm.raporlar.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Henüz oluşturulmuş bir rapor yok. \"Yeni Rapor\" sekmesinden bir rapor " +
                    "oluşturduğunuzda burada listelenecek.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn {
            items(filtreli, key = { it.id }) { rapor ->
                ListItem(
                    headlineContent = { Text("${rapor.urunAdi} — ${rapor.firmaAdi}") },
                    supportingContent = {
                        Text("Tarih: ${dateFmt.format(Date(rapor.tarihMillis))}   •   Rapor No: ${rapor.raporNo}")
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                try {
                                    val urun = vm.urunler.find { it.id == rapor.urunId }
                                    if (urun == null) {
                                        Toast.makeText(context, "Bu raporun ürünü artık bulunamıyor (silinmiş olabilir).", Toast.LENGTH_LONG).show()
                                        return@IconButton
                                    }
                                    val girdi = RaporGirdisi(
                                        product = urun,
                                        firmaAdi = rapor.firmaAdi,
                                        tarihMillis = rapor.tarihMillis,
                                        miktarDeger = rapor.miktarDeger,
                                        miktarBirim = rapor.miktarBirim,
                                        adet = rapor.adet.ifBlank { null },
                                        ekAciklama = rapor.ekAciklama,
                                        sabitKopmaYuku = rapor.kopmaYukuTablosu
                                    )
                                    val dosya = PdfGenerator.olustur(context, girdi)
                                    val uri = PdfGenerator.paylasUri(context, dosya)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Raporu Paylaş / Kaydet"))
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "PDF oluşturulurken hata oldu: ${e.message ?: e.javaClass.simpleName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Paylaş")
                            }
                            IconButton(onClick = { vm.raporSil(rapor.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                )
                Divider()
            }
        }
    }
}
