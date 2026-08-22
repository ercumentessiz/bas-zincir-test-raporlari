package com.baszincir.testraporu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baszincir.testraporu.data.Product

/**
 * Basit ürün listesi + ekleme/silme. Ölçü/kimyasal analiz gibi detaylı alanlar
 * Firestore konsolundan da düzenlenebilir; burada temel alanlar (isim, kategori,
 * parça adı, kopma yükü taban değerleri) düzenlenir.
 */
@Composable
fun ProductManageScreen(vm: ReportViewModel) {
    val context = LocalContext.current
    var yeniAdi by remember { mutableStateOf("") }
    var yeniKategori by remember { mutableStateOf(Product.KATEGORI_G80) }
    var formAcik by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ürünler (${vm.urunler.size})", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { formAcik = !formAcik }) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }

        // Her zaman erişilebilir: ürünler VEYA müşteriler boşsa (ikisi birden olması gerekmez),
        // ya da veriyi en güncel sürümle YENİDEN yüklemek isterseniz.
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(12.dp)) {
                Text(
                    if (vm.urunler.isEmpty() || vm.musteriler.isEmpty())
                        "Veritabanında eksik veri var. Uygulamayla birlikte gelen 18 ürünü ve " +
                            "müşteri listesini tek dokunuşla yükleyebilirsiniz."
                    else
                        "Uygulama güncellendiğinde verileri en güncel haliyle yeniden yüklemek " +
                            "isterseniz (mevcut kayıtların üzerine yazar, kopya oluşturmaz):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.ornekVerileriYukle(context) },
                    enabled = !vm.yukleniyor
                ) {
                    Text(if (vm.yukleniyor) "Yükleniyor..." else "Örnek Verileri Yükle / Güncelle")
                }
                vm.yuklemeSonucu?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (formAcik) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = yeniAdi,
                onValueChange = { yeniAdi = it },
                label = { Text("Ürün adı (örn. 20 mm G-80 Zincir)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                FilterChip(
                    selected = yeniKategori == Product.KATEGORI_G80,
                    onClick = { yeniKategori = Product.KATEGORI_G80 },
                    label = { Text("G-80") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = yeniKategori == Product.KATEGORI_KALIBRE,
                    onClick = { yeniKategori = Product.KATEGORI_KALIBRE },
                    label = { Text("Kalibre") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = yeniKategori == Product.KATEGORI_CEKI,
                    onClick = { yeniKategori = Product.KATEGORI_CEKI },
                    label = { Text("Çeki Zinciri") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (yeniAdi.isNotBlank()) {
                    vm.urunKaydet(
                        Product(
                            urunAdi = yeniAdi,
                            kategori = yeniKategori,
                            parcaAdiSatir1 = yeniAdi
                        )
                    ) {
                        yeniAdi = ""
                        formAcik = false
                    }
                }
            }) { Text("Kaydet") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Not: Ölçü, kimyasal analiz ve kopma yükü taban değerleri gibi detaylı " +
                    "alanları Firebase konsolundaki Firestore veri sekmesinden (urunler koleksiyonu) " +
                    "düzenleyebilirsiniz.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn {            items(vm.urunler, key = { it.id }) { urun ->
                ListItem(
                    headlineContent = { Text(urun.urunAdi) },
                    supportingContent = { Text(kategoriEtiketi(urun.kategori)) },
                    trailingContent = {
                        IconButton(onClick = { vm.urunSil(urun.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                )
                Divider()
            }
        }
    }
}

private fun kategoriEtiketi(kategori: String): String = when (kategori) {
    Product.KATEGORI_G80 -> "G-80"
    Product.KATEGORI_KALIBRE -> "Kalibre"
    Product.KATEGORI_CEKI -> "Çeki Zinciri"
    else -> kategori
}
