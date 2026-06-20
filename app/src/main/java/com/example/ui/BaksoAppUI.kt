package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BaksoMenu
import com.example.data.BaksoOrder
import com.example.data.CartItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// Helper formatting Indonesian Rupiah
fun formatRupiah(amount: Double): String {
    val formatter = DecimalFormat("#,###")
    return "Rp " + formatter.format(amount).replace(",", ".")
}

// Convert timestamp to neat date string
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaksoAppUI(viewModel: BaksoViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartCount by viewModel.cartItemCount.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            // Persist bottom navigation for easy navigation across screens
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Menu") },
                    label = { Text("Menu") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_menu")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CART || currentScreen == AppScreen.CHECKOUT,
                    onClick = { viewModel.navigateTo(AppScreen.CART) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(cartCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Keranjang")
                        }
                    },
                    label = { Text("Keranjang") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_cart")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORY || currentScreen == AppScreen.TRACKING,
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                    icon = { Icon(Icons.Filled.History, contentDescription = "Riwayat") },
                    label = { Text("Transaksi") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Animate screen transitions smoothly
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.HOME -> MenuListScreen(viewModel)
                    AppScreen.CART -> CartScreen(viewModel)
                    AppScreen.CHECKOUT -> CheckoutScreen(viewModel)
                    AppScreen.TRACKING -> DeliveryTrackingScreen(viewModel)
                    AppScreen.HISTORY -> OrderHistoryScreen(viewModel)
                }
            }
        }
    }
}

