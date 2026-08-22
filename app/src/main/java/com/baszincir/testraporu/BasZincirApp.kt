package com.baszincir.testraporu

import android.app.Application
import com.google.firebase.FirebaseApp

class BasZincirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    companion object {
        // Bu uygulamaya ve Firestore veritabanına erişebilecek tek yetkili e-postalar.
        // Firestore güvenlik kurallarında da (firestore.rules) aynı liste ayrıca zorunlu kılınıyor,
        // burası sadece giriş ekranında hızlı bir ön kontrol.
        val YETKILI_EMAILLER = setOf(
            "baszincir@gmail.com",
            "baszincirosb@gmail.com",
            "pazarlama2@gmail.com",
            "pdrercumentessiz@gmail.com"
        )
    }
}
