package com.example.safebridge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.safebridge.data.dao.AdbAuditDao
import com.example.safebridge.data.dao.ClientPermissionDao
import com.example.safebridge.data.model.AdbAuditEntity
import com.example.safebridge.data.model.ClientPermissionEntity

@Database(
    entities = [
        ClientPermissionEntity::class,
        AdbAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SafeBridgeDatabase : RoomDatabase() {
    abstract fun clientPermissionDao(): ClientPermissionDao
    abstract fun adbAuditDao(): AdbAuditDao

    companion object {
        @Volatile
        private var INSTANCE: SafeBridgeDatabase? = null

        fun getInstance(context: Context): SafeBridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeBridgeDatabase::class.java,
                    "safebridge_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
