package com.baszincir.testraporu.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baszincir.testraporu.data.Customer
import com.baszincir.testraporu.data.FirestoreRepository
import com.baszincir.testraporu.data.Product
import com.baszincir.testraporu.data.Rapor
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val repo = FirestoreRepository()

    var urunler by mutableStateOf<List<Product>>(emptyList())
        private set
    var musteriler by mutableStateOf<List<Customer>>(emptyList())
        private set
    var raporlar by mutableStateOf<List<Rapor>>(emptyList())
        private set
    var yukleniyor by mutableStateOf(false)
        private set

    init {
        yenile()
    }

    var hataMesaji by mutableStateOf<String?>(null)
        private set

    fun yenile() {
        viewModelScope.launch {
            yukleniyor = true
            try {
                urunler = repo.tumUrunler()
                musteriler = repo.tumMusteriler()
                raporlar = repo.tumRaporlar()
                hataMesaji = null
            } catch (e: Exception) {
                hataMesaji = "Veriler yüklenirken hata oluştu: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                yukleniyor = false
            }
        }
    }

    fun urunKaydet(p: Product, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.urunKaydet(p)
                yenile()
                onDone()
            } catch (e: Exception) {
                hataMesaji = "Ürün kaydedilemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun urunSil(id: String) {
        viewModelScope.launch {
            try {
                repo.urunSil(id)
                yenile()
            } catch (e: Exception) {
                hataMesaji = "Ürün silinemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun musteriKaydet(c: Customer, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.musteriKaydet(c)
                yenile()
                onDone()
            } catch (e: Exception) {
                hataMesaji = "Müşteri kaydedilemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun musteriSil(id: String) {
        viewModelScope.launch {
            try {
                repo.musteriSil(id)
                yenile()
            } catch (e: Exception) {
                hataMesaji = "Müşteri silinemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    /** Yeni oluşturulan bir raporu geçmişe kaydeder (o anki rastgele kopma yükü değerleriyle birlikte). */
    fun raporKaydet(rapor: Rapor) {
        viewModelScope.launch {
            try {
                repo.raporKaydet(rapor)
                raporlar = repo.tumRaporlar()
            } catch (e: Exception) {
                hataMesaji = "Rapor geçmişe kaydedilemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun raporSil(id: String) {
        viewModelScope.launch {
            try {
                repo.raporSil(id)
                raporlar = repo.tumRaporlar()
            } catch (e: Exception) {
                hataMesaji = "Rapor silinemedi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    var yuklemeSonucu by mutableStateOf<String?>(null)
        private set

    fun ornekVerileriYukle(context: android.content.Context) {
        viewModelScope.launch {
            yukleniyor = true
            yuklemeSonucu = null
            try {
                val urunler = com.baszincir.testraporu.data.SeedLoader.urunleriOku(context)
                val musteriler = com.baszincir.testraporu.data.SeedLoader.musterileriOku(context)
                repo.topluUrunYukle(urunler)
                repo.topluMusteriYukle(musteriler)
                yenile()
                yuklemeSonucu = "${urunler.size} ürün ve ${musteriler.size} müşteri yüklendi."
            } catch (e: Exception) {
                yuklemeSonucu = "Hata: ${e.localizedMessage}"
            } finally {
                yukleniyor = false
            }
        }
    }
}
