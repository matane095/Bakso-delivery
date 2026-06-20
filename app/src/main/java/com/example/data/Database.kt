package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "menus")
data class BaksoMenu(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val rating: Float,
    val category: String, // Favorite, Spesial, Jumbo, Minuman
    val emoji: String     // e.g. "☄️", "🥣", "🌶️", "🧀", "🥚", "🍹"
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val menuId: Int,
    val name: String,
    val price: Double,
    val quantity: Int,
    val notes: String = "",
    val emoji: String = "🥣"
)

@Entity(tableName = "orders")
data class BaksoOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalPrice: Double,
    val address: String,
    val deliveryNotes: String = "",
    val status: String, // "PENDING", "CONFIRMED", "COOKING", "DELIVERING", "COMPLETED"
    val itemsSummary: String, // e.g., "2x Bakso Urat Granat, 1x Es Jeruk"
    val paymentMethod: String, // "Cash on Delivery", "GoPay", "DANA", "Transfer Bank"
    val deliveryType: String, // "Regular", "Express"
    val deliveryFee: Double
)

// 2. DAO (Data Access Object)
@Dao
interface BaksoDao {
    @Query("SELECT * FROM menus ORDER BY category ASC, id ASC")
    fun getAllMenus(): Flow<List<BaksoMenu>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: BaksoMenu)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(menus: List<BaksoMenu>)

    @Query("SELECT COUNT(*) FROM menus")
    suspend fun getMenuCount(): Int

    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE menuId = :menuId")
    suspend fun deleteCartByMenuId(menuId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<BaksoOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: BaksoOrder): Long

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderById(orderId: Int): Flow<BaksoOrder?>
}

// 3. Database
@Database(entities = [BaksoMenu::class, CartItem::class, BaksoOrder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun baksoDao(): BaksoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bakso_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
