package com.example.shoptracklite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>
    
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsSync(): Settings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: Settings)
}

