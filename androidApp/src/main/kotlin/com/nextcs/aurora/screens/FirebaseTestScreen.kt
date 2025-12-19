package com.nextcs.aurora.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextcs.aurora.auth.FirebaseAuthManager
import com.nextcs.aurora.data.FirestoreRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import kotlinx.coroutines.launch

@Composable
fun FirebaseTestScreen() {
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }
    val firestoreRepo = remember { FirestoreRepository() }
    
    var status by remember { mutableStateOf("Firebase Status: Checking...") }
    var testEmail by remember { mutableStateOf("test@aurora.com") }
    var testPassword by remember { mutableStateOf("testpassword123") }
    
    LaunchedEffect(Unit) {
        try {
            val app = Firebase.app
            status = "✅ Firebase initialized: ${app.name}"
        } catch (e: Exception) {
            status = "❌ Firebase error: ${e.message}"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = status, style = MaterialTheme.typography.titleMedium)
        
        HorizontalDivider()
        
        // Auth Test
        Text("Authentication Test", style = MaterialTheme.typography.titleSmall)
        
        OutlinedTextField(
            value = testEmail,
            onValueChange = { testEmail = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = testPassword,
            onValueChange = { testPassword = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val result = authManager.createUserWithEmail(testEmail, testPassword)
                    status = result.fold(
                        onSuccess = { "✅ User created: ${it.email}" },
                        onFailure = { "❌ Create failed: ${it.message}" }
                    )
                }
            }) {
                Text("Create")
            }
            
            Button(onClick = {
                scope.launch {
                    val result = authManager.signInWithEmail(testEmail, testPassword)
                    status = result.fold(
                        onSuccess = { "✅ Signed in: ${it.email}" },
                        onFailure = { "❌ Sign in failed: ${it.message}" }
                    )
                }
            }) {
                Text("Sign In")
            }
            
            Button(onClick = {
                authManager.signOut()
                status = "✅ Signed out"
            }) {
                Text("Sign Out")
            }
        }
        
        Text("Current user: ${authManager.currentUser?.email ?: "None"}")
    }
}
