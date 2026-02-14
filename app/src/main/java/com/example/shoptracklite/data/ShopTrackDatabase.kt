package com.example.shoptracklite.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [
        Product::class, 
        Sale::class, 
        Favorite::class, 
        PriceRange::class, 
        Settings::class, 
        Expense::class, 
        CashReconciliation::class,
        Supply::class,
        ProductSupplyLink::class,
        PurchaseBill::class,
        PurchaseItem::class,
        Category::class
    ],
    version = 19,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ShopTrackDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun priceRangeDao(): PriceRangeDao
    abstract fun settingsDao(): SettingsDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashReconciliationDao(): CashReconciliationDao
    abstract fun supplyDao(): SupplyDao
    abstract fun productSupplyLinkDao(): ProductSupplyLinkDao
    abstract fun purchaseBillDao(): PurchaseBillDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ShopTrackDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sales ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE favorites (productId INTEGER NOT NULL, PRIMARY KEY(productId))")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add hasQuantityBasedPricing column to products table
                database.execSQL("ALTER TABLE products ADD COLUMN hasQuantityBasedPricing INTEGER NOT NULL DEFAULT 0")
                
                // Create price_ranges table with correct schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS price_ranges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        minQuantity INTEGER NOT NULL,
                        maxQuantity INTEGER NOT NULL,
                        price REAL NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                """)
                
                // Create index for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_price_ranges_productId ON price_ranges (productId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop and recreate price_ranges table to fix any schema mismatches
                database.execSQL("DROP TABLE IF EXISTS price_ranges")
                
                database.execSQL("""
                    CREATE TABLE price_ranges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        minQuantity INTEGER NOT NULL,
                        maxQuantity INTEGER NOT NULL,
                        price REAL NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX index_price_ranges_productId ON price_ranges (productId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop and recreate price_ranges table with explicit column definitions
                database.execSQL("DROP TABLE IF EXISTS price_ranges")
                
                database.execSQL("""
                    CREATE TABLE price_ranges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        minQuantity INTEGER NOT NULL,
                        maxQuantity INTEGER NOT NULL,
                        price REAL NOT NULL
                    )
                """)
                
                database.execSQL("CREATE INDEX index_price_ranges_productId ON price_ranges (productId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add barcode column to products table
                database.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add wholesalePrice column to products table
                database.execSQL("ALTER TABLE products ADD COLUMN wholesalePrice REAL")
                
                // Create settings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        wholesaleModeEnabled INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Insert default settings
                database.execSQL("INSERT INTO settings (id, wholesaleModeEnabled) VALUES (1, 0)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add currencyCode column to settings table
                database.execSQL("ALTER TABLE settings ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isWholesale column to sales table
                database.execSQL("ALTER TABLE sales ADD COLUMN isWholesale INTEGER NOT NULL DEFAULT 0")
                
                // Create expenses table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        date INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add displayOrder column to favorites table for reordering
                database.execSQL("ALTER TABLE favorites ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isCancelled column to sales table for cancellation tracking
                database.execSQL("ALTER TABLE sales ADD COLUMN isCancelled INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add shopName column to settings table for report branding
                database.execSQL("ALTER TABLE settings ADD COLUMN shopName TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create cash_reconciliation table for daily cash tracking
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cash_reconciliation (
                        date TEXT PRIMARY KEY NOT NULL,
                        actualCashCounted REAL NOT NULL DEFAULT 0.0,
                        changeForTomorrow REAL NOT NULL DEFAULT 0.0,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add openingCash column for first-day manual entry
                database.execSQL("ALTER TABLE cash_reconciliation ADD COLUMN openingCash REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create supplies table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS supplies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        costPerUnit REAL NOT NULL,
                        lowStockThreshold INTEGER NOT NULL DEFAULT 10,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create product_supply_links table (columns in alphabetical order for Room)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_supply_links (
                        productId INTEGER NOT NULL,
                        quantityConsumed REAL NOT NULL,
                        supplyId INTEGER NOT NULL,
                        PRIMARY KEY(productId, supplyId)
                    )
                """)
                
                // Create purchase_bills table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        supplierName TEXT,
                        totalAmount REAL NOT NULL,
                        notes TEXT
                    )
                """)
                
                // Create purchase_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        billId INTEGER NOT NULL,
                        itemType TEXT NOT NULL,
                        itemId INTEGER,
                        itemName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitCost REAL NOT NULL,
                        totalCost REAL NOT NULL
                    )
                """)
                
                // Create indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_billId ON purchase_items (billId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_product_supply_links_productId ON product_supply_links (productId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_product_supply_links_supplyId ON product_supply_links (supplyId)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN colorHex TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN trackInventory INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)
                database.execSQL("ALTER TABLE products ADD COLUMN categoryId INTEGER")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sales ADD COLUMN transactionId INTEGER")
                database.execSQL("ALTER TABLE settings ADD COLUMN crNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): ShopTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopTrackDatabase::class.java,
                    "shoptrack_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
