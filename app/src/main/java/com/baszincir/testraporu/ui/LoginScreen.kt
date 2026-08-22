package com.baszincir.testraporu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.baszincir.testraporu.R
import com.baszincir.testraporu.auth.AuthViewModel

@Composable
fun LoginScreen(vm: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var sifreGoster by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Baş Zincir Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Baş Zincir - Test Raporu", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sifre,
            onValueChange = { sifre = it },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = if (sifreGoster) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { sifreGoster = !sifreGoster }) {
                    Icon(
                        imageVector = if (sifreGoster) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (sifreGoster) "Şifreyi gizle" else "Şifreyi göster"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.girisYap(email, sifre) },
            enabled = !vm.yukleniyor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.yukleniyor) "Giriş yapılıyor..." else "Giriş Yap")
        }
        vm.hata?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
