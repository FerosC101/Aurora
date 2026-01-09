package com.nextcs.aurora.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.location.LocationService
import com.nextcs.aurora.location.PlacePrediction
import com.nextcs.aurora.location.PlacesAutocompleteService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Location search field with Google Places Autocomplete
 * Shows dropdown suggestions as user types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchField(
    value: String,
    onValueChange: (String, LatLng?) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    onCurrentLocationClick: (() -> Unit)? = null,
    onMapPickerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val placesService = remember { PlacesAutocompleteService(context) }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf(value) }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Update search query when value changes externally
    LaunchedEffect(value) {
        if (value != searchQuery) {
            searchQuery = value
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue ->
                searchQuery = newValue
                onValueChange(newValue, null) // Clear coordinates when typing
                
                // Cancel previous search
                searchJob?.cancel()
                
                if (newValue.length >= 3) {
                    showDropdown = true
                    isSearching = true
                    
                    // Debounce search (wait 300ms after user stops typing)
                    searchJob = scope.launch {
                        delay(300)
                        try {
                            predictions = placesService.searchLocations(newValue)
                            isSearching = false
                        } catch (e: Exception) {
                            Log.e("LocationSearch", "Search error", e)
                            isSearching = false
                            predictions = emptyList()
                        }
                    }
                } else {
                    showDropdown = false
                    predictions = emptyList()
                    isSearching = false
                }
            },
            placeholder = { Text(placeholder, color = Color(0xFF424242), fontSize = 15.sp) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loading indicator (shown while searching)
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1976D2)
                        )
                    }
                    
                    // Clear button (X) - positioned left of pin
                    if (searchQuery.isNotEmpty() && !isSearching) {
                        IconButton(
                            onClick = { 
                                searchQuery = ""
                                onValueChange("", null)
                                showDropdown = false
                                predictions = emptyList()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Single Pin Icon - shows menu with options
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "Location Options",
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Use Current Location option (only if available)
                            onCurrentLocationClick?.let { onClick ->
                                DropdownMenuItem(
                                    text = { Text("Use Current Location") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                            }
                            
                            // Pick on Map option (only if available)
                            onMapPickerClick?.let { onClick ->
                                DropdownMenuItem(
                                    text = { Text("Pick on Map") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF1976D2)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF212121),
                unfocusedTextColor = Color(0xFF212121)
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp)
        )

        // Dropdown with predictions
        if (showDropdown && predictions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn {
                    items(predictions) { prediction ->
                        PredictionItem(
                            prediction = prediction,
                            onClick = {
                                showDropdown = false
                                isSearching = true
                                scope.launch {
                                    try {
                                        val details = placesService.getPlaceDetails(prediction.placeId)
                                        if (details?.latLng != null) {
                                            searchQuery = prediction.primaryText
                                            onValueChange(prediction.primaryText, details.latLng)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LocationSearch", "Error getting place details", e)
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        )
                        if (prediction != predictions.last()) {
                            Divider(color = Color(0xFFE0E0E0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF757575),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = prediction.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )
            Text(
                text = prediction.secondaryText,
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        }
    }
}
