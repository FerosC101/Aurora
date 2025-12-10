package org.aurora.android.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.aurora.android.database.AppDatabase
import org.aurora.android.database.SavedRouteEntity
import org.aurora.android.repository.SavedRoutesRepository

data class SavedRoute(
    val id: Long,
    val name: String,
    val origin: String,
    val destination: String,
    val distance: String,
    val estimatedTime: String,
    val lastUsed: String,
    val isFavorite: Boolean = false
)

@Composable
fun ExploreScreen(
    onRouteClick: (SavedRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // TODO: Re-enable when KSP is working
    // Repository disabled due to missing Room implementation
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // Dummy data since database is not available
    val savedRoutes = remember { emptyList<SavedRoute>() }
    val favoriteRoutes = remember { emptyList<SavedRoute>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Explore",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF1E88E5),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.fillMaxWidth(),
                            color = Color(0xFF1E88E5)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Saved Routes",
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Favorites",
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
        
        // Content
        when (selectedTab) {
            0 -> SavedRoutesContent(
                routes = savedRoutes,
                onRouteClick = { route ->
                    // TODO: Re-enable when KSP is working
                    // scope.launch {
                    //     repository.markRouteAsUsed(route.id)
                    // }
                    onRouteClick(route)
                },
                onToggleFavorite = { route ->
                    // TODO: Re-enable when KSP is working
                    // scope.launch {
                    //     repository.toggleFavorite(route.id, !route.isFavorite)
                    // }
                },
                onDeleteRoute = { route ->
                    // TODO: Re-enable when KSP is working
                    // scope.launch {
                    //     repository.deleteRouteById(route.id)
                    // }
                }
            )
            1 -> FavoritesContent(
                routes = favoriteRoutes,
                onRouteClick = { route ->
                    // TODO: Re-enable when KSP is working
                    // scope.launch {
                    //     repository.markRouteAsUsed(route.id)
                    // }
                    onRouteClick(route)
                }
            )
        }
    }
}

// Extension function to convert entity to UI model
private fun SavedRouteEntity.toSavedRoute(): SavedRoute {
    val daysSince = (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60 * 24)
    val lastUsedText = when {
        lastUsed == 0L -> "Never"
        daysSince == 0L -> "Today"
        daysSince == 1L -> "Yesterday"
        daysSince < 7 -> "$daysSince days ago"
        else -> "${daysSince / 7} weeks ago"
    }
    
    return SavedRoute(
        id = id,
        name = name,
        origin = origin,
        destination = destination,
        distance = "${String.format("%.1f", distance)} km",
        estimatedTime = "$estimatedTime min",
        lastUsed = lastUsedText,
        isFavorite = isFavorite
    )
}

@Composable
fun SavedRoutesContent(
    routes: List<SavedRoute>,
    onRouteClick: (SavedRoute) -> Unit,
    onToggleFavorite: (SavedRoute) -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit
) {
    if (routes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFBDBDBD)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No saved routes yet",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    onClick = { onRouteClick(route) },
                    onToggleFavorite = { onToggleFavorite(route) },
                    onDeleteRoute = { onDeleteRoute(route) }
                )
            }
        }
    }
}

@Composable
fun FavoritesContent(
    routes: List<SavedRoute>,
    onRouteClick: (SavedRoute) -> Unit
) {
    if (routes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFBDBDBD)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorite routes",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    text = "Mark routes as favorite to see them here",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    onClick = { onRouteClick(route) },
                    onToggleFavorite = null,
                    onDeleteRoute = null
                )
            }
        }
    }
}

@Composable
fun SavedRouteCard(
    route: SavedRoute,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
    onDeleteRoute: (() -> Unit)?
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog && onDeleteRoute != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Route") },
            text = { Text("Are you sure you want to delete '${route.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRoute()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE91E63))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    if (onToggleFavorite != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (route.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (route.isFavorite) Color(0xFFE91E63) else Color(0xFF9E9E9E)
                            )
                        }
                    }
                    
                    if (onDeleteRoute != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete route",
                                tint = Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = route.origin,
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = route.destination,
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RouteInfoChip(Icons.Default.Star, route.distance)
                RouteInfoChip(Icons.Default.DateRange, route.estimatedTime)
                RouteInfoChip(Icons.Default.Email, route.lastUsed)
            }
        }
    }
}

@Composable
fun RouteInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF1E88E5),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF757575)
        )
    }
}
