package com.nextcs.aurora.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.nextcs.aurora.auth.FirebaseAuthManager
import com.nextcs.aurora.data.UserProfileRepository
import com.nextcs.aurora.data.models.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Step 1: Account credentials
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Step 2: Personal information
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    
    // Step 3: Address information
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    
    // Step 4: Emergency contact
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    
    // UI state
    var currentStep by remember { mutableStateOf(1) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }
    val profileRepo = remember { UserProfileRepository() }
    
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    
    fun validateStep1(): Boolean {
        return when {
            email.isBlank() -> {
                errorMessage = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errorMessage = "Invalid email format"
                false
            }
            password.isBlank() -> {
                errorMessage = "Password is required"
                false
            }
            password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                false
            }
            password != confirmPassword -> {
                errorMessage = "Passwords do not match"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }
    
    fun validateStep2(): Boolean {
        return when {
            fullName.isBlank() -> {
                errorMessage = "Full name is required"
                false
            }
            phoneNumber.isBlank() -> {
                errorMessage = "Phone number is required"
                false
            }
            phoneNumber.length < 10 -> {
                errorMessage = "Invalid phone number"
                false
            }
            dateOfBirth.isBlank() -> {
                errorMessage = "Date of birth is required"
                false
            }
            gender.isBlank() -> {
                errorMessage = "Please select gender"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }
    
    fun validateStep3(): Boolean {
        return when {
            address.isBlank() -> {
                errorMessage = "Address is required"
                false
            }
            city.isBlank() -> {
                errorMessage = "City is required"
                false
            }
            country.isBlank() -> {
                errorMessage = "Country is required"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }
    
    fun validateStep4(): Boolean {
        return when {
            emergencyContactName.isBlank() -> {
                errorMessage = "Emergency contact name is required"
                false
            }
            emergencyContactPhone.isBlank() -> {
                errorMessage = "Emergency contact phone is required"
                false
            }
            emergencyContactPhone.length < 10 -> {
                errorMessage = "Invalid emergency contact phone"
                false
            }
            !agreeToTerms -> {
                errorMessage = "You must agree to the terms and conditions"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }
    
    fun registerUser() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                println("🔵 Starting registration for: $email")
                
                // Create Firebase auth account
                val authResult = authManager.createUserWithEmail(email, password)
                authResult.fold(
                    onSuccess = { firebaseUser ->
                        println("✅ Firebase auth user created: ${firebaseUser.uid}")
                        
                        // Update display name
                        val profileUpdateResult = authManager.updateUserProfile(fullName)
                        profileUpdateResult.fold(
                            onSuccess = {
                                println("✅ Display name updated: $fullName")
                                
                                // Create user profile in Firestore
                                val userProfile = UserProfile(
                                    userId = firebaseUser.uid,
                                    fullName = fullName,
                                    email = email,
                                    phoneNumber = phoneNumber,
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    address = address,
                                    city = city,
                                    country = country,
                                    emergencyContactName = emergencyContactName,
                                    emergencyContactPhone = emergencyContactPhone
                                )
                                
                                println("🔵 Saving user profile to Firestore...")
                                
                                // Try to save profile but don't block on it
                                scope.launch {
                                    val saveResult = profileRepo.saveUserProfile(userProfile)
                                    saveResult.fold(
                                        onSuccess = {
                                            println("✅ User profile saved successfully to Firestore!")
                                        },
                                        onFailure = { error ->
                                            println("❌ Profile save failed (but auth succeeded): ${error.message}")
                                            error.printStackTrace()
                                        }
                                    )
                                }
                                
                                // Complete registration even if Firestore save fails
                                println("✅ Registration complete! Proceeding to app...")
                                isLoading = false
                                onRegisterSuccess()
                            },
                            onFailure = { error ->
                                println("❌ Failed to update display name: ${error.message}")
                                error.printStackTrace()
                                isLoading = false
                                errorMessage = "Failed to update profile: ${error.message}"
                            }
                        )
                    },
                    onFailure = { error ->
                        println("❌ Registration failed: ${error.message}")
                        error.printStackTrace()
                        isLoading = false
                        errorMessage = when {
                            error.message?.contains("email address is already in use") == true -> 
                                "This email is already registered"
                            error.message?.contains("network") == true -> 
                                "Network error. Please check your connection"
                            error.message?.contains("A network error") == true -> 
                                "Network error. Please check your internet connection"
                            else -> "Registration failed: ${error.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                println("❌ Unexpected error during registration: ${e.message}")
                e.printStackTrace()
                isLoading = false
                errorMessage = "Unexpected error: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with progress
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Aurora Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Step $currentStep of 4",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { currentStep / 4f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF06B6D4),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }
            
            // Form card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message
                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    when (currentStep) {
                        1 -> {
                            // Step 1: Account Credentials
                            Text(
                                text = "Account Credentials",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address *") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Lock else Icons.Default.Lock,
                                            null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Lock else Icons.Default.Lock,
                                            null
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) 
                                    VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        2 -> {
                            // Step 2: Personal Information
                            Text(
                                text = "Personal Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number *") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("+1234567890") }
                            )
                            
                            OutlinedTextField(
                                value = dateOfBirth,
                                onValueChange = { dateOfBirth = it },
                                label = { Text("Date of Birth *") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("YYYY-MM-DD") }
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = showGenderMenu,
                                onExpandedChange = { showGenderMenu = it }
                            ) {
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Gender *") },
                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGenderMenu) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = showGenderMenu,
                                    onDismissRequest = { showGenderMenu = false }
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                showGenderMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        3 -> {
                            // Step 3: Address Information
                            Text(
                                text = "Address Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Street Address *") },
                                leadingIcon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = { Text("Country *") },
                                leadingIcon = { Icon(Icons.Default.Place, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        4 -> {
                            // Step 4: Emergency Contact & Terms
                            Text(
                                text = "Emergency Contact",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            OutlinedTextField(
                                value = emergencyContactName,
                                onValueChange = { emergencyContactName = it },
                                label = { Text("Emergency Contact Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = emergencyContactPhone,
                                onValueChange = { emergencyContactPhone = it },
                                label = { Text("Emergency Contact Phone *") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("+1234567890") }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { agreeToTerms = !agreeToTerms },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = agreeToTerms,
                                    onCheckedChange = { agreeToTerms = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I agree to the Terms & Conditions and Privacy Policy",
                                    fontSize = 14.sp,
                                    color = Color(0xFF424242)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Back")
                            }
                        }
                        
                        Button(
                            onClick = {
                                println("🔵 Button clicked! Current step: $currentStep")
                                val isValid = when (currentStep) {
                                    1 -> {
                                        println("🔵 Validating step 1...")
                                        validateStep1()
                                    }
                                    2 -> {
                                        println("🔵 Validating step 2...")
                                        validateStep2()
                                    }
                                    3 -> {
                                        println("🔵 Validating step 3...")
                                        validateStep3()
                                    }
                                    4 -> {
                                        println("🔵 Validating step 4...")
                                        validateStep4()
                                    }
                                    else -> false
                                }
                                
                                println("🔵 Validation result: $isValid")
                                
                                if (isValid) {
                                    if (currentStep < 4) {
                                        println("✅ Moving to step ${currentStep + 1}")
                                        currentStep++
                                    } else {
                                        println("✅ Calling registerUser()...")
                                        registerUser()
                                    }
                                } else {
                                    println("❌ Validation failed: $errorMessage")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (currentStep < 4) "Next" else "Register")
                            }
                        }
                    }
                    
                    // Login link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already have an account? ",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                        Text(
                            text = "Sign In",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }
        }
    }
}
