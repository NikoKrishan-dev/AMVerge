package com.amverge.android.backend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amverge.android.backend.data.database.dao.EpisodeProjectDao
import com.amverge.android.backend.data.database.dao.ProcessingJobDao
import com.amverge.android.backend.data.database.dao.SceneDao
import com.amverge.android.backend.data.database.dao.ScenepackDao
import com.amverge.android.backend.data.database.dao.SeriesDao
import com.amverge.android.backend.data.database.entity.EpisodeProjectEntity
import com.amverge.android.backend.data.database.entity.ProcessingJobEntity
import com.amverge.android.backend.data.database.entity.SceneEntity
import com.amverge.android.backend.data.database.entity.ScenepackEntity
import com.amverge.android.backend.data.database.entity.ScenepackSceneCrossRef
import com.amverge.android.backend.data.database.entity.SeriesEntity

@Database(
    entities = [
        EpisodeProjectEntity::class,
        SceneEntity::class,
        ProcessingJobEntity::class,
        SeriesEntity::class,
        ScenepackEntity::class,
        ScenepackSceneCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AMVergeDatabase : RoomDatabase() {

    abstract fun episodeProjectDao(): EpisodeProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun processingJobDao(): ProcessingJobDao
    abstract fun seriesDao(): SeriesDao
    abstract fun scenepackDao(): ScenepackDao

    companion object {
        private const val DATABASE_NAME = "amverge.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE episode_projects ADD COLUMN seriesId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE episode_projects ADD COLUMN episodeNumber INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE scenes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scenes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS series (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        coverUri TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenepacks (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenepack_scenes (
                        scenepackId TEXT NOT NULL,
                        sceneId TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        PRIMARY KEY(scenepackId, sceneId),
                        FOREIGN KEY(scenepackId) REFERENCES scenepacks(id) ON DELETE CASCADE,
                        FOREIGN KEY(sceneId) REFERENCES scenes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scenepack_scenes_scenepackId ON scenepack_scenes(scenepackId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scenepack_scenes_sceneId ON scenepack_scenes(sceneId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scenes ADD COLUMN motionScore REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE scenes ADD COLUMN isDeadframe INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: AMVergeDatabase? = null

        fun getInstance(context: Context): AMVergeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AMVergeDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
