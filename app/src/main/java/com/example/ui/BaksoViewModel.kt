package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BaksoMenu
import com.example.data.BaksoOrder
import com.example.data.BaksoRepository
import com.example.data.CartItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CART,
    CHECKOUT,
    TRACKING,
    HISTORY
}

class BaksoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BaksoRepository

    // Screen States
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database Flows
    val allMenus: StateFlow<List<BaksoMenu>>
    val cartItems: StateFlow<List<CartItem>>
    val allOrders: StateFlow<List<BaksoOrder>>

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Filtered Menus Flow
    val filteredMenus: StateFlow<List<BaksoMenu>>

    // Cart Details
    val cartSubtotal: StateFlow<Double>
    val cartItemCount: StateFlow<Int>

    // Checkout Form Inputs
    var addressInput = MutableStateFlow("Jl. Mangga Besar No. 42, RT 05/RW 03, Jakarta Barat")
    var deliveryNotesInput = MutableStateFlow("Pagar hitam sebelah warung kelontong, tolong sambal dipisah ya pak.")
    var selectedPaymentMethod = MutableStateFlow("Tunai (COD)") // Options: "Tunai (COD)", "GoPay", "DANA", "Transfer Mandiri"
    var selectedDeliveryType = MutableStateFlow("Regular")       // Options: "Regular", "Express"

    // Active Tracking
    private val _activeTrackingOrderId = MutableStateFlow<Int?>(null)
    val activeTrackingOrderId: StateFlow<Int?> = _activeTrackingOrderId.asStateFlow()

    val activeTrackingOrder: StateFlow<BaksoOrder?>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BaksoRepository(database.baksoDao())

        // Prepopulate baseline data
        viewModelScope.launch {
            repository.prepopulateMenuIfNeeded()
        }

        allMenus = repository.allMenus.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        cartItems = repository.cartItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allOrders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactive filter
        filteredMenus = combine(allMenus, _searchQuery, _selectedCategory) { list, query, cat ->
            list.filter { item ->
                val matchesQuery = item.name.contains(query, ignoreCase = true) || 
                                   item.description.contains(query, ignoreCase = true)
                val matchesCategory = cat == "Semua" || item.category == cat
                matchesQuery && matchesCategory
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Cart calculations
        cartSubtotal = cartItems.map { list ->
            list.sumOf { it.price * it.quantity }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        cartItemCount = cartItems.map { list ->
            list.sumOf { it.quantity }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        // Tracking stream
        activeTrackingOrder = _activeTrackingOrderId.flatMapLatest { id ->
            if (id != null) {
                repository.getOrderById(id)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    // Navigation Methods
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Search and Filters
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Cart Operations
    fun addToCart(menu: BaksoMenu, notes: String = "") {
        viewModelScope.launch {
            repository.addToCart(menu, notes)
        }
    }

    fun increaseQuantity(item: CartItem) {
        viewModelScope.launch {
            repository.updateCartQuantity(item, item.quantity + 1)
        }
    }

    fun decreaseQuantity(item: CartItem) {
        viewModelScope.launch {
            repository.updateCartQuantity(item, item.quantity - 1)
        }
    }

    fun removeCartItem(item: CartItem) {
        viewModelScope.launch {
            repository.removeCartItem(item)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    // Checkout & Order Placement
    fun checkout() {
        val deliveryFee = if (selectedDeliveryType.value == "Express") 15000.0 else 8000.0
        viewModelScope.launch {
            val orderId = repository.placeOrder(
                address = addressInput.value,
                deliveryNotes = deliveryNotesInput.value,
                paymentMethod = selectedPaymentMethod.value,
                deliveryType = selectedDeliveryType.value,
                deliveryFee = deliveryFee
            )
            if (orderId != -1) {
                _activeTrackingOrderId.value = orderId
                _currentScreen.value = AppScreen.TRACKING
                // Start a simulated background job to update delivery tracker step-by-step
                simulateDeliveryStatus(orderId)
            }
        }
    }

    fun viewOrderTracking(orderId: Int) {
        _activeTrackingOrderId.value = orderId
        _currentScreen.value = AppScreen.TRACKING
    }

    // Simulated Courier / Restaurant Update
    private fun simulateDeliveryStatus(orderId: Int) {
        viewModelScope.launch {
            // Step 1: PENDING (0s - already there)
            delay(5000) // After 5s, food is confirmed
            repository.updateOrderStatus(orderId, "CONFIRMED")
            
            delay(6000) // After another 6s, chef is cooking
            repository.updateOrderStatus(orderId, "COOKING")
            
            delay(7000) // After another 7s, driver picks it up
            repository.updateOrderStatus(orderId, "DELIVERING")
            
            delay(8000) // After another 8s, delivered!
            repository.updateOrderStatus(orderId, "COMPLETED")
        }
    }
}
