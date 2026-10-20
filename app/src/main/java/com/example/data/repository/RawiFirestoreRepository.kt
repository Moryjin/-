package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuthorProfile
import com.example.data.model.Chapter
import com.example.data.model.CommunityPost
import com.example.data.model.FollowSubscription
import com.example.data.model.Novel
import com.example.data.model.NovelDiscussionGroup
import com.example.data.model.NovelReview
import com.example.data.model.ParagraphComment
import com.example.data.model.ReadingProgress
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RawiFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب جوجل أولاً للوصول إلى السحابة.")
    }

    // --- NOVELS ---

    fun observePublicNovels(): Flow<List<Novel>> = flow {
        requireUserId()
        val path = "novels"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Novel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getPublicNovels(): Result<List<Novel>> {
        val path = "novels"
        return try {
            requireUserId()
            val snapshot = db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Novel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createNovel(
        authorName: String,
        title: String,
        synopsis: String,
        originalLanguage: String,
        genre: String,
        targetAudience: String,
        keywords: List<String>,
        coverColorHex: String,
        isCompleted: Boolean = false
    ): Result<String> {
        val path = "novels"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(path).document()
            val cleanedKeywords = keywords.map { it.trim() }.filter { it.isNotEmpty() }.take(10)
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.trim().take(100),
                "title" to title.trim().take(200),
                "synopsis" to synopsis.trim().take(4000),
                "originalLanguage" to originalLanguage.trim().take(50),
                "genre" to genre.trim().take(60),
                "targetAudience" to targetAudience.trim().take(60),
                "keywords" to cleanedKeywords,
                "coverColorHex" to coverColorHex,
                "chapterCount" to 0,
                "ratingCount" to 0,
                "averageRating" to 0.0,
                "plotRatingAvg" to 0.0,
                "styleRatingAvg" to 0.0,
                "charactersRatingAvg" to 0.0,
                "translationRatingAvg" to 0.0,
                "isCompleted" to isCompleted,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNovelChapterCount(novelId: String, newCount: Int, isCompleted: Boolean? = null): Result<Unit> {
        val path = "novels/$novelId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "chapterCount" to newCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCompleted != null) {
                updates["isCompleted"] = isCompleted
            }
            db.collection("novels").document(novelId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CHAPTERS ---

    fun observeChaptersForNovel(novelId: String): Flow<List<Chapter>> = flow {
        requireUserId()
        val path = "chapters"
        emitAll(
            db.collection(path)
                .whereEqualTo("novelId", novelId)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chapter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.chapterNumber }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createChapter(
        novelId: String,
        chapterNumber: Int,
        title: String,
        paragraphs: List<String>,
        language: String
    ): Result<String> {
        val path = "chapters"
        return try {
            val uid = requireUserId()
            val cleanParagraphs = paragraphs
                .map { it.trim().take(5000) }
                .filter { it.isNotEmpty() }
                .take(60)
                .ifEmpty { listOf(title.trim()) }
            val wordCount = cleanParagraphs.sumOf { it.split(Regex("\\s+")).size }

            val docRef = db.collection(path).document()
            val payload = mapOf(
                "novelId" to novelId,
                "authorId" to uid,
                "chapterNumber" to chapterNumber.coerceAtLeast(1),
                "title" to title.trim().take(200),
                "paragraphs" to cleanParagraphs,
                "language" to language.trim().take(50),
                "wordCount" to wordCount,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- PARAGRAPH SIDE COMMENTS ---

    fun observeCommentsForChapter(chapterId: String): Flow<List<ParagraphComment>> = flow {
        requireUserId()
        val path = "paragraph_comments"
        emitAll(
            db.collection(path)
                .whereEqualTo("chapterId", chapterId)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ParagraphComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addParagraphComment(
        novelId: String,
        chapterId: String,
        paragraphIndex: Int,
        userName: String,
        commentText: String
    ): Result<String> {
        val path = "paragraph_comments"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "novelId" to novelId,
                "chapterId" to chapterId,
                "paragraphIndex" to paragraphIndex.coerceIn(0, 500),
                "userId" to uid,
                "userName" to userName.trim().ifEmpty { "قارئ روي" }.take(100),
                "commentText" to commentText.trim().take(1500),
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- MULTI-CRITERIA REVIEWS ---

    fun observeReviewsForNovel(novelId: String): Flow<List<NovelReview>> = flow {
        requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("novelId", novelId)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NovelReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitNovelReview(
        novel: Novel,
        userName: String,
        plotRating: Int,
        styleRating: Int,
        charactersRating: Int,
        translationRating: Int,
        reviewText: String
    ): Result<String> {
        val path = "reviews"
        return try {
            val uid = requireUserId()
            val p = plotRating.coerceIn(1, 5)
            val s = styleRating.coerceIn(1, 5)
            val c = charactersRating.coerceIn(1, 5)
            val t = translationRating.coerceIn(1, 5)
            val overall = (p + s + c + t) / 4.0

            val docRef = db.collection(path).document()
            val payload = mapOf(
                "novelId" to novel.id,
                "userId" to uid,
                "userName" to userName.trim().ifEmpty { "قارئ روي" }.take(100),
                "plotRating" to p,
                "styleRating" to s,
                "charactersRating" to c,
                "translationRating" to t,
                "overallRating" to overall,
                "reviewText" to reviewText.trim().take(2500),
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            val oldCount = novel.ratingCount.coerceAtLeast(0)
            val newCount = oldCount + 1
            val newAvg = ((novel.averageRating * oldCount) + overall) / newCount
            val newPlot = ((novel.plotRatingAvg * oldCount) + p) / newCount
            val newStyle = ((novel.styleRatingAvg * oldCount) + s) / newCount
            val newChars = ((novel.charactersRatingAvg * oldCount) + c) / newCount
            val newTrans = ((novel.translationRatingAvg * oldCount) + t) / newCount

            db.collection("novels").document(novel.id).update(
                mapOf(
                    "ratingCount" to newCount,
                    "averageRating" to (Math.round(newAvg * 10.0) / 10.0),
                    "plotRatingAvg" to (Math.round(newPlot * 10.0) / 10.0),
                    "styleRatingAvg" to (Math.round(newStyle * 10.0) / 10.0),
                    "charactersRatingAvg" to (Math.round(newChars * 10.0) / 10.0),
                    "translationRatingAvg" to (Math.round(newTrans * 10.0) / 10.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- FOLLOWS & CUSTOM NOTIFICATIONS ---

    fun observeUserFollows(): Flow<List<FollowSubscription>> = flow {
        val uid = requireUserId()
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FollowSubscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getFollowById(followId: String): Result<FollowSubscription?> {
        val path = "follows/$followId"
        return try {
            val doc = db.collection("follows").document(followId).get().await()
            Result.success(doc.toObject(FollowSubscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertFollowSubscription(
        targetId: String,
        targetType: String,
        targetName: String,
        targetSubtitle: String,
        notificationsEnabled: Boolean = true,
        lastKnownChapterCount: Int = 0,
        readingStatus: String = "READING"
    ): Result<String> {
        val uid = requireUserId()
        val docId = "${uid}_${targetType}_${targetId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val path = "follows/$docId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "targetId" to targetId,
                "targetType" to targetType,
                "targetName" to targetName.trim().take(200),
                "targetSubtitle" to targetSubtitle.trim().take(300),
                "notificationsEnabled" to notificationsEnabled,
                "lastKnownChapterCount" to lastKnownChapterCount.coerceAtLeast(0),
                "readingStatus" to readingStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("follows").document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFollowNotificationAndStatus(
        followId: String,
        notificationsEnabled: Boolean,
        readingStatus: String,
        lastKnownChapterCount: Int
    ): Result<Unit> {
        val path = "follows/$followId"
        return try {
            requireUserId()
            db.collection("follows").document(followId).update(
                mapOf(
                    "notificationsEnabled" to notificationsEnabled,
                    "readingStatus" to readingStatus,
                    "lastKnownChapterCount" to lastKnownChapterCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFollowSubscription(followId: String): Result<Unit> {
        val path = "follows/$followId"
        return try {
            requireUserId()
            db.collection("follows").document(followId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- AUTHOR PROFILES ---

    fun observePublicAuthorProfiles(): Flow<List<AuthorProfile>> = flow {
        requireUserId()
        val path = "author_profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AuthorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAuthorProfile(
        displayName: String,
        bio: String,
        specialtyGenres: List<String>,
        websiteUrl: String,
        twitterHandle: String,
        instagramHandle: String
    ): Result<String> {
        val uid = requireUserId()
        val docId = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val path = "author_profiles/$docId"
        return try {
            val existingDoc = db.collection("author_profiles").document(docId).get().await()
            val cleanedGenres = specialtyGenres.map { it.trim() }.filter { it.isNotEmpty() }.take(10)
            if (existingDoc.exists()) {
                db.collection("author_profiles").document(docId).update(
                    mapOf(
                        "displayName" to displayName.trim().take(100),
                        "bio" to bio.trim().take(2000),
                        "specialtyGenres" to cleanedGenres,
                        "websiteUrl" to websiteUrl.trim().take(300),
                        "twitterHandle" to twitterHandle.trim().take(100),
                        "instagramHandle" to instagramHandle.trim().take(100),
                        "visibility" to "PUBLIC",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("author_profiles").document(docId).set(
                    mapOf(
                        "authorId" to uid,
                        "displayName" to displayName.trim().take(100),
                        "bio" to bio.trim().take(2000),
                        "specialtyGenres" to cleanedGenres,
                        "websiteUrl" to websiteUrl.trim().take(300),
                        "twitterHandle" to twitterHandle.trim().take(100),
                        "instagramHandle" to instagramHandle.trim().take(100),
                        "visibility" to "PUBLIC",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- READING PROGRESS TRACKING (Strictly Per-User Isolated) ---

    fun observeUserReadingProgress(): Flow<List<ReadingProgress>> = flow {
        val uid = requireUserId()
        val path = "reading_progress"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ReadingProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveReadingProgress(
        novelId: String,
        novelTitle: String,
        genre: String,
        lastReadChapterId: String,
        lastReadChapterNumber: Int,
        lastReadParagraphIndex: Int,
        completedChapterIds: List<String>,
        totalChapters: Int
    ): Result<String> {
        val uid = requireUserId()
        val docId = "${uid}_${novelId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "reading_progress/$docId"
        return try {
            val safeTotal = totalChapters.coerceAtLeast(1)
            val cleanCompleted = completedChapterIds.distinct().take(100)
            val fraction = (cleanCompleted.size.toDouble() / safeTotal.toDouble()).coerceIn(0.0, 1.0)
            val existingDoc = db.collection("reading_progress").document(docId).get().await()
            if (existingDoc.exists()) {
                db.collection("reading_progress").document(docId).update(
                    mapOf(
                        "novelTitle" to novelTitle.trim().take(200),
                        "genre" to genre.trim().take(60),
                        "lastReadChapterId" to lastReadChapterId.take(128),
                        "lastReadChapterNumber" to lastReadChapterNumber.coerceAtLeast(1),
                        "lastReadParagraphIndex" to lastReadParagraphIndex.coerceIn(0, 1000),
                        "completedChapterIds" to cleanCompleted,
                        "totalChapters" to safeTotal,
                        "progressFraction" to fraction,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("reading_progress").document(docId).set(
                    mapOf(
                        "userId" to uid,
                        "novelId" to novelId,
                        "novelTitle" to novelTitle.trim().take(200),
                        "genre" to genre.trim().take(60),
                        "lastReadChapterId" to lastReadChapterId.take(128),
                        "lastReadChapterNumber" to lastReadChapterNumber.coerceAtLeast(1),
                        "lastReadParagraphIndex" to lastReadParagraphIndex.coerceIn(0, 1000),
                        "completedChapterIds" to cleanCompleted,
                        "totalChapters" to safeTotal,
                        "progressFraction" to fraction,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- COMMUNITY DISCUSSION GROUPS & AUTHOR/READER POSTS ---

    fun observeDiscussionGroups(): Flow<List<NovelDiscussionGroup>> = flow {
        requireUserId()
        val path = "discussion_groups"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NovelDiscussionGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.memberCount }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getDiscussionGroups(): Result<List<NovelDiscussionGroup>> {
        val path = "discussion_groups"
        return try {
            requireUserId()
            val snapshot = db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NovelDiscussionGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createDiscussionGroup(
        novelId: String,
        novelTitle: String,
        authorName: String,
        groupTitle: String,
        description: String,
        genre: String
    ): Result<String> {
        val path = "discussion_groups"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "novelId" to novelId.take(128),
                "novelTitle" to novelTitle.trim().take(200),
                "authorId" to uid,
                "authorName" to authorName.trim().ifEmpty { "مؤلف روي" }.take(100),
                "groupTitle" to groupTitle.trim().take(200),
                "description" to description.trim().take(2000),
                "genre" to genre.trim().ifEmpty { "عام" }.take(60),
                "memberCount" to 1,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDiscussionGroupMemberCount(groupId: String, newCount: Int): Result<Unit> {
        val path = "discussion_groups/$groupId"
        return try {
            requireUserId()
            db.collection("discussion_groups").document(groupId).update(
                mapOf(
                    "memberCount" to newCount.coerceAtLeast(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeCommunityPosts(): Flow<List<CommunityPost>> = flow {
        requireUserId()
        val path = "community_posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCommunityPost(
        groupId: String,
        novelId: String,
        novelTitle: String,
        authorName: String,
        authorRole: String,
        postType: String,
        content: String,
        authorAnswer: String = "",
        answeredByName: String = ""
    ): Result<String> {
        val path = "community_posts"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "groupId" to groupId.ifBlank { "general" }.take(128),
                "novelId" to novelId.take(128),
                "novelTitle" to novelTitle.trim().take(200),
                "authorId" to uid,
                "authorName" to authorName.trim().ifEmpty { "عضو مجتمع روي" }.take(100),
                "authorRole" to if (authorRole == "مؤلف") "مؤلف" else "قارئ",
                "postType" to if (postType in listOf("AUTHOR_UPDATE", "QA_QUESTION", "GROUP_DISCUSSION")) postType else "GROUP_DISCUSSION",
                "content" to content.trim().take(3000),
                "authorAnswer" to authorAnswer.trim().take(3000),
                "answeredByName" to answeredByName.trim().take(100),
                "likesCount" to 0,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun answerCommunityQuestion(
        postId: String,
        answerText: String,
        answeredByName: String
    ): Result<Unit> {
        val path = "community_posts/$postId"
        return try {
            requireUserId()
            db.collection("community_posts").document(postId).update(
                mapOf(
                    "authorAnswer" to answerText.trim().take(3000),
                    "answeredByName" to answeredByName.trim().ifEmpty { "المؤلف" }.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementCommunityPostLike(postId: String, currentLikes: Int): Result<Unit> {
        val path = "community_posts/$postId"
        return try {
            requireUserId()
            db.collection("community_posts").document(postId).update(
                mapOf(
                    "likesCount" to (currentLikes + 1).coerceAtLeast(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
