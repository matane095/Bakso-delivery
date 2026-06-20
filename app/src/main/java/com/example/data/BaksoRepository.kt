package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BaksoRepository(private val baksoDao: BaksoDao) {

    val allMenus: Flow<List<BaksoMenu>> = baksoDao.getAllMenus()
    val cartItems: Flow<List<CartItem>> = baksoDao.getCartItems()
    val allOrders: Flow<List<BaksoOrder>> = baksoDao.getAllOrders()

    suspend fun prepopulateMenuIfNeeded() {
        if (baksoDao.getMenuCount() == 0) {
            val defaultMenus = listOf(
                BaksoMenu(
                    name = "Bakso Urat Granat",
                    description = "Bakso sapi urat ukuran jumbo yang kenyal dan gurih, diisi potongan sambal cabai rawit merah pedas meledak di mulut.",
                    price = 28000.0,
                    rating = 4.8f,
                    category = "Spesial Pedas",
                    emoji = "🌶️"
                ),
                BaksoMenu(
                    name = "Bakso Halus Kuah Kaldu",
                    description = "Bakso sapi halus premium yang super empuk dan lembut, disajikan dengan kuah kaldu sapi asli yang gurih nan segar.",
                    price = 22000.0,
                    rating = 4.7f,
                    category = "Favorit Klasik",
                    emoji = "🥣"
                ),
                BaksoMenu(
                    name = "Bakso Mercon Lava",
                    description = "Bakso sapi jumbo berlumur bumbu mercon cabai rawit melimpah, kuah merah membakar lidah.",
                    price = 26000.0,
                    rating = 4.9f,
                    category = "Spesial Pedas",
                    emoji = "☄️"
                ),
                BaksoMenu(
                    name = "Bakso Isian Keju Mozzarella",
                    description = "Bakso sapi berkualitas dengan isian keju mozzarella lumer melimpah di bagian dalamnya yang gurih dan gurih.",
                    price = 25000.0,
                    rating = 4.6f,
                    category = "Modern / Unik",
                    emoji = "🧀"
                ),
                BaksoMenu(
                    name = "Bakso Telur Puyuh Gurih",
                    description = "Bakso sapi lezat dengan isian telur puyuh rebus di tengahnya, kenikmatan dobel di setiap gigitan.",
                    price = 23000.0,
                    rating = 4.5f,
                    category = "Favorit Klasik",
                    emoji = "🥚"
                ),
                BaksoMenu(
                    name = "Bakso Raksasa Beranak",
                    description = "Satu bakso ukuran super besar yang di dalamnya berisi beberapa bakso cilik, daging cincang, dan kuah spesial.",
                    price = 35000.0,
                    rating = 4.9f,
                    category = "Jumbo Kenyang",
                    emoji = "🍲"
                ),
                BaksoMenu(
                    name = "Mie Ayam Bakso Spesial",
                    description = "Mie keriting kenyal dengan topping semur ayam gurih melimpah, disajikan bersama dua butir bakso halus pilihan.",
                    price = 24000.0,
                    rating = 4.8f,
                    category = "Mie & Ayam",
                    emoji = "🍜"
                ),
                BaksoMenu(
                    name = "Es Teh Manis Jumbo",
                    description = "Es teh manis segar dari seduhan teh murni berkualitas, penyegar tenggorokan paling pas.",
                    price = 5000.0,
                    rating = 4.8f,
                    category = "Minuman Segar",
                    emoji = "🍹"
                ),
                BaksoMenu(
                    name = "Es Jeruk Peras Murni",
                    description = "Es jeruk segar hasil perasan jeruk asli pilihan dengan perpaduan asam manis seimbang.",
                    price = 7000.0,
                    rating = 4.7f,
                    category = "Minuman Segar",
                    emoji = "🍊"
                )
            )
            baksoDao.insertMenus(defaultMenus)
        }
    }

    suspend fun addToCart(menu: BaksoMenu, notes: String = "") {
        val currentCart = cartItems.first()
        val existingItem = currentCart.find { it.menuId == menu.id }
        if (existingItem != null) {
            val updated = existingItem.copy(
                quantity = existingItem.quantity + 1,
                notes = if (notes.isNotEmpty()) notes else existingItem.notes
            )
            baksoDao.updateCartItem(updated)
        } else {
            baksoDao.insertCartItem(
                CartItem(
                    menuId = menu.id,
                    name = menu.name,
                    price = menu.price,
                    quantity = 1,
                    notes = notes,
                    emoji = menu.emoji
                )
            )
        }
    }

    suspend fun updateCartQuantity(cartItem: CartItem, newQty: Int) {
        if (newQty <= 0) {
            baksoDao.deleteCartItem(cartItem)
        } else {
            baksoDao.updateCartItem(cartItem.copy(quantity = newQty))
        }
    }

    suspend fun removeCartItem(cartItem: CartItem) {
        baksoDao.deleteCartItem(cartItem)
    }

    suspend fun placeOrder(
        address: String,
        deliveryNotes: String,
        paymentMethod: String,
        deliveryType: String,
        deliveryFee: Double
    ): Int {
        val currentCart = cartItems.first()
        if (currentCart.isEmpty()) return -1

        val subtotal = currentCart.sumOf { it.price * it.quantity }
        val total = subtotal + deliveryFee
        val itemsSummary = currentCart.joinToString(", ") { "${it.quantity}x ${it.name}" }

        val order = BaksoOrder(
            totalPrice = total,
            address = address,
            deliveryNotes = deliveryNotes,
            status = "PENDING",
            itemsSummary = itemsSummary,
            paymentMethod = paymentMethod,
            deliveryType = deliveryType,
            deliveryFee = deliveryFee
        )

        val orderId = baksoDao.insertOrder(order).toInt()
        baksoDao.clearCart()
        return orderId
    }

    fun getOrderById(orderId: Int): Flow<BaksoOrder?> {
        return baksoDao.getOrderById(orderId)
    }

    suspend fun updateOrderStatus(orderId: Int, newStatus: String) {
        baksoDao.updateOrderStatus(orderId, newStatus)
    }

    suspend fun clearCart() {
        baksoDao.clearCart()
    }
}
