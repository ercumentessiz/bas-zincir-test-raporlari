package com.baszincir.testraporu.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baszincir.testraporu.BasZincirApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    var girisYapildi by mutableStateOf(auth.currentUser != null)
        private set
    var hata by mutableStateOf<String?>(null)
        private set
    var yukleniyor by mutableStateOf(false)
        private set

    fun girisYap(email: String, sifre: String) {
        val temizEmail = email.trim().lowercase()
        if (temizEmail !in BasZincirApp.YETKILI_EMAILLER) {
            hata = "Bu e-posta adresinin erişim yetkisi yok."
            return
        }
        yukleniyor = true
        hata = null
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(temizEmail, sifre).await()
                girisYapildi = true
            } catch (e: Exception) {
                hata = "Giriş başarısız: ${e.localizedMessage}"
            } finally {
                yukleniyor = false
            }
        }
    }

    fun cikisYap() {
        auth.signOut()
        girisYapildi = false
    }
}
