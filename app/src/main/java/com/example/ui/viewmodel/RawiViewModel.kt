package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiTranslationService
import com.example.data.audio.NarratorTtsManager
import com.example.data.audio.TtsPlaybackState
import com.example.data.local.OfflineChapterEntity
import com.example.data.local.OfflineChapterRepository
import com.example.data.model.AuthorProfile
import com.example.data.model.Chapter
import com.example.data.model.CommunityPost
import com.example.data.model.FollowSubscription
import com.example.data.model.NarratorProfile
import com.example.data.model.Novel
import com.example.data.model.NovelDiscussionGroup
import com.example.data.model.NovelReview
import com.example.data.model.ParagraphComment
import com.example.data.model.PersonalizedRecommendation
import com.example.data.model.RawiCatalogConstants
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingProgress
import com.example.data.model.RecommendationLearningStats
import com.example.data.model.TranslationDisplayMode
import com.example.data.repository.RawiFirestoreRepository
import com.example.data.seed.RawiSeedCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SearchFilterState(
    val keywordQuery: String = "",
    val selectedGenre: String = "الكل",
    val selectedAudience: String = "الكل",
    val selectedLanguage: String = "الكل",
    val minimumRating: Float = 0f,
    val onlyCompleted: Boolean = false
)

data class ReaderSettingsState(
    val themeMode: ReaderThemeMode = ReaderThemeMode.DAY_PAPER,
    val fontSizeSp: Int = 19,
    val useClassicSerifFont: Boolean = true,
    val targetTranslationLanguage: String = "العربية",
    val translationDisplayMode: TranslationDisplayMode = TranslationDisplayMode.BILINGUAL_SIDE_BY_SIDE,
    val isTranslating: Boolean = false,
    val translatedTitle: String = "",
    val translatedParagraphs: Map<Int, String> = emptyMap(),
    val statusBannerMessage: String? = null
)

data class CustomAlertItem(
    val id: String,
    val novelId: String,
    val title: String,
    val message: String,
    val isUnread: Boolean = true
)

