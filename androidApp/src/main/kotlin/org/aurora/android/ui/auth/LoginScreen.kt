package org.aurora.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aurora.android.auth.model.User
import org.aurora.android.auth.service.AuthService

@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    authService: AuthService
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo and Title Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Aurora Logo",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF1E88E5)
                    )
                    
                    Text(
                        text = "Aurora",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    
                    Text(
                        text = "Smart Navigation Platform",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                // Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sign In Tab
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFF06B6D4) else Color.Transparent,
                            contentColor = if (selectedTab == 0) Color.White else Color(0xFF757575)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (selectedTab == 0) 4.dp else 0.dp
                        )
                    ) {
                        Text(
                            "Sign In",
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    
                    // Register Tab
                    Button(
                        onClick = { onNavigateToRegister() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF757575)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Register",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                
                // Form Content
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    
                    Text(
                        text = "Sign in to access your dashboard",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email or Username", color = Color(0xFF757575)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF1E88E5),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    
                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password", color = Color(0xFF757575)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF757575)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF1E88E5),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    
                    // Error Message
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            authService.login(email, password)
                                        }
                                        result.onSuccess { user ->
                                            onLoginSuccess(user)
                                        }.onFailure { error ->
                                            errorMessage = error.message ?: "Invalid credentials"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Login failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please fill in all fields"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
