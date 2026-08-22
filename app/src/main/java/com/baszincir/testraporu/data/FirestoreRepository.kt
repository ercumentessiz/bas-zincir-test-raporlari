package com.baszincir.testraporu.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore koleksiyon yapısı:
 *   urunler/{productId}   -> Product alanları
 *   musteriler/{customerId} -> Customer alanları
 *   raporlar/{raporId}    -> Rapor alanları (geçmiş raporlar)
 *
 * Bütün okuma/yazma işlemleri yalnızca giriş yapmış (ve firestore.rules'da izinli olan)
 * kullanıcılar için çalışır.
 */
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val urunlerRef = db.collection("urunler")
    private val musterilerRef = db.collection("musteriler")
    private val raporlarRef = db.collection("raporlar")

    /** Bozuk/eksik alanlı tek bir kayıt yüzünden tüm listenin patlamaması için,
     *  her belge ayrı ayrı ve güvenli şekilde okunur — sorunlu olan atlanır. */
    suspend fun tumUrunler(): List<Product> {
        val snap = urunlerRef.get().await()
        return snap.documents.mapNotNull { doc ->
            try {
                doc.toObject(Product::class.java)?.apply { id = doc.id }
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.urunAdi }
    }

    suspend fun urunKaydet(product: Product) {
        val id = product.id.ifBlank { urunlerRef.document().id }
        product.id = id
        urunlerRef.document(id).set(product, SetOptions.merge()).await()
    }

    suspend fun urunSil(productId: String) {
        urunlerRef.document(productId).delete().await()
    }

    suspend fun tumMusteriler(): List<Customer> {
        val snap = musterilerRef.get().await()
        return snap.documents.mapNotNull { doc ->
            try {
                doc.toObject(Customer::class.java)?.apply { id = doc.id }
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.ad }
    }

    suspend fun musteriKaydet(customer: Customer) {
        val id = customer.id.ifBlank { musterilerRef.document().id }
        customer.id = id
        musterilerRef.document(id).set(customer, SetOptions.merge()).await()
    }

    suspend fun musteriSil(customerId: String) {
        musterilerRef.document(customerId).delete().await()
    }

    /** Örnek verileri (res/raw'dan okunan) toplu halde Firestore'a yazar. Var olan kayıtların üzerine yazar (merge). */
    suspend fun topluUrunYukle(urunler: List<Product>) {
        urunler.chunked(400).forEach { parca ->
            val batch = db.batch()
            parca.forEach { p ->
                val id = p.id.ifBlank { urunlerRef.document().id }
                batch.set(urunlerRef.document(id), p.copy(id = id), SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    suspend fun topluMusteriYukle(musteriler: List<Customer>) {
        musteriler.chunked(400).forEach { parca ->
            val batch = db.batch()
            parca.forEach { c ->
                val id = c.id.ifBlank { musterilerRef.document().id }
                batch.set(musterilerRef.document(id), c.copy(id = id), SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    /** Yeni oluşturulan raporu geçmişe kaydeder (en yeni en üstte görünecek şekilde tarih damgalı). */
    suspend fun raporKaydet(rapor: Rapor) {
        val id = rapor.id.ifBlank { raporlarRef.document().id }
        rapor.id = id
        raporlarRef.document(id).set(rapor, SetOptions.merge()).await()
    }

    /** En yeni oluşturulan rapor en üstte olacak şekilde tüm geçmiş raporları getirir. */
    suspend fun tumRaporlar(): List<Rapor> {
        val snap = raporlarRef.get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(Rapor::class.java)?.apply { id = doc.id }
        }.sortedByDescending { it.olusturulmaMillis }
    }

    suspend fun raporSil(raporId: String) {
        raporlarRef.document(raporId).delete().await()
    }
}