// 1. MENU LIST SCREEN
@Composable
fun MenuListScreen(viewModel: BaksoViewModel) {
    val menus by viewModel.filteredMenus.collectAsState()
    val categories = listOf("Semua", "Favorit Klasik", "Spesial Pedas", "Jumbo Kenyang", "Mie & Ayam", "Minuman Segar")
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showNotesDialogMenu by remember { mutableStateOf<BaksoMenu?>(null) }
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("menu_screen")
    ) {
        // App top header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bakso Nusantara 🍲",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "Warung Bakso",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kuah Kaldu Gurih Spesial & Berbagai Pilihan Bakso Mantap!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Search Bar & Filter Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-14).dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Cari bakso urat, mercon, mie ayam...", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar_input")
            )
        }

        // Horizontal Category Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(category) },
                    label = { 
                        Text(
                            text = category, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Food Grid/List
        if (menus.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🔍", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bakso tidak ditemukan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coba cari menu lezat yang lainnya, juragan!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("menus_list"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(menus) { menu ->
                    FoodCardItem(
                        menu = menu,
                        onAddToCart = {
                            showNotesDialogMenu = menu
                            noteText = ""
                        }
                    )
                }
            }
        }
    }

    // Modal dialogue for custom notes when adding bakso to basket
    if (showNotesDialogMenu != null) {
        val selectedMenu = showNotesDialogMenu!!
        AlertDialog(
            onDismissRequest = { showNotesDialogMenu = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedMenu.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sesuaikan Pesanan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = selectedMenu.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedMenu.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Catatan Masakan (Opsional)", fontSize = 12.sp) },
                        placeholder = { Text("Contoh: sambal dipisah, seledri banyak, mie putih saja", fontSize = 12.sp, color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_notes_input"),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addToCart(selectedMenu, noteText)
                        showNotesDialogMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_to_cart_confirm")
                ) {
                    Text("Tambahkan ke Keranjang", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialogMenu = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun FoodCardItem(menu: BaksoMenu, onAddToCart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_item_${menu.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Emoji for food representation (Aesthetic dynamic fallback)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = menu.emoji,
                    fontSize = 42.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                // Category tag
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = menu.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = menu.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = menu.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = menu.rating.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = formatRupiah(menu.price),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Add button
            IconButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(40.dp)
                    .testTag("add_to_cart_btn_${menu.id}"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Ke Keranjang",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


// 2. SHOPPING CART (KERANJANG) SCREEN
@Composable
fun CartScreen(viewModel: BaksoViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_screen")
    ) {
        // Toolbar
        TopAppBarHeader(title = "Keranjang Belanja 🛒", onBack = { viewModel.navigateTo(AppScreen.HOME) })

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🗑️", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keranjang Kosong",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ayo isi perutmu dengan kenikmatan Bakso Nusantara yang legendaris!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                            .testTag("order_now_btn")
                    ) {
                        Text("Pesan Bakso Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Cart List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("cart_items_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { item ->
                    CartItemCard(
                        item = item,
                        onIncrease = { viewModel.increaseQuantity(item) },
                        onDecrease = { viewModel.decreaseQuantity(item) },
                        onDelete = { viewModel.removeCartItem(item) }
                    )
                }
            }

            // Summary Card at bottom of screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Makanan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(formatRupiah(subtotal), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biaya Pengiriman", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text("Mulai dari Rp 8.000", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.CHECKOUT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("checkout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lanjut ke Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Selesai", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_row_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji representation
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.notes.isNotEmpty()) {
                    Text(
                        text = "📝: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatRupiah(item.price * item.quantity),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quantitative Selector Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .size(28.dp)
                        .testTag("qty_decrease_${item.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.testTag("qty_val_${item.id}")
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(28.dp)
                        .testTag("qty_increase_${item.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_item_${item.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray)
                }
            }
        }
    }
}


// 3. CHECKOUT SCREEN
@Composable
fun CheckoutScreen(viewModel: BaksoViewModel) {
    val address by viewModel.addressInput.collectAsState()
    val notes by viewModel.deliveryNotesInput.collectAsState()
    val paymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    val deliveryType by viewModel.selectedDeliveryType.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()

    val deliveryFee = if (deliveryType == "Express") 15000.0 else 8000.0
    val total = subtotal + deliveryFee

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("checkout_screen")
    ) {
        TopAppBarHeader(title = "Pembayaran & Delivery 🛵", onBack = { viewModel.navigateTo(AppScreen.CART) })

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alamat Pengiriman Section
            Text(
                text = "📍 Alamat Pengantaran",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = address,
                onValueChange = { viewModel.addressInput.value = it },
                label = { Text("Alamat Lengkap", fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.deliveryNotesInput.value = it },
                label = { Text("Petunjuk / Catatan Drop-off", fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_notes_input")
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Layanan Pengiriman Section
            Text(
                text = "⚡ Opsi Delivery",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionSelectorCard(
                    title = "Regular",
                    subtitle = "20-30 Mnt",
                    price = "Rp 8.000",
                    selected = deliveryType == "Regular",
                    onClick = { viewModel.selectedDeliveryType.value = "Regular" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delivery_regular")
                )
                OptionSelectorCard(
                    title = "Express",
                    subtitle = "10-15 Mnt",
                    price = "Rp 15.000",
                    selected = deliveryType == "Express",
                    onClick = { viewModel.selectedDeliveryType.value = "Express" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delivery_express")
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Metode Pembayaran Section
            Text(
                text = "💳 Metode Pembayaran",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tunai (COD)", "GoPay", "DANA", "Transfer Mandiri").forEach { method ->
                    val isSelected = method == paymentMethod
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectedPaymentMethod.value = method }
                            .testTag("payment_option_$method"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectedPaymentMethod.value = method },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = method,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Detail Rincian Biaya
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rincian Biaya", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Bakso", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(formatRupiah(subtotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ongkos Pengiriman", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(formatRupiah(deliveryFee), style = MaterialTheme.typography.bodyMedium)
                    }
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pembayaran", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatRupiah(total),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // checkout CTA Button
            Button(
                onClick = { viewModel.checkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_order_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Konfirmasi & Kirim Bakso! 🍲🛵",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OptionSelectorCard(
    title: String,
    subtitle: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(price, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


// 4. ACTIVE ORDER DELIVERY TRACKING SCREEN (LACAK PENGIRIMAN)
@Composable
fun DeliveryTrackingScreen(viewModel: BaksoViewModel) {
    val activeOrder by viewModel.activeTrackingOrder.collectAsState()

    if (activeOrder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val order = activeOrder!!

    // Tracking Step Calculator
    val stepIndex = when (order.status) {
        "PENDING" -> 0
        "CONFIRMED" -> 1
        "COOKING" -> 2
        "DELIVERING" -> 3
        "COMPLETED" -> 4
        else -> 0
    }

    val mapProgressAnim = remember { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        // Smooth slide coordinates multiplier for animations on canvas
        val targetVal = when (order.status) {
            "PENDING" -> 0.05f
            "CONFIRMED" -> 0.25f
            "COOKING" -> 0.50f
            "DELIVERING" -> 0.75f
            "COMPLETED" -> 1.0f
            else -> 0.0f
        }
        mapProgressAnim.animateTo(
            targetValue = targetVal,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tracking_screen")
    ) {
        TopAppBarHeader(title = "Lacak Pengiriman 🛵", onBack = { viewModel.navigateTo(AppScreen.HISTORY) })

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Est Arrival Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Estimasi Kedatangan Bakso Anda",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (order.status) {
                            "PENDING" -> "Menunggu konfirmasi..."
                            "CONFIRMED" -> "15 - 20 Menit"
                            "COOKING" -> "10 - 15 Menit"
                            "DELIVERING" -> "5 - 10 Menit"
                            "COMPLETED" -> "Bakso Sudah Sampai!"
                            else -> "Segera Tiba!"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = mapProgressAnim.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            // Real-Time Animated Delivery Map Canvas
            Text(
                text = "🗺️ Jalur Pengantaran (Real-time)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("live_map_canvas"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Draw nice minimalist background road path
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw horizontal street path
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(50f, size.height / 2 + 20f),
                            end = Offset(size.width - 50f, size.height / 2 + 20f),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    }

                    // Place the Start Restaurant Icon, End House Icon, and flowing Delivery Courier Icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp, top = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏪", fontSize = 24.sp)
                            Text("Toko Bakso", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, top = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏠", fontSize = 24.sp)
                            Text("Rumah Anda", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Simulated sliding scooter courier indicator
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val availableWidth = maxWidth - 110.dp
                        val offsetDx = (availableWidth * mapProgressAnim.value) + 40.dp
                        
                        Box(
                            modifier = Modifier
                                .absoluteOffset(x = offsetDx, y = (maxHeight / 2) - 22.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (order.status == "COMPLETED") "🍜" else "🛵💨",
                                    fontSize = 28.sp,
                                    modifier = Modifier.animateContentSize()
                                )
                                Text(
                                    text = if (order.status == "COMPLETED") "Selesai!" else "Sedang jalan",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Timeline Stepper vertical representation
            Text(
                text = "📋 Status Pengantaran",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val stepsList = listOf(
                        StepData("Pesanan Diajukan", "Pesanan bakso terkirim ke dapur kami.", "📝", 0),
                        StepData("Dikonfirmasi Warung", "Warung mengonfirmasi pesanan sedang diantre.", "✅", 1),
                        StepData("Sedang Memasak", "Bakso sedang direbus dan dituangkan kuah panas.", "🍳", 2),
                        StepData("Dalam Perjalanan", "Kurir sedang menggeber motor menuju alamat Anda.", "🛵", 3),
                        StepData("Sudah Sampai!", "Pesanan diserahterimakan, nikmati selagi hangat!", "🍜", 4)
                    )

                    stepsList.forEachIndexed { idx, step ->
                        StatusTimelineRow(
                            step = step,
                            isActive = stepIndex >= idx,
                            isLast = idx == stepsList.size - 1
                        )
                    }
                }
            }

            // Courier Info Card
            if (order.status == "DELIVERING" || order.status == "COOKING" || order.status == "CONFIRMED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pak Slamet (Mitra Kurir)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Mengantar dengan Honda Vario Hitam", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(
                            onClick = { /* Simulated Call action - no actual implementation per rule 6 */ },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Hubungi Kurir", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Order details summary block
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alamat Pengiriman", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(order.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    if (order.deliveryNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Catatan Drop-off", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.deliveryNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Detail Menu Pesanan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Metode Pembayaran", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(order.paymentMethod, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Return home button
            Button(
                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("back_to_menu_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Belanja Menu Lainnya", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class StepData(val name: String, val desc: String, val icon: String, val stepIdx: Int)

@Composable
fun StatusTimelineRow(step: StepData, isActive: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_step_${step.stepIdx}"),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(step.icon, fontSize = 12.sp)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = step.name,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else Color.LightGray
            )
            Text(
                text = step.desc,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) Color.Gray else Color.LightGray
            )
        }
    }
}


// 5. ORDER HISTORY SCREEN (RIWAYAT TRANSAKSI)
@Composable
fun OrderHistoryScreen(viewModel: BaksoViewModel) {
    val orders by viewModel.allOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        TopAppBarHeader(title = "Riwayat Pesanan 📋", onBack = { viewModel.navigateTo(AppScreen.HOME) })

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🥣", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Transaksi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Anda belum pernah memesan bakso lezat kami. Yuk buat pesanan pertamamu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("orders_history_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    HistoryOrderCard(
                        order = order,
                        onTrackClick = { viewModel.viewOrderTracking(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryOrderCard(order: BaksoOrder, onTrackClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pesanan #${order.id}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTimestamp(order.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Styled Status Bubble
                val (color, text) = when (order.status) {
                    "PENDING" -> Pair(Color(0xFFE65100), "Dibuat")
                    "CONFIRMED" -> Pair(Color(0xFF00701A), "Dikonfirmasi")
                    "COOKING" -> Pair(Color(0xFFF9A825), "Dimasak")
                    "DELIVERING" -> Pair(Color(0xFF0277BD), "Kurir Di Jalan")
                    "COMPLETED" -> Pair(Color(0xFF2E7D32), "Selesai")
                    else -> Pair(Color.Gray, order.status)
                }

                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = order.itemsSummary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = "Alamat", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = order.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Pembayaran", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = formatRupiah(order.totalPrice),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onTrackClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (order.status == "COMPLETED") MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.primary
                    ),
                    border = if (order.status == "COMPLETED") BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
                    modifier = Modifier.testTag("track_btn_${order.id}")
                ) {
                    Text(
                        text = if (order.status == "COMPLETED") "Detail Pesanan" else "Lacak Delivery",
                        fontWeight = FontWeight.Bold,
                        color = if (order.status == "COMPLETED") MaterialTheme.colorScheme.secondary else Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// SHARED COMPONENT: APP TOP BAR
@Composable
fun TopAppBarHeader(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif
                )
            )
        }
    }
}
