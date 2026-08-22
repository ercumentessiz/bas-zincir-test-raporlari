package com.baszincir.testraporu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupProperties

/**
 * Arama yapılabilir, tek seçimli açılır menü. label -> gösterilen metin.
 *
 * ÖNEMLİ: DropdownMenu varsayılan olarak "focusable" bir Popup açar; bu da her
 * harf yazıldığında listenin yeniden çizilmesiyle birlikte klavye odağının
 * metin kutusundan alınmasına (ve ikinci harfin yazılamamasına) yol açıyordu.
 * PopupProperties(focusable = false) ile menü artık odağı çalmıyor, yazmaya
 * kesintisiz devam edilebiliyor.
 */
@Composable
fun <T> SearchableDropdown(
    baslik: String,
    ogeler: List<T>,
    secili: T?,
    etiketVer: (T) -> String,
    onSecim: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var acik by remember { mutableStateOf(false) }
    var arama by remember { mutableStateOf(secili?.let(etiketVer) ?: "") }

    val filtreli = remember(arama, ogeler) {
        if (arama.isBlank()) ogeler
        else ogeler.filter { etiketVer(it).contains(arama, ignoreCase = true) }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = arama,
            onValueChange = {
                arama = it
                acik = true
            },
            label = { Text(baslik) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DropdownMenu(
            expanded = acik && filtreli.isNotEmpty(),
            onDismissRequest = { acik = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth()
        ) {
            filtreli.take(60).forEach { oge ->
                DropdownMenuItem(
                    text = { Text(etiketVer(oge)) },
                    onClick = {
                        onSecim(oge)
                        arama = etiketVer(oge)
                        acik = false
                    }
                )
            }
        }
    }
}
