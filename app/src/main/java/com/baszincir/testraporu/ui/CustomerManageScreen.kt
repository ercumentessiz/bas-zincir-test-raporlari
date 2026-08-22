package com.baszincir.testraporu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baszincir.testraporu.data.Customer

@Composable
fun CustomerManageScreen(vm: ReportViewModel) {
    var yeniAd by remember { mutableStateOf("") }
    var yeniIl by remember { mutableStateOf("") }
    var formAcik by remember { mutableStateOf(false) }
    var arama by remember { mutableStateOf("") }
    val listeDurumu = rememberLazyListState()

    val filtreli = remember(arama, vm.musteriler) {
        if (arama.isBlank()) vm.musteriler
        else vm.musteriler.filter { it.ad.contains(arama, ignoreCase = true) }
    }

    // Arama metni veya liste değiştiğinde, kayan listeyi her zaman başa al —
    // eski kaydırma konumu yeni (daha uzun/kısa) listede anlamsız kalıp
    // içeriğin "kaymış" görünmesine yol açmasın diye.
    LaunchedEffect(arama, vm.musteriler.size) {
        listeDurumu.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Müşteriler (${vm.musteriler.size})", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { formAcik = !formAcik }) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }

        if (formAcik) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = yeniAd,
                onValueChange = { yeniAd = it },
                label = { Text("Firma adı") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = yeniIl,
                onValueChange = { yeniIl = it },
                label = { Text("İl (opsiyonel)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (yeniAd.isNotBlank()) {
                    vm.musteriKaydet(Customer(ad = yeniAd, il = yeniIl)) {
                        yeniAd = ""; yeniIl = ""; formAcik = false
                    }
                }
            }) { Text("Kaydet") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = arama,
            onValueChange = { arama = it },
            label = { Text("Müşteri ara") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(state = listeDurumu) {
            items(filtreli, key = { it.id }) { musteri ->
                ListItem(
                    headlineContent = { Text(musteri.ad) },
                    supportingContent = { if (musteri.il.isNotBlank()) Text(musteri.il) },
                    trailingContent = {
                        IconButton(onClick = { vm.musteriSil(musteri.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                )
                Divider()
            }
        }
    }
}