class RawiViewModel(
    private val firestoreRepository: RawiFirestoreRepository,
    private val offlineRepository: OfflineChapterRepository,
    private val translationService: GeminiTranslationService,
    private val ttsManager: NarratorTtsManager,
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

    private val _isDarkAppTheme = MutableStateFlow(false)
    val isDarkAppTheme: StateFlow<Boolean> = _isDarkAppTheme.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter.asStateFlow()

    private val _readerSettings = MutableStateFlow(ReaderSettingsState())
    val readerSettings: StateFlow<ReaderSettingsState> = _readerSettings.asStateFlow()

    val ttsState: StateFlow<TtsPlaybackState> = ttsManager.state

    private val _selectedNovel = MutableStateFlow<Novel?>(null)
    val selectedNovel: StateFlow<Novel?> = _selectedNovel.asStateFlow()

    private val _selectedChapter = MutableStateFlow<Chapter?>(null)
    val selectedChapter: StateFlow<Chapter?> = _selectedChapter.asStateFlow()

    private val _chaptersForSelectedNovel = MutableStateFlow<List<Chapter>>(emptyList())
    val chaptersForSelectedNovel: StateFlow<List<Chapter>> = _chaptersForSelectedNovel.asStateFlow()

    private val _commentsForSelectedChapter = MutableStateFlow<List<ParagraphComment>>(emptyList())
    val commentsForSelectedChapter: StateFlow<List<ParagraphComment>> = _commentsForSelectedChapter.asStateFlow()

    private val _reviewsForSelectedNovel = MutableStateFlow<List<NovelReview>>(emptyList())
    val reviewsForSelectedNovel: StateFlow<List<NovelReview>> = _reviewsForSelectedNovel.asStateFlow()

    private val _inspectedAuthorProfile = MutableStateFlow<AuthorProfile?>(null)
    val inspectedAuthorProfile: StateFlow<AuthorProfile?> = _inspectedAuthorProfile.asStateFlow()

    private var novelChaptersJob: Job? = null
    private var novelReviewsJob: Job? = null
    private var chapterCommentsJob: Job? = null

    val novelsState: StateFlow<UiState<List<Novel>>> = firestoreRepository.observePublicNovels()
        .combine(_searchFilter) { novels, filter ->
            val filtered = novels.filter { novel ->
                val q = filter.keywordQuery.trim().lowercase()
                val matchesKeyword = q.isEmpty() ||
                    novel.title.lowercase().contains(q) ||
                    novel.synopsis.lowercase().contains(q) ||
                    novel.authorName.lowercase().contains(q) ||
                    novel.keywords.any { it.lowercase().contains(q) }
                val matchesGenre = filter.selectedGenre == "الكل" || novel.genre == filter.selectedGenre
                val matchesAudience = filter.selectedAudience == "الكل" || novel.targetAudience == filter.selectedAudience
                val matchesLang = filter.selectedLanguage == "الكل" || novel.originalLanguage == filter.selectedLanguage
                val matchesRating = novel.averageRating >= filter.minimumRating
                val matchesCompleted = !filter.onlyCompleted || novel.isCompleted

                matchesKeyword && matchesGenre && matchesAudience && matchesLang && matchesRating && matchesCompleted
            }
            UiState.Success(filtered) as UiState<List<Novel>>
        }
        .catch { error ->
            Log.w("RawiVM", "Error observing novels", error)
            emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل قائمة الروايات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val allNovelsUnfiltered: StateFlow<List<Novel>> = firestoreRepository.observePublicNovels()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val userFollows: StateFlow<List<FollowSubscription>> = firestoreRepository.observeUserFollows()
        .catch { error ->
            Log.w("RawiVM", "Error observing follows", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val readingProgressList: StateFlow<List<ReadingProgress>> = firestoreRepository.observeUserReadingProgress()
        .catch { error ->
            Log.w("RawiVM", "Error observing reading progress", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val authorProfiles: StateFlow<List<AuthorProfile>> = firestoreRepository.observePublicAuthorProfiles()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val discussionGroups: StateFlow<List<NovelDiscussionGroup>> = firestoreRepository.observeDiscussionGroups()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val communityPosts: StateFlow<List<CommunityPost>> = firestoreRepository.observeCommunityPosts()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val offlineChapters: StateFlow<List<OfflineChapterEntity>> = offlineRepository.allOfflineChapters
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredGenres: StateFlow<Set<String>> = _preferredGenres.asStateFlow()

    // Self-learning interaction signals (Likes, Comments, Reviews, Discussions)
    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _likedNovelIds = MutableStateFlow<Set<String>>(emptySet())
    val likedNovelIds: StateFlow<Set<String>> = _likedNovelIds.asStateFlow()

    private val _commentedNovelIds = MutableStateFlow<Set<String>>(emptySet())
    val commentedNovelIds: StateFlow<Set<String>> = _commentedNovelIds.asStateFlow()

    fun togglePreferredGenre(genre: String) {
        if (genre == "الكل") return
        val current = _preferredGenres.value.toMutableSet()
        if (genre in current) {
            current.remove(genre)
        } else {
            current.add(genre)
        }
        _preferredGenres.value = current
    }

    val recommendationLearningStats: StateFlow<RecommendationLearningStats> = combine(
        userFollows,
        readingProgressList,
        _preferredGenres,
        _likedPostIds,
        combine(_likedNovelIds, _commentedNovelIds, communityPosts) { likedNovels, commentedNovels, posts ->
            Triple(likedNovels, commentedNovels, posts)
        }
    ) { follows, progressList, explicitGenres, likedPosts, (likedNovels, commentedNovels, posts) ->
        val favNovels = follows.count { it.targetType == "NOVEL" && it.readingStatus == "FAVORITE" } + likedNovels.size
        val followedAuthors = follows.count { it.targetType == "AUTHOR" }
        val completedNovels = (
            progressList.filter { it.progressFraction >= 1.0 }.map { it.novelId } +
                follows.filter { it.targetType == "NOVEL" && it.readingStatus == "COMPLETED" }.map { it.targetId }
            ).distinct().size
        val userCommunityInteractions = posts.count { it.authorId == currentUserId } +
            likedPosts.size +
            likedNovels.size +
            commentedNovels.size +
            progressList.sumOf { it.completedChapterIds.size }
        val inferredGenresCount = (
            explicitGenres +
                progressList.map { it.genre }.filter { it.isNotBlank() }
            ).distinct().size

        RecommendationLearningStats(
            favoriteNovelsCount = favNovels,
            followedAuthorsCount = followedAuthors,
            preferredGenresCount = inferredGenresCount,
            completedNovelsCount = completedNovels,
            userInteractionsCount = userCommunityInteractions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = RecommendationLearningStats()
    )

    // Self-Learning Personalized Recommendations Engine based on Favorite Novels, Preferred Genres,
    // Followed Authors, and User Interactions (Likes, Comments, and Completed Novels)
    val personalizedRecommendations: StateFlow<List<PersonalizedRecommendation>> = combine(
        allNovelsUnfiltered,
        readingProgressList,
        userFollows,
        _preferredGenres,
        combine(_likedNovelIds, _commentedNovelIds, communityPosts) { likedNovels, commentedNovels, posts ->
            Triple(likedNovels, commentedNovels, posts)
        }
    ) { novels, progressList, follows, explicitGenres, (likedNovels, commentedNovels, posts) ->
        if (novels.isEmpty()) return@combine emptyList()

        // 1. Favorite & Followed Novels
        val favoriteNovelIds = follows
            .filter { it.targetType == "NOVEL" && it.readingStatus == "FAVORITE" }
            .map { it.targetId }
            .toSet() + likedNovels
        val followedNovelIds = follows
            .filter { it.targetType == "NOVEL" }
            .map { it.targetId }
            .toSet() + favoriteNovelIds

        // 2. Followed Authors (by ID or Name)
        val followedAuthorIds = follows.filter { it.targetType == "AUTHOR" }.map { it.targetId }.toSet()
        val followedAuthorNames = follows.filter { it.targetType == "AUTHOR" }.map { it.targetName.lowercase() }.toSet()

        // 3. Completed Novels & Reading History
        val completedNovelIds = (
            progressList.filter { it.progressFraction >= 1.0 }.map { it.novelId } +
                follows.filter { it.targetType == "NOVEL" && it.readingStatus == "COMPLETED" }.map { it.targetId }
            ).toSet()
        val inProgressNovelIds = progressList.filter { it.progressFraction in 0.01..0.99 }.map { it.novelId }.toSet()

        // 4. Interaction Signals (Comments, Community Discussions, Likes)
        val userCommunityNovelIds = posts.filter { it.authorId == currentUserId && it.novelId.isNotBlank() }
            .map { it.novelId }
            .toSet()
        val interactedNovelIds = likedNovels + commentedNovels + userCommunityNovelIds
        val interactedGenres = novels.filter { it.id in interactedNovelIds }.map { it.genre }
        val completedGenres = novels.filter { it.id in completedNovelIds }.map { it.genre }
        val favoriteGenres = novels.filter { it.id in favoriteNovelIds }.map { it.genre }

        // 5. Ranked Preferred Genres combining explicit selection + favorites + completions + interactions
        val historyGenres = progressList.map { it.genre }.filter { it.isNotBlank() }
        val followedGenres = novels.filter { it.id in followedNovelIds }.map { it.genre }
        val rankedGenres = (
            explicitGenres.toList() + explicitGenres.toList() +
                favoriteGenres + completedGenres + interactedGenres +
                historyGenres + followedGenres
            )
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        novels
            .sortedByDescending { it.id !in completedNovelIds }
            .map { novel ->
                val isExplicitPreferred = novel.genre in explicitGenres
                val isFavoriteGenre = novel.genre in favoriteGenres
                val isCompletedGenre = novel.genre in completedGenres
                val isInteractedGenre = novel.genre in interactedGenres
                val isHistoryGenre = novel.genre in historyGenres
                val isFollowedAuthor = (novel.authorId.isNotBlank() && novel.authorId in followedAuthorIds) ||
                    novel.authorName.lowercase() in followedAuthorNames ||
                    novels.any { it.id in favoriteNovelIds && it.authorName.equals(novel.authorName, ignoreCase = true) }

                val genreBonus = when {
                    isExplicitPreferred -> 20
                    rankedGenres.firstOrNull() == novel.genre -> 18
                    novel.genre in rankedGenres -> 14
                    else -> 6
                }
                val authorFollowBonus = if (isFollowedAuthor) 12 else 0
                val interactionLearningBonus = when {
                    novel.id in interactedNovelIds -> 10
                    isInteractedGenre && isCompletedGenre -> 9
                    isInteractedGenre || isCompletedGenre || isFavoriteGenre -> 7
                    else -> 2
                }
                val ratingBonus = (if (novel.averageRating > 0.0) novel.averageRating * 11.5 else 50.0)
                    .toInt()
                    .coerceIn(10, 58)
                val noveltyBonus = if (novel.id !in completedNovelIds) 4 else 0

                val score = (genreBonus + authorFollowBonus + interactionLearningBonus + ratingBonus + noveltyBonus)
                    .coerceIn(78, 99)

                val factors = mutableListOf<String>()
                if (isFollowedAuthor) {
                    factors.add("للمؤلف المتابَع: ${novel.authorName}")
                }
                if (isExplicitPreferred) {
                    factors.add("نوعك المفضل: ${novel.genre}")
                } else if (isFavoriteGenre) {
                    factors.add("مشابه لرواياتك المفضلة (${novel.genre})")
                } else if (isCompletedGenre) {
                    factors.add("تعلّم من إكمالك لـ (${novel.genre})")
                } else if (isInteractedGenre) {
                    factors.add("تعلّم من إعجاباتك وتعليقاتك")
                } else if (isHistoryGenre) {
                    factors.add("من سجل قراءتك (${novel.genre})")
                } else {
                    factors.add("تصنيف: ${novel.genre}")
                }

                if (novel.id in interactedNovelIds || isInteractedGenre) {
                    factors.add("تحسين ذكي من تفاعلاتك")
                } else if (novel.averageRating >= 4.0) {
                    factors.add("تقييم القراء ${String.format("%.1f", novel.averageRating)}★")
                }

                val reason = when {
                    isFollowedAuthor && (isExplicitPreferred || isFavoriteGenre) ->
                        "لأنك تتابع الكاتب «${novel.authorName}» وتفضل روايات «${novel.genre}»"
                    isFollowedAuthor ->
                        "إصدار مميز للمؤلف الذي تتابعه أو تفضل أعماله «${novel.authorName}»"
                    isCompletedGenre ->
                        "تعلّم النظام من إكمالك لروايات سابقة في «${novel.genre}» وتفاعلاتك الإيجابية"
                    isInteractedGenre || novel.id in interactedNovelIds ->
                        "اقتراح ذكي تطوّر بناءً على إعجاباتك وتعليقاتك ومناقشاتك في «${novel.genre}»"
                    isExplicitPreferred ->
                        "لأنك اخترت «${novel.genre}» ضمن أنواعك الأدبية المفضلة وتقييمها ${String.format("%.1f", if (novel.averageRating > 0) novel.averageRating else 5.0)}★"
                    isFavoriteGenre || isHistoryGenre ->
                        "مشابهة لرواياتك المفضلة وسجل قراءتك في تصنيف «${novel.genre}»"
                    else ->
                        "ترشيح مخصص لذوقك القرائي في الروايات العالمية (${novel.originalLanguage})"
                }

                PersonalizedRecommendation(
                    novel = novel,
                    matchScorePercent = score,
                    reasonAr = reason,
                    matchingFactors = factors.distinct().take(3)
                )
            }
            .sortedByDescending { it.matchScorePercent }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val customAlerts: StateFlow<List<CustomAlertItem>> = combine(
        allNovelsUnfiltered,
        userFollows
    ) { novels, follows ->
        val alerts = mutableListOf<CustomAlertItem>()
        for (follow in follows) {
            if (!follow.notificationsEnabled) continue
            if (follow.targetType == "NOVEL") {
                val matchingNovel = novels.find { it.id == follow.targetId }
                if (matchingNovel != null) {
                    val newChapters = matchingNovel.chapterCount - follow.lastKnownChapterCount
                    if (newChapters > 0) {
                        alerts.add(
                            CustomAlertItem(
                                id = "alert_${follow.id}",
                                novelId = matchingNovel.id,
                                title = "فصل جديد في «${matchingNovel.title}»",
                                message = "نشر الكاتب ${matchingNovel.authorName} $newChapters فصل/فصول جديدة (إجمالي ${matchingNovel.chapterCount} فصول).",
                                isUnread = true
                            )
                        )
                    } else {
                        alerts.add(
                            CustomAlertItem(
                                id = "alert_synced_${follow.id}",
                                novelId = matchingNovel.id,
                                title = "متابعة نشطة: «${matchingNovel.title}»",
                                message = "التنبيهات الفورية مفعّلة لهذه الرواية (${matchingNovel.chapterCount} فصول منشورة).",
                                isUnread = false
                            )
                        )
                    }
                }
            } else if (follow.targetType == "AUTHOR" || follow.targetType == "READER") {
                alerts.add(
                    CustomAlertItem(
                        id = "alert_community_${follow.id}",
                        novelId = "",
                        title = "تنبيهات ${if (follow.targetType == "AUTHOR") "المؤلف" else "القارئ"}: ${follow.targetName}",
                        message = "تصلك إشعارات فورية عند نشر أعمال أو مراجعات نقدية جديدة.",
                        isUnread = false
                    )
                )
            }
        }
        alerts
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            RawiSeedCatalog.seedInitialCatalogIfEmpty(firestoreRepository)
        }
    }

    fun toggleAppDarkTheme() {
        val next = !_isDarkAppTheme.value
        _isDarkAppTheme.value = next
        _readerSettings.value = _readerSettings.value.copy(
            themeMode = if (next) ReaderThemeMode.VELVET_NIGHT else ReaderThemeMode.DAY_PAPER
        )
    }

    // --- AUTHOR PROFILE INSPECTION & EDITING ---

    fun openAuthorProfile(authorName: String, authorId: String = "") {
        val cloudMatch = authorProfiles.value.find {
            (authorId.isNotBlank() && it.authorId == authorId) ||
                it.displayName.equals(authorName, ignoreCase = true)
        }
        val curatedMatch = RawiCatalogConstants.CURATED_AUTHOR_BIOS[authorName]
        val resolved = cloudMatch ?: curatedMatch ?: AuthorProfile(
            id = authorId.ifBlank { authorName },
            authorId = authorId.ifBlank { authorName },
            displayName = authorName,
            bio = "مؤلف وروائي ينشر أعماله الأدبية عبر منصة روي متعدّدة اللغات.",
            specialtyGenres = allNovelsUnfiltered.value
                .filter { it.authorName == authorName }
                .map { it.genre }
                .distinct(),
            websiteUrl = "https://rawi.app/authors/${authorName.replace(" ", "-")}",
            twitterHandle = "@${authorName.replace(" ", "_").take(15)}",
            instagramHandle = "@${authorName.replace(" ", ".").take(15)}"
        )
        _inspectedAuthorProfile.value = resolved
    }

    fun dismissAuthorProfile() {
        _inspectedAuthorProfile.value = null
    }

    fun saveMyAuthorProfile(
        displayName: String,
        bio: String,
        specialtyGenres: List<String>,
        websiteUrl: String,
        twitterHandle: String,
        instagramHandle: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = firestoreRepository.saveAuthorProfile(
                displayName = displayName,
                bio = bio,
                specialtyGenres = specialtyGenres,
                websiteUrl = websiteUrl,
                twitterHandle = twitterHandle,
                instagramHandle = instagramHandle
            )
            if (res.isSuccess) {
                onComplete(true, "تم حفظ ملفك الشخصي كمؤلف وروابط التواصل الاجتماعي بنجاح!")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ ملف المؤلف")
            }
        }
    }

    // --- READING PROGRESS TRACKING ---

    fun saveReadingPosition(paragraphIndex: Int) {
        val novel = _selectedNovel.value ?: return
        val chapter = _selectedChapter.value ?: return
        val existingProgress = readingProgressList.value.find { it.novelId == novel.id }
        val completedIds = existingProgress?.completedChapterIds ?: emptyList()
        val total = chaptersForSelectedNovel.value.size.coerceAtLeast(novel.chapterCount.coerceAtLeast(1))

        viewModelScope.launch {
            firestoreRepository.saveReadingProgress(
                novelId = novel.id,
                novelTitle = novel.title,
                genre = novel.genre,
                lastReadChapterId = chapter.id,
                lastReadChapterNumber = chapter.chapterNumber,
                lastReadParagraphIndex = paragraphIndex,
                completedChapterIds = completedIds,
                totalChapters = total
            )
            offlineRepository.updateProgress(chapter.id, paragraphIndex)
            _readerSettings.value = _readerSettings.value.copy(
                statusBannerMessage = "تم حفظ موضع القراءة عند الفقرة #${paragraphIndex + 1} من «${chapter.title}»"
            )
        }
    }

    fun toggleMarkCurrentChapterRead() {
        val novel = _selectedNovel.value ?: return
        val chapter = _selectedChapter.value ?: return
        val existingProgress = readingProgressList.value.find { it.novelId == novel.id }
        val currentCompleted = existingProgress?.completedChapterIds?.toMutableSet() ?: mutableSetOf()
        val isNowRead = if (chapter.id in currentCompleted) {
            currentCompleted.remove(chapter.id)
            false
        } else {
            currentCompleted.add(chapter.id)
            true
        }
        val total = chaptersForSelectedNovel.value.size.coerceAtLeast(novel.chapterCount.coerceAtLeast(1))

        viewModelScope.launch {
            firestoreRepository.saveReadingProgress(
                novelId = novel.id,
                novelTitle = novel.title,
                genre = novel.genre,
                lastReadChapterId = chapter.id,
                lastReadChapterNumber = chapter.chapterNumber,
                lastReadParagraphIndex = existingProgress?.lastReadParagraphIndex ?: 0,
                completedChapterIds = currentCompleted.toList(),
                totalChapters = total
            )
            _readerSettings.value = _readerSettings.value.copy(
                statusBannerMessage = if (isNowRead)
                    "تم تحديد «${chapter.title}» كمقروء (${currentCompleted.size}/$total فصول)"
                else
                    "تم إلغاء تحديد «${chapter.title}» كمقروء"
            )
        }
    }

    // --- SEARCH & FILTER ---

    fun updateKeywordQuery(query: String) {
        _searchFilter.value = _searchFilter.value.copy(keywordQuery = query)
    }

    fun updateGenreFilter(genre: String) {
        _searchFilter.value = _searchFilter.value.copy(selectedGenre = genre)
    }

    fun updateAudienceFilter(audience: String) {
        _searchFilter.value = _searchFilter.value.copy(selectedAudience = audience)
    }

    fun updateLanguageFilter(language: String) {
        _searchFilter.value = _searchFilter.value.copy(selectedLanguage = language)
    }

    fun updateMinimumRating(rating: Float) {
        _searchFilter.value = _searchFilter.value.copy(minimumRating = rating)
    }

    fun toggleOnlyCompleted() {
        _searchFilter.value = _searchFilter.value.copy(onlyCompleted = !_searchFilter.value.onlyCompleted)
    }

    fun clearSearchFilters() {
        _searchFilter.value = SearchFilterState()
    }

    // --- NOVEL & CHAPTER SELECTION ---

    fun selectNovel(novel: Novel) {
        _selectedNovel.value = novel
        novelChaptersJob?.cancel()
        novelReviewsJob?.cancel()

        val savedProgress = readingProgressList.value.find { it.novelId == novel.id }

        novelChaptersJob = viewModelScope.launch {
            firestoreRepository.observeChaptersForNovel(novel.id)
                .catch { Log.w("RawiVM", "Failed to observe chapters", it) }
                .collect { chapters ->
                    _chaptersForSelectedNovel.value = chapters
                    if (_selectedChapter.value == null || _selectedChapter.value?.novelId != novel.id) {
                        val resumeChapter = savedProgress?.let { prog ->
                            chapters.find { it.id == prog.lastReadChapterId || it.chapterNumber == prog.lastReadChapterNumber }
                        } ?: chapters.firstOrNull()
                        if (resumeChapter != null) {
                            selectChapter(resumeChapter)
                        }
                    }
                }
        }

        novelReviewsJob = viewModelScope.launch {
            firestoreRepository.observeReviewsForNovel(novel.id)
                .catch { Log.w("RawiVM", "Failed to observe reviews", it) }
                .collect { reviews ->
                    _reviewsForSelectedNovel.value = reviews
                }
        }
    }

    fun selectChapter(chapter: Chapter) {
        ttsManager.stop()
        _selectedChapter.value = chapter
        _readerSettings.value = _readerSettings.value.copy(
            translatedTitle = "",
            translatedParagraphs = emptyMap(),
            isTranslating = false
        )
        chapterCommentsJob?.cancel()
        chapterCommentsJob = viewModelScope.launch {
            firestoreRepository.observeCommentsForChapter(chapter.id)
                .catch { Log.w("RawiVM", "Failed to observe comments", it) }
                .collect { comments ->
                    _commentsForSelectedChapter.value = comments
                }
        }
        viewModelScope.launch {
            val offline = offlineRepository.getById(chapter.id)
            val cachedTrans = offline?.getTranslatedParagraphsList()
            if (cachedTrans != null && offline.translatedLanguage != null) {
                val map = cachedTrans.mapIndexed { index, s -> index to s }.toMap()
                _readerSettings.value = _readerSettings.value.copy(
                    targetTranslationLanguage = offline.translatedLanguage,
                    translatedParagraphs = map
                )
            }
        }
    }

    fun openOfflineChapterForReading(offlineEntity: OfflineChapterEntity) {
        ttsManager.stop()
        val syntheticNovel = allNovelsUnfiltered.value.find { it.id == offlineEntity.novelId }
            ?: Novel(
                id = offlineEntity.novelId,
                title = offlineEntity.novelTitle,
                authorName = offlineEntity.authorName,
                genre = offlineEntity.genre,
                originalLanguage = offlineEntity.originalLanguage,
                chapterCount = offlineEntity.chapterNumber
            )
        _selectedNovel.value = syntheticNovel

        val chapter = Chapter(
            id = offlineEntity.chapterId,
            novelId = offlineEntity.novelId,
            chapterNumber = offlineEntity.chapterNumber,
            title = offlineEntity.chapterTitle,
            paragraphs = offlineEntity.getParagraphsList(),
            language = offlineEntity.originalLanguage,
            wordCount = offlineEntity.getParagraphsList().sumOf { it.split(" ").size }
        )
        selectChapter(chapter)
    }

    // --- INSTANT AI TRANSLATION ---

    fun setTargetTranslationLanguage(language: String) {
        _readerSettings.value = _readerSettings.value.copy(targetTranslationLanguage = language)
    }

    fun setTranslationDisplayMode(mode: TranslationDisplayMode) {
        _readerSettings.value = _readerSettings.value.copy(translationDisplayMode = mode)
    }

    fun translateCurrentChapterNow() {
        val chapter = _selectedChapter.value ?: return
        val targetLang = _readerSettings.value.targetTranslationLanguage
        _readerSettings.value = _readerSettings.value.copy(
            isTranslating = true,
            statusBannerMessage = "جاري الترجمة الفورية للفصل إلى $targetLang..."
        )
        viewModelScope.launch {
            val result = translationService.translateChapterParagraphs(
                chapterTitle = chapter.title,
                paragraphs = chapter.paragraphs,
                sourceLanguage = chapter.language,
                targetLanguage = targetLang
            )
            result.onSuccess { payload ->
                val map = payload.translatedParagraphs.mapIndexed { idx, text -> idx to text }.toMap()
                _readerSettings.value = _readerSettings.value.copy(
                    isTranslating = false,
                    translatedTitle = payload.translatedTitle,
                    translatedParagraphs = map,
                    statusBannerMessage = "تمت الترجمة الفورية إلى $targetLang بنجاح"
                )
            }.onFailure {
                _readerSettings.value = _readerSettings.value.copy(
                    isTranslating = false,
                    statusBannerMessage = "تعذرت الترجمة الفورية حالياً"
                )
            }
        }
    }

    fun translateSingleParagraphAt(index: Int) {
        val chapter = _selectedChapter.value ?: return
        val paragraphText = chapter.paragraphs.getOrNull(index) ?: return
        val targetLang = _readerSettings.value.targetTranslationLanguage
        _readerSettings.value = _readerSettings.value.copy(isTranslating = true)
        viewModelScope.launch {
            val translated = translationService.translateSingleParagraph(
                paragraph = paragraphText,
                sourceLanguage = chapter.language,
                targetLanguage = targetLang
            )
            val updatedMap = _readerSettings.value.translatedParagraphs.toMutableMap()
            updatedMap[index] = translated
            _readerSettings.value = _readerSettings.value.copy(
                isTranslating = false,
                translatedParagraphs = updatedMap,
                statusBannerMessage = "تمت ترجمة الفقرة #${index + 1} إلى $targetLang"
            )
        }
    }

    // --- READER APPEARANCE & NIGHT MODE ---

    fun setReaderThemeMode(mode: ReaderThemeMode) {
        _readerSettings.value = _readerSettings.value.copy(themeMode = mode)
    }

    fun adjustReaderFontSize(delta: Int) {
        val next = (_readerSettings.value.fontSizeSp + delta).coerceIn(14, 28)
        _readerSettings.value = _readerSettings.value.copy(fontSizeSp = next)
    }

    fun toggleReaderFontStyle() {
        _readerSettings.value = _readerSettings.value.copy(
            useClassicSerifFont = !_readerSettings.value.useClassicSerifFont
        )
    }

    fun clearStatusBanner() {
        _readerSettings.value = _readerSettings.value.copy(statusBannerMessage = null)
    }

    // --- AUDIO NARRATOR (TTS) ---

    fun selectNarrator(profile: NarratorProfile) {
        ttsManager.selectNarratorProfile(profile)
    }

    fun setNarratorSpeed(multiplier: Float) {
        ttsManager.setCustomSpeedMultiplier(multiplier)
    }

    fun toggleAudioNarration(startFromParagraph: Int = 0, readTranslatedIfAvailable: Boolean = false) {
        if (ttsState.value.isPlaying) {
            ttsManager.stop()
            return
        }
        val chapter = _selectedChapter.value ?: return
        val translatedMap = _readerSettings.value.translatedParagraphs
        val useTranslated = readTranslatedIfAvailable && translatedMap.isNotEmpty()
        val paragraphsToRead = if (useTranslated) {
            chapter.paragraphs.mapIndexed { index, original -> translatedMap[index] ?: original }
        } else {
            chapter.paragraphs
        }
        val lang = if (useTranslated) _readerSettings.value.targetTranslationLanguage else chapter.language
        ttsManager.startReading(paragraphsToRead, startFromParagraph, lang)
    }

    fun stopAudioNarration() {
        ttsManager.stop()
    }

    // --- PARAGRAPH SIDE COMMENTS ---

    fun addSideComment(paragraphIndex: Int, commentText: String) {
        val novel = _selectedNovel.value ?: return
        val chapter = _selectedChapter.value ?: return
        if (commentText.isBlank()) return

        _commentedNovelIds.value = _commentedNovelIds.value + novel.id
        viewModelScope.launch {
            firestoreRepository.addParagraphComment(
                novelId = novel.id,
                chapterId = chapter.id,
                paragraphIndex = paragraphIndex,
                userName = currentUserName,
                commentText = commentText
            )
        }
    }

    // --- MULTI-CRITERIA REVIEW SUBMISSION ---

    fun submitReview(
        plotRating: Int,
        styleRating: Int,
        charactersRating: Int,
        translationRating: Int,
        reviewText: String
    ) {
        val novel = _selectedNovel.value ?: return
        if (reviewText.isBlank()) return
        _commentedNovelIds.value = _commentedNovelIds.value + novel.id
        if ((plotRating + styleRating + charactersRating + translationRating) / 4.0 >= 4.0) {
            _likedNovelIds.value = _likedNovelIds.value + novel.id
        }
        viewModelScope.launch {
            val res = firestoreRepository.submitNovelReview(
                novel = novel,
                userName = currentUserName,
                plotRating = plotRating,
                styleRating = styleRating,
                charactersRating = charactersRating,
                translationRating = translationRating,
                reviewText = reviewText
            )
            if (res.isSuccess) {
                _readerSettings.value = _readerSettings.value.copy(
                    statusBannerMessage = "تم نشر تقييمك الدقيق وتحديث نموذج التوصيات الذكي!"
                )
            }
        }
    }

    // --- OFFLINE DOWNLOADS ---

    fun downloadCurrentChapterForOffline() {
        val novel = _selectedNovel.value ?: return
        val chapter = _selectedChapter.value ?: return
        val translatedMap = _readerSettings.value.translatedParagraphs
        val translatedList = if (translatedMap.isNotEmpty()) {
            chapter.paragraphs.mapIndexed { idx, orig -> translatedMap[idx] ?: orig }
        } else null

        viewModelScope.launch {
            offlineRepository.saveChapterForOffline(
                chapterId = chapter.id,
                novelId = novel.id,
                novelTitle = novel.title,
                authorName = novel.authorName,
                genre = novel.genre,
                chapterNumber = chapter.chapterNumber,
                chapterTitle = chapter.title,
                originalLanguage = chapter.language,
                paragraphs = chapter.paragraphs,
                translatedParagraphs = translatedList,
                translatedLanguage = if (translatedList != null) _readerSettings.value.targetTranslationLanguage else null
            )
            _readerSettings.value = _readerSettings.value.copy(
                statusBannerMessage = "تم حفظ «${chapter.title}» للقراءة بدون إنترنت"
            )
        }
    }

    fun downloadAllNovelChaptersOffline() {
        val novel = _selectedNovel.value ?: return
        val chapters = _chaptersForSelectedNovel.value
        if (chapters.isEmpty()) return
        viewModelScope.launch {
            for (ch in chapters) {
                offlineRepository.saveChapterForOffline(
                    chapterId = ch.id,
                    novelId = novel.id,
                    novelTitle = novel.title,
                    authorName = novel.authorName,
                    genre = novel.genre,
                    chapterNumber = ch.chapterNumber,
                    chapterTitle = ch.title,
                    originalLanguage = ch.language,
                    paragraphs = ch.paragraphs
                )
            }
            _readerSettings.value = _readerSettings.value.copy(
                statusBannerMessage = "تم تحميل جميع فصول «${novel.title}» (${chapters.size} فصول) للقراءة بدون إنترنت"
            )
        }
    }

    fun deleteOfflineChapter(chapterId: String) {
        viewModelScope.launch {
            offlineRepository.removeOfflineChapter(chapterId)
        }
    }

    // --- FOLLOWS, LIBRARY SHELVES & CUSTOM ALERTS ---

    fun toggleFollowNovel(novel: Novel, readingStatus: String = "READING") {
        val existing = userFollows.value.find { it.targetType == "NOVEL" && it.targetId == novel.id }
        viewModelScope.launch {
            if (existing != null) {
                firestoreRepository.removeFollowSubscription(existing.id)
            } else {
                firestoreRepository.upsertFollowSubscription(
                    targetId = novel.id,
                    targetType = "NOVEL",
                    targetName = novel.title,
                    targetSubtitle = novel.authorName,
                    notificationsEnabled = true,
                    lastKnownChapterCount = novel.chapterCount,
                    readingStatus = readingStatus
                )
            }
        }
    }

    fun updateNovelShelfStatus(follow: FollowSubscription, newStatus: String) {
        viewModelScope.launch {
            firestoreRepository.updateFollowNotificationAndStatus(
                followId = follow.id,
                notificationsEnabled = follow.notificationsEnabled,
                readingStatus = newStatus,
                lastKnownChapterCount = follow.lastKnownChapterCount
            )
        }
    }

    fun toggleFollowNotification(follow: FollowSubscription) {
        viewModelScope.launch {
            val matchingNovel = allNovelsUnfiltered.value.find { it.id == follow.targetId }
            firestoreRepository.updateFollowNotificationAndStatus(
                followId = follow.id,
                notificationsEnabled = !follow.notificationsEnabled,
                readingStatus = follow.readingStatus,
                lastKnownChapterCount = matchingNovel?.chapterCount ?: follow.lastKnownChapterCount
            )
        }
    }

    fun markFollowAlertAsSeen(follow: FollowSubscription, currentChapterCount: Int) {
        viewModelScope.launch {
            firestoreRepository.updateFollowNotificationAndStatus(
                followId = follow.id,
                notificationsEnabled = follow.notificationsEnabled,
                readingStatus = follow.readingStatus,
                lastKnownChapterCount = currentChapterCount
            )
        }
    }

    fun toggleFollowCommunityReader(readerId: String, readerName: String, roleTitle: String) {
        val existing = userFollows.value.find { it.targetType == "READER" && it.targetId == readerId }
        viewModelScope.launch {
            if (existing != null) {
                firestoreRepository.removeFollowSubscription(existing.id)
            } else {
                firestoreRepository.upsertFollowSubscription(
                    targetId = readerId,
                    targetType = "READER",
                    targetName = readerName,
                    targetSubtitle = roleTitle,
                    notificationsEnabled = true,
                    lastKnownChapterCount = 0,
                    readingStatus = "FAVORITE"
                )
            }
        }
    }

    fun toggleFollowAuthor(authorName: String, authorId: String = "", subtitle: String = "مؤلف في منصة روي") {
        val cleanTargetId = (authorId.ifBlank { authorName })
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "author_${authorName.hashCode().toString().replace("-", "")}" }
            .take(64)
        val existing = userFollows.value.find {
            it.targetType == "AUTHOR" && (it.targetId == cleanTargetId || it.targetName.equals(authorName, ignoreCase = true))
        }
        viewModelScope.launch {
            if (existing != null) {
                firestoreRepository.removeFollowSubscription(existing.id)
            } else {
                firestoreRepository.upsertFollowSubscription(
                    targetId = cleanTargetId,
                    targetType = "AUTHOR",
                    targetName = authorName,
                    targetSubtitle = subtitle,
                    notificationsEnabled = true,
                    lastKnownChapterCount = 0,
                    readingStatus = "FAVORITE"
                )
            }
        }
    }

    fun toggleFavoriteNovelLike(novel: Novel) {
        val currentLikes = _likedNovelIds.value.toMutableSet()
        val isNowLiked = if (novel.id in currentLikes) {
            currentLikes.remove(novel.id)
            false
        } else {
            currentLikes.add(novel.id)
            true
        }
        _likedNovelIds.value = currentLikes

        viewModelScope.launch {
            val existing = userFollows.value.find { it.targetType == "NOVEL" && it.targetId == novel.id }
            if (isNowLiked) {
                firestoreRepository.upsertFollowSubscription(
                    targetId = novel.id,
                    targetType = "NOVEL",
                    targetName = novel.title,
                    targetSubtitle = novel.authorName,
                    notificationsEnabled = existing?.notificationsEnabled ?: true,
                    lastKnownChapterCount = novel.chapterCount,
                    readingStatus = "FAVORITE"
                )
            } else if (existing != null && existing.readingStatus == "FAVORITE") {
                firestoreRepository.updateFollowNotificationAndStatus(
                    followId = existing.id,
                    notificationsEnabled = existing.notificationsEnabled,
                    readingStatus = "READING",
                    lastKnownChapterCount = existing.lastKnownChapterCount
                )
            }
        }
    }

    // --- COMMUNITY HUB: DISCUSSION GROUPS, AUTHOR UPDATES, Q&A & DISCUSSIONS ---

    fun toggleJoinDiscussionGroup(group: NovelDiscussionGroup) {
        val existing = userFollows.value.find { it.targetType == "GROUP" && it.targetId == group.id }
        viewModelScope.launch {
            if (existing != null) {
                firestoreRepository.removeFollowSubscription(existing.id)
                firestoreRepository.updateDiscussionGroupMemberCount(group.id, (group.memberCount - 1).coerceAtLeast(1))
            } else {
                firestoreRepository.upsertFollowSubscription(
                    targetId = group.id,
                    targetType = "GROUP",
                    targetName = group.groupTitle,
                    targetSubtitle = "${group.novelTitle} • ${group.authorName}",
                    notificationsEnabled = true,
                    lastKnownChapterCount = 0,
                    readingStatus = "FAVORITE"
                )
                firestoreRepository.updateDiscussionGroupMemberCount(group.id, group.memberCount + 1)
                if (group.novelId.isNotBlank()) {
                    _likedNovelIds.value = _likedNovelIds.value + group.novelId
                }
            }
        }
    }

    fun createNovelDiscussionGroup(
        novel: Novel?,
        groupTitle: String,
        description: String,
        genre: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (groupTitle.isBlank() || description.isBlank()) {
            onComplete(false, "يرجى إدخال اسم مجموعة النقاش ووصفها")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepository.createDiscussionGroup(
                novelId = novel?.id ?: "general",
                novelTitle = novel?.title ?: "نقاش أدبي عام",
                authorName = currentUserName,
                groupTitle = groupTitle,
                description = description,
                genre = novel?.genre ?: genre
            )
            val groupId = res.getOrNull()
            if (groupId != null) {
                firestoreRepository.upsertFollowSubscription(
                    targetId = groupId,
                    targetType = "GROUP",
                    targetName = groupTitle,
                    targetSubtitle = "${novel?.title ?: "نقاش عام"} • $currentUserName",
                    notificationsEnabled = true,
                    lastKnownChapterCount = 0,
                    readingStatus = "FAVORITE"
                )
                onComplete(true, "تم إنشاء مجموعة النقاش «$groupTitle» وانضمامك إليها بنجاح!")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء مجموعة النقاش")
            }
        }
    }

    fun publishCommunityPost(
        groupId: String,
        novel: Novel?,
        authorRole: String,
        postType: String,
        content: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (content.isBlank()) {
            onComplete(false, "يرجى كتابة محتوى المشاركة أولاً")
            return
        }
        if (novel != null) {
            _commentedNovelIds.value = _commentedNovelIds.value + novel.id
        }
        viewModelScope.launch {
            val res = firestoreRepository.createCommunityPost(
                groupId = groupId.ifBlank { "general" },
                novelId = novel?.id ?: "",
                novelTitle = novel?.title ?: "مجتمع روي العام",
                authorName = currentUserName,
                authorRole = authorRole,
                postType = postType,
                content = content
            )
            if (res.isSuccess) {
                onComplete(true, "تم نشر مشاركتك في مجتمع روي وتحديث توصياتك الذكية!")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "تعذر نشر المشاركة")
            }
        }
    }

    fun answerReaderQuestion(
        post: CommunityPost,
        answerText: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (answerText.isBlank()) {
            onComplete(false, "يرجى كتابة الإجابة أولاً")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepository.answerCommunityQuestion(
                postId = post.id,
                answerText = answerText,
                answeredByName = currentUserName
            )
            if (res.isSuccess) {
                onComplete(true, "تم نشر إجابة المؤلف على السؤال بنجاح!")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ الإجابة")
            }
        }
    }

    fun likeCommunityPost(post: CommunityPost) {
        if (post.id in _likedPostIds.value) return
        _likedPostIds.value = _likedPostIds.value + post.id
        if (post.novelId.isNotBlank()) {
            _likedNovelIds.value = _likedNovelIds.value + post.novelId
        }
        viewModelScope.launch {
            firestoreRepository.incrementCommunityPostLike(post.id, post.likesCount)
        }
    }

    // --- AUTHOR STUDIO: PUBLISH NOVEL & CHAPTER ---

    fun publishNewNovelWithFirstChapter(
        title: String,
        synopsis: String,
        originalLanguage: String,
        genre: String,
        targetAudience: String,
        keywordsRaw: String,
        coverColorHex: String,
        firstChapterTitle: String,
        firstChapterContent: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val keywords = keywordsRaw.split(",", "،", " ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(10)
            val novelRes = firestoreRepository.createNovel(
                authorName = currentUserName,
                title = title,
                synopsis = synopsis,
                originalLanguage = originalLanguage,
                genre = genre,
                targetAudience = targetAudience,
                keywords = keywords,
                coverColorHex = coverColorHex,
                isCompleted = false
            )
            val novelId = novelRes.getOrNull()
            if (novelId == null) {
                onComplete(false, novelRes.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء الرواية")
                return@launch
            }

            val paragraphs = firstChapterContent
                .split(Regex("\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(firstChapterContent.trim()) }

            val chRes = firestoreRepository.createChapter(
                novelId = novelId,
                chapterNumber = 1,
                title = firstChapterTitle.ifBlank { "الفصل الأول" },
                paragraphs = paragraphs,
                language = originalLanguage
            )
            if (chRes.isSuccess) {
                firestoreRepository.updateNovelChapterCount(novelId, 1, false)
                firestoreRepository.upsertFollowSubscription(
                    targetId = novelId,
                    targetType = "NOVEL",
                    targetName = title,
                    targetSubtitle = currentUserName,
                    notificationsEnabled = true,
                    lastKnownChapterCount = 1,
                    readingStatus = "READING"
                )
                onComplete(true, "تم نشر رواية «$title» والفصل الأول بنجاح!")
            } else {
                onComplete(false, chRes.exceptionOrNull()?.localizedMessage ?: "تعذر نشر الفصل الأول")
            }
        }
    }

    fun publishAdditionalChapter(
        novel: Novel,
        chapterTitle: String,
        chapterContent: String,
        markNovelCompleted: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val paragraphs = chapterContent
                .split(Regex("\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(chapterContent.trim()) }
            val nextNumber = novel.chapterCount + 1
            val chRes = firestoreRepository.createChapter(
                novelId = novel.id,
                chapterNumber = nextNumber,
                title = chapterTitle.ifBlank { "الفصل $nextNumber" },
                paragraphs = paragraphs,
                language = novel.originalLanguage
            )
            if (chRes.isSuccess) {
                firestoreRepository.updateNovelChapterCount(novel.id, nextNumber, markNovelCompleted)
                onComplete(true, "تم نشر الفصل #$nextNumber من «${novel.title}» بنجاح!")
            } else {
                onComplete(false, chRes.exceptionOrNull()?.localizedMessage ?: "تعذر نشر الفصل")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
