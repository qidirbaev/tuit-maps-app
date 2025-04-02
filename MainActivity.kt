@file:Suppress("DEPRECATION", "PrivatePropertyName", "LocalVariableName")

package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private val REQUEST_LOCATION_PERMISSION = 1
    private val PREFS_NAME = "AppPreferences"
    private val FIRST_LAUNCH_KEY = "isFirstLaunch"
//    private val FAVORITE_LOCATIONS_KEY = "favoriteLocations"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)

        setContent {
            if (isFirstLaunch) {
                AnimatedWelcomeScreen {
                    sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply()
                    setContent {
                        EnhancedLocationApp(sharedPreferences)
                    }
                }
            } else {
                EnhancedLocationApp(sharedPreferences)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setContent {
                        EnhancedLocationApp(sharedPreferences)
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedLocationApp(sharedPreferences: SharedPreferences) {
    var currentScreen by remember { mutableStateOf("input") }
    var targetLocation by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var favoriteLocations by remember { mutableStateOf(loadFavoriteLocations(sharedPreferences)) }
    var mapMode by remember { mutableStateOf("standard") }

    when (currentScreen) {
        "input" -> EnhancedLocationInputScreen(
            onLocationSelected = { location ->
                targetLocation = location
                currentScreen = "map"
            },
            onViewFavorites = { currentScreen = "favorites" },
            favoriteLocations = favoriteLocations
        )
        "map" -> EnhancedMapsDemo(
            targetLocation = targetLocation,
            mapMode = mapMode,
            onBack = { currentScreen = "input" },
            onChangeMapMode = { newMode -> mapMode = newMode },
            onAddToFavorites = { name, location ->
                val newFavorites = favoriteLocations.toMutableMap()
                newFavorites[name] = location
                favoriteLocations = newFavorites
                saveFavoriteLocations(sharedPreferences, favoriteLocations)
            }
        )
        "favorites" -> FavoritesScreen(
            favoriteLocations = favoriteLocations,
            onBack = { currentScreen = "input" },
            onSelectLocation = { location ->
                targetLocation = location
                currentScreen = "map"
            },
            onRemoveLocation = { name ->
                val newFavorites = favoriteLocations.toMutableMap()
                newFavorites.remove(name)
                favoriteLocations = newFavorites
                saveFavoriteLocations(sharedPreferences, favoriteLocations)
            }
        )
    }
}

@Composable
fun AnimatedWelcomeScreen(onContinue: () -> Unit) {
    var animateIn by remember { mutableStateOf(false) }
    var pulseState by remember { mutableFloatStateOf(0f) }
    val pulseSizeFraction = 0.1f

    val pulseAnimation = rememberInfiniteTransition()
    val pulse by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1f + pulseSizeFraction,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(key1 = true) {
        animateIn = true
        delay(500)
        pulseState = pulse
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E3B4E),
                        Color(0xFF1A2639)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
            ) {
                Text(
                    text = "First individual work",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = animateIn,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
            ) {
                Text(
                    text = "Mobile development",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFB8C5D9),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = animateIn,
                enter = scaleIn() + fadeIn(initialAlpha = 0.3f)
            ) {
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C3E50)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .width(200.dp)
                        .height(56.dp)
                        .scale(pulseState)
                ) {
                    Text(
                        "Explore",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLocationInputScreen(
    onLocationSelected: (LatLng) -> Unit,
    onViewFavorites: () -> Unit,
    favoriteLocations: Map<String, LatLng>
) {
    var latText by remember { mutableStateOf(TextFieldValue("")) }
    var lngText by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedQuickLocation by remember { mutableStateOf<Pair<String, LatLng>?>(null) }

    val quickLocations = listOf(
        "New York" to LatLng(40.7128, -74.0060),
        "London" to LatLng(51.5074, -0.1278),
        "Tokyo" to LatLng(35.6762, 139.6503),
        "Sydney" to LatLng(-33.8688, 151.2093),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA),
                        Color(0xFFE4E8F0)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Where would you like to go?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.padding(vertical = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Enter Coordinates",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = latText.text,
                        onValueChange = {
                            latText = TextFieldValue(it)
                            selectedQuickLocation = null
                        },
                        label = { Text("Latitude") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF2C3E50),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF2C3E50),
                        ),
                        isError = errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = lngText.text,
                        onValueChange = {
                            lngText = TextFieldValue(it)
                            selectedQuickLocation = null
                        },
                        label = { Text("Longitude") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF2C3E50),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF2C3E50),
                        ),
                        isError = errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Locations",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        quickLocations.forEach { (name, latLng) ->
                            QuickLocationChip(
                                name = name,
                                isSelected = selectedQuickLocation?.first == name,
                                onSelect = {
                                    selectedQuickLocation = name to latLng
                                    latText = TextFieldValue(latLng.latitude.toString())
                                    lngText = TextFieldValue(latLng.longitude.toString())
                                }
                            )
                        }
                    }
                }
            }

            if (favoriteLocations.isNotEmpty()) {
                Button(
                    onClick = onViewFavorites,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF313029)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("View Favorite Locations (${favoriteLocations.size})")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    try {
                        val lat = latText.text.toDouble()
                        val lng = lngText.text.toDouble()

                        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                            errorMessage = "Latitude must be between -90 and 90, Longitude between -180 and 180"
                            return@Button
                        }

                        errorMessage = null
                        onLocationSelected(LatLng(lat, lng))
                    } catch (e: NumberFormatException) {
                        errorMessage = "Invalid latitude or longitude format"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF313029)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "Explore Location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickLocationChip(name: String, isSelected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) Color(0xFF2C3E50) else Color(0xFFE4E8F0)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else Color(0xFF2C3E50),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EnhancedMapsDemo(
    targetLocation: LatLng,
    mapMode: String,
    onBack: () -> Unit,
    onChangeMapMode: (String) -> Unit,
    onAddToFavorites: (String, LatLng) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, 15f)
    }
    var locationName by remember { mutableStateOf("Fetching location...") }
    var showInfoPanel by remember { mutableStateOf(false) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteName by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Fetch location name when targetLocation changes
    LaunchedEffect(targetLocation) {
        locationName = getLocationName(context, targetLocation.latitude, targetLocation.longitude)
    }

    if (showFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showFavoriteDialog = false },
            title = { Text("Save Location") },
            text = {
                Column {
                    Text("Enter a name for this location:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = favoriteName,
                        onValueChange = { favoriteName = it },
                        label = { Text("Location Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (favoriteName.isNotEmpty()) {
                            onAddToFavorites(favoriteName, targetLocation)
                            showFavoriteDialog = false
                            favoriteName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFavoriteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = when (mapMode) {
                    "satellite" -> null  // Use satellite view
                    "night" -> MapStyleOptions("""[{"featureType":"all","elementType":"geometry","stylers":[{"color":"#242f3e"}]},{"featureType":"all","elementType":"labels.text.stroke","stylers":[{"lightness":-80}]},{"featureType":"administrative","elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},{"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},{"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#6b9a76"}]},{"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#2b3544"}]},{"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},{"featureType":"road.arterial","elementType":"geometry.fill","stylers":[{"color":"#38414e"}]},{"featureType":"road.arterial","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},{"featureType":"road.highway","elementType":"geometry.fill","stylers":[{"color":"#746855"}]},{"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},{"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},{"featureType":"road.local","elementType":"geometry.fill","stylers":[{"color":"#38414e"}]},{"featureType":"road.local","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},{"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},{"featureType":"transit.station","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},{"featureType":"water","elementType":"labels.text.stroke","stylers":[{"lightness":-20}]}]""")
                    else -> null  // Standard view
                },
                mapType = if (mapMode == "satellite") MapType.SATELLITE else MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                compassEnabled = true,
                zoomControlsEnabled = true,
                mapToolbarEnabled = true
            )
        ) {
            Marker(
                state = MarkerState(position = targetLocation),
                title = locationName
            )
        }

        // Top location panel
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { showInfoPanel = !showInfoPanel }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = "📍 $locationName",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Control buttons at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Back")
            }

            // Map type buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { onChangeMapMode("standard") },
                    containerColor = if (mapMode == "standard") Color(0xFF313029) else Color.Gray.copy(alpha = 0.7f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("M")
                }

                FloatingActionButton(
                    onClick = { onChangeMapMode("satellite") },
                    containerColor = if (mapMode == "satellite") Color(0xFF313029) else Color.Gray.copy(alpha = 0.7f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("S")
                }

                FloatingActionButton(
                    onClick = { onChangeMapMode("night") },
                    containerColor = if (mapMode == "night") Color(0xFF313029) else Color.Gray.copy(alpha = 0.7f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("N")
                }
            }

            Button(
                onClick = { showFavoriteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF313029).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("★ Save")
            }
        }

        // Extra info panel
        AnimatedVisibility(
            visible = showInfoPanel,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Location Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Latitude: ${targetLocation.latitude}")
                    Text("Longitude: ${targetLocation.longitude}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Address: $locationName")
                }
            }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteLocations: Map<String, LatLng>,
    onBack: () -> Unit,
    onSelectLocation: (LatLng) -> Unit,
    onRemoveLocation: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA),
                        Color(0xFFE4E8F0)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Favorite Locations",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )

            // Empty box for alignment
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (favoriteLocations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You haven't saved any locations yet.\nSave locations to view them here.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                favoriteLocations.forEach { (name, location) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLocation(location) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2C3E50)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lat: ${location.latitude.toString().take(7)}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Lng: ${location.longitude.toString().take(7)}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            IconButton(
                                onClick = { onRemoveLocation(name) }
                            ) {
                                Text(
                                    text = "×",
                                    fontSize = 24.sp,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Favorite locations persistence functions
fun saveFavoriteLocations(sharedPreferences: SharedPreferences, locations: Map<String, LatLng>) {
    val editor = sharedPreferences.edit()
    val locationsSet = locations.map { (name, latLng) ->
        "$name:${latLng.latitude}:${latLng.longitude}"
    }.toSet()

    val FAVORITE_LOCATIONS_KEY = "favoriteLocations"

    editor.putStringSet(FAVORITE_LOCATIONS_KEY, locationsSet)
    editor.apply()
}

fun loadFavoriteLocations(sharedPreferences: SharedPreferences): Map<String, LatLng> {
    val FAVORITE_LOCATIONS_KEY = "favoriteLocations"
    val locationsSet = sharedPreferences.getStringSet(FAVORITE_LOCATIONS_KEY, emptySet()) ?: emptySet()
    return locationsSet.mapNotNull { locationString ->
        val parts = locationString.split(":")
        if (parts.size == 3) {
            try {
                val name = parts[0]
                val lat = parts[1].toDouble()
                val lng = parts[2].toDouble()
                name to LatLng(lat, lng)
            } catch (e: NumberFormatException) {
                Log.e("LocationApp", "Failed to parse location: $locationString", e)
                null
            }
        } else {
            null
        }
    }.toMap()
}

// Extension function to help with location name retrieval
@SuppressLint("DefaultLocale")
fun getLocationName(context: Context, lat: Double, lng: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            when {
                address.thoroughfare != null -> {
                    val sb = StringBuilder()
                    if (address.featureName != null && address.featureName != address.thoroughfare) {
                        sb.append(address.featureName).append(", ")
                    }
                    sb.append(address.thoroughfare)
                    if (address.locality != null) {
                        sb.append(", ").append(address.locality)
                    }
                    sb.toString()
                }
                address.locality != null -> {
                    val sb = StringBuilder(address.locality)
                    if (address.adminArea != null) {
                        sb.append(", ").append(address.adminArea)
                    }
                    sb.toString()
                }
                address.adminArea != null -> address.adminArea
                else -> "Unknown location"
            }
        } else {
            "Location at ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
        }
    } catch (e: Exception) {
        Log.e("LocationApp", "Error getting location name", e)
        "Location at ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
    }
}