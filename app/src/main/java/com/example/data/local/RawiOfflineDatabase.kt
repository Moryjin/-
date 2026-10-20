package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "offline_chapters")
data class OfflineChapterEntity(
    @PrimaryKey val chapterId: String,
    val novelId: String,
    val novelTitle: String,
    val authorName: String,
    val genre: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val originalLanguage: String,
    val paragraphsJson: String,
    val translatedParagraphsJson: String? = null,
    val translatedLanguage: String? = null,
    val lastReadParagraphIndex: Int = 0,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun getParagraphsList(): List<String> {
        return try {
            Json.decodeFromString<List<String>>(paragraphsJson)
        } catch (e: Exception) {
            listOf(paragraphsJson)
        }
    }

    fun getTranslatedParagraphsList(): List<String>? {
        val raw = translatedParagraphsJson ?: return null
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            null
        }
    }
}

@Dao
interface OfflineChapterDao {
    @Query("SELECT * FROM offline_chapters ORDER BY downloadedAt DESC")
    fun observeAllOfflineChapters(): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun observeOfflineChaptersForNovel(novelId: String): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getOfflineChapterById(chapterId: String): OfflineChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineChapter(entity: OfflineChapterEntity)

    @Query("UPDATE offline_chapters SET lastReadParagraphIndex = :paragraphIndex WHERE chapterId = :chapterId")
    suspend fun updateReadingProgress(chapterId: String, paragraphIndex: Int)

    @Query("DELETE FROM offline_chapters WHERE chapterId = :chapterId")
    suspend fun deleteOfflineChapter(chapterId: String)
}

@Database(entities = [OfflineChapterEntity::class], version = 1, exportSchema = false)
abstract class RawiOfflineDatabase : RoomDatabase() {
    abstract fun offlineChapterDao(): OfflineChapterDao

    companion object {
        @Volatile
        private var INSTANCE: RawiOfflineDatabase? = null

        fun getInstance(context: Context): RawiOfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RawiOfflineDatabase::class.java,
                    "rawi_offline_chapters.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class OfflineChapterRepository(private val dao: OfflineChapterDao) {
    val allOfflineChapters: Flow<List<OfflineChapterEntity>> = dao.observeAllOfflineChapters()

    fun observeForNovel(novelId: String): Flow<List<OfflineChapterEntity>> =
        dao.observeOfflineChaptersForNovel(novelId)

    suspend fun getById(chapterId: String): OfflineChapterEntity? =
        dao.getOfflineChapterById(chapterId)

    suspend fun saveChapterForOffline(
        chapterId: String,
        novelId: String,
        novelTitle: String,
        authorName: String,
        genre: String,
        chapterNumber: Int,
        chapterTitle: String,
        originalLanguage: String,
        paragraphs: List<String>,
        translatedParagraphs: List<String>? = null,
        translatedLanguage: String? = null
    ) {
        val entity = OfflineChapterEntity(
            chapterId = chapterId,
            novelId = novelId,
            novelTitle = novelTitle,
            authorName = authorName,
            genre = genre,
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle,
            originalLanguage = originalLanguage,
            paragraphsJson = Json.encodeToString(paragraphs),
            translatedParagraphsJson = translatedParagraphs?.let { Json.encodeToString(it) },
            translatedLanguage = translatedLanguage
        )
        dao.upsertOfflineChapter(entity)
    }

    suspend fun updateProgress(chapterId: String, paragraphIndex: Int) {
        dao.updateReadingProgress(chapterId, paragraphIndex)
    }

    suspend fun removeOfflineChapter(chapterId: String) {
        dao.deleteOfflineChapter(chapterId)
    }
}
