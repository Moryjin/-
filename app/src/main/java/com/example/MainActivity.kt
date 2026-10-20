package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.GeminiTranslationService
import com.example.data.audio.NarratorTtsManager
import com.example.data.local.OfflineChapterRepository
import com.example.data.local.RawiOfflineDatabase
import com.example.data.repository.RawiFirestoreRepository
import com.example.ui.auth.RawiAuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.screens.AuthorProfileDialog
import com.example.ui.screens.AuthorStudioScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.DiscoverySearchScreen
import com.example.ui.screens.LibraryOfflineAlertsScreen
import com.example.ui.screens.NovelDetailAndReaderScreen
import com.example.ui.theme.RawiTheme
import com.example.ui.viewmodel.RawiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                RawiRootApp()
            }
        }
    }
}

@Composable
fun RawiRootApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        RawiTheme {
            RawiAuthScreen(onAuthSuccess = {})
        }
    } else {
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "قارئ ومؤلف روي"
        RawiAuthenticatedContent(
            currentUserId = user.uid,
            currentUserName = displayName
        )
    }
}

@Composable
fun RawiAuthenticatedContent(
    currentUserId: String,
    currentUserName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: RawiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val offlineDb = RawiOfflineDatabase.getInstance(app)
                RawiViewModel(
                    firestoreRepository = RawiFirestoreRepository(firestore),
                    offlineRepository = OfflineChapterRepository(offlineDb.offlineChapterDao()),
                    translationService = GeminiTranslationService(),
                    ttsManager = NarratorTtsManager(app),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName
                )
            }
        }
    )

    val isDarkAppTheme by viewModel.isDarkAppTheme.collectAsStateWithLifecycle()
    val novelsState by viewModel.novelsState.collectAsStateWithLifecycle()
    val allNovels by viewModel.allNovelsUnfiltered.collectAsStateWithLifecycle()
    val personalizedRecommendations by viewModel.personalizedRecommendations.collectAsStateWithLifecycle()
    val recommendationLearningStats by viewModel.recommendationLearningStats.collectAsStateWithLifecycle()
    val preferredGenres by viewModel.preferredGenres.collectAsStateWithLifecycle()
    val likedNovelIds by viewModel.likedNovelIds.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val readingProgressList by viewModel.readingProgressList.collectAsStateWithLifecycle()
    val authorProfiles by viewModel.authorProfiles.collectAsStateWithLifecycle()
    val discussionGroups by viewModel.discussionGroups.collectAsStateWithLifecycle()
    val communityPosts by viewModel.communityPosts.collectAsStateWithLifecycle()
    val inspectedAuthorProfile by viewModel.inspectedAuthorProfile.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val userFollows by viewModel.userFollows.collectAsStateWithLifecycle()
    val offlineChapters by viewModel.offlineChapters.collectAsStateWithLifecycle()
    val customAlerts by viewModel.customAlerts.collectAsStateWithLifecycle()
    val selectedNovel by viewModel.selectedNovel.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val chaptersForNovel by viewModel.chaptersForSelectedNovel.collectAsStateWithLifecycle()
    val commentsForChapter by viewModel.commentsForSelectedChapter.collectAsStateWithLifecycle()
    val reviewsForNovel by viewModel.reviewsForSelectedNovel.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableIntStateOf(0) }
    var librarySubTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadAlertsCount = customAlerts.count { it.isUnread }

    RawiTheme(darkTheme = isDarkAppTheme) {
        // Author Profile Modal Dialog whenever an author is inspected
        val activeAuthor = inspectedAuthorProfile
        if (activeAuthor != null) {
            val works = allNovels.filter {
                it.authorName.equals(activeAuthor.displayName, ignoreCase = true) ||
                    (activeAuthor.authorId.isNotBlank() && it.authorId == activeAuthor.authorId)
            }
            val isAuthorFollowed = userFollows.any {
                it.targetType == "AUTHOR" &&
                    (it.targetName.equals(activeAuthor.displayName, ignoreCase = true) ||
                        (activeAuthor.authorId.isNotBlank() && it.targetId == activeAuthor.authorId))
            }
            AuthorProfileDialog(
                authorProfile = activeAuthor,
                publishedWorks = works,
                isFollowed = isAuthorFollowed,
                onToggleFollowAuthor = {
                    viewModel.toggleFollowAuthor(
                        authorName = activeAuthor.displayName,
                        authorId = activeAuthor.authorId,
                        subtitle = activeAuthor.specialtyGenres.joinToString(" • ").ifBlank { "مؤلف في منصة روي" }
                    )
                },
                onOpenCommunityHub = {
                    currentDestination = 2
                },
                onDismiss = viewModel::dismissAuthorProfile,
                onSelectNovel = { novel ->
                    viewModel.selectNovel(novel)
                    currentDestination = 1
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentDestination == 0,
                                onClick = {
                                    viewModel.stopAudioNarration()
                                    currentDestination = 0
                                },
                                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                                label = { Text("اكتشف") },
                                modifier = Modifier.testTag("nav_discover")
                            )
                            NavigationBarItem(
                                selected = currentDestination == 1,
                                onClick = {
                                    if (selectedNovel == null && allNovels.isNotEmpty()) {
                                        viewModel.selectNovel(allNovels.first())
                                    }
                                    currentDestination = 1
                                },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                label = { Text("القارئ") },
                                modifier = Modifier.testTag("nav_reader")
                            )
                            NavigationBarItem(
                                selected = currentDestination == 2,
                                onClick = {
                                    viewModel.stopAudioNarration()
                                    currentDestination = 2
                                },
                                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                label = { Text("المجتمع") },
                                modifier = Modifier.testTag("nav_community")
                            )
                            NavigationBarItem(
                                selected = currentDestination == 3,
                                onClick = {
                                    viewModel.stopAudioNarration()
                                    currentDestination = 3
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unreadAlertsCount > 0) {
                                                Badge { Text(unreadAlertsCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Bookmarks, contentDescription = null)
                                    }
                                },
                                label = { Text("المكتبة") },
                                modifier = Modifier.testTag("nav_library")
                            )
                            NavigationBarItem(
                                selected = currentDestination == 4,
                                onClick = {
                                    viewModel.stopAudioNarration()
                                    currentDestination = 4
                                },
                                icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                label = { Text("المؤلف") },
                                modifier = Modifier.testTag("nav_studio")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = currentDestination == 0,
                                onClick = { currentDestination = 0 },
                                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                                label = { Text("اكتشف") }
                            )
                            NavigationRailItem(
                                selected = currentDestination == 1,
                                onClick = {
                                    if (selectedNovel == null && allNovels.isNotEmpty()) {
                                        viewModel.selectNovel(allNovels.first())
                                    }
                                    currentDestination = 1
                                },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                label = { Text("القارئ") }
                            )
                            NavigationRailItem(
                                selected = currentDestination == 2,
                                onClick = { currentDestination = 2 },
                                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                label = { Text("المجتمع") }
                            )
                            NavigationRailItem(
                                selected = currentDestination == 3,
                                onClick = { currentDestination = 3 },
                                icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                label = { Text("المكتبة") }
                            )
                            NavigationRailItem(
                                selected = currentDestination == 4,
                                onClick = { currentDestination = 4 },
                                icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                label = { Text("المؤلف") }
                            )
                        }
                    }

                    when (currentDestination) {
                        0 -> {
                            DiscoverySearchScreen(
                                novelsState = novelsState,
                                personalizedRecommendations = personalizedRecommendations,
                                recommendationLearningStats = recommendationLearningStats,
                                preferredGenres = preferredGenres,
                                likedNovelIds = likedNovelIds,
                                readingProgressList = readingProgressList,
                                searchFilter = searchFilter,
                                userFollows = userFollows,
                                customAlerts = customAlerts,
                                isDarkTheme = isDarkAppTheme,
                                userName = currentUserName,
                                onToggleDarkTheme = viewModel::toggleAppDarkTheme,
                                onOpenAlertsTab = {
                                    librarySubTab = 2
                                    currentDestination = 3
                                },
                                onOpenCommunityHub = {
                                    currentDestination = 2
                                },
                                onTogglePreferredGenre = viewModel::togglePreferredGenre,
                                onToggleLikeNovel = viewModel::toggleFavoriteNovelLike,
                                onToggleFollowAuthor = viewModel::toggleFollowAuthor,
                                onKeywordChange = viewModel::updateKeywordQuery,
                                onGenreChange = viewModel::updateGenreFilter,
                                onAudienceChange = viewModel::updateAudienceFilter,
                                onLanguageChange = viewModel::updateLanguageFilter,
                                onMinRatingChange = viewModel::updateMinimumRating,
                                onToggleCompletedOnly = viewModel::toggleOnlyCompleted,
                                onClearFilters = viewModel::clearSearchFilters,
                                onSelectNovel = { novel ->
                                    viewModel.selectNovel(novel)
                                    currentDestination = 1
                                },
                                onOpenAuthorProfile = { authorName, authorId ->
                                    viewModel.openAuthorProfile(authorName, authorId)
                                },
                                onToggleFollowNovel = { novel ->
                                    viewModel.toggleFollowNovel(novel)
                                },
                                onToggleFollowReader = { reader ->
                                    viewModel.toggleFollowCommunityReader(
                                        readerId = reader.id,
                                        readerName = reader.name,
                                        roleTitle = reader.roleTitle
                                    )
                                },
                                onSignOut = {
                                    viewModel.stopAudioNarration()
                                    signOut(context, credentialManager, {}, scope)
                                }
                            )
                        }

                        1 -> {
                            val activeNovel = selectedNovel ?: allNovels.firstOrNull()
                            if (activeNovel != null) {
                                val isFollowed = userFollows.any {
                                    it.targetType == "NOVEL" && it.targetId == activeNovel.id
                                }
                                val currentProgress = readingProgressList.find { it.novelId == activeNovel.id }
                                NovelDetailAndReaderScreen(
                                    novel = activeNovel,
                                    chapters = chaptersForNovel,
                                    selectedChapter = selectedChapter,
                                    commentsForChapter = commentsForChapter,
                                    reviewsForNovel = reviewsForNovel,
                                    offlineChapters = offlineChapters,
                                    readingProgress = currentProgress,
                                    isFollowed = isFollowed,
                                    readerSettings = readerSettings,
                                    ttsState = ttsState,
                                    onBack = {
                                        viewModel.stopAudioNarration()
                                        currentDestination = 0
                                    },
                                    onOpenAuthorProfile = {
                                        viewModel.openAuthorProfile(activeNovel.authorName, activeNovel.authorId)
                                    },
                                    onSelectChapter = viewModel::selectChapter,
                                    onToggleFollowNovel = { viewModel.toggleFollowNovel(activeNovel) },
                                    onToggleMarkChapterRead = viewModel::toggleMarkCurrentChapterRead,
                                    onSaveReadingPosition = viewModel::saveReadingPosition,
                                    onDownloadCurrentChapter = viewModel::downloadCurrentChapterForOffline,
                                    onDownloadAllChapters = viewModel::downloadAllNovelChaptersOffline,
                                    onSelectTargetLanguage = viewModel::setTargetTranslationLanguage,
                                    onTranslateChapterNow = viewModel::translateCurrentChapterNow,
                                    onTranslateSingleParagraph = viewModel::translateSingleParagraphAt,
                                    onChangeTranslationDisplayMode = viewModel::setTranslationDisplayMode,
                                    onSelectNarratorProfile = viewModel::selectNarrator,
                                    onChangeNarratorSpeed = viewModel::setNarratorSpeed,
                                    onToggleAudioNarration = viewModel::toggleAudioNarration,
                                    onStopAudioNarration = viewModel::stopAudioNarration,
                                    onChangeReaderTheme = viewModel::setReaderThemeMode,
                                    onAdjustFontSize = viewModel::adjustReaderFontSize,
                                    onToggleFontFamily = viewModel::toggleReaderFontStyle,
                                    onAddParagraphComment = viewModel::addSideComment,
                                    onSubmitReview = viewModel::submitReview,
                                    onDismissStatusBanner = viewModel::clearStatusBanner
                                )
                            } else {
                                DiscoverySearchScreen(
                                    novelsState = novelsState,
                                    personalizedRecommendations = personalizedRecommendations,
                                    recommendationLearningStats = recommendationLearningStats,
                                    preferredGenres = preferredGenres,
                                    likedNovelIds = likedNovelIds,
                                    readingProgressList = readingProgressList,
                                    searchFilter = searchFilter,
                                    userFollows = userFollows,
                                    customAlerts = customAlerts,
                                    isDarkTheme = isDarkAppTheme,
                                    userName = currentUserName,
                                    onToggleDarkTheme = viewModel::toggleAppDarkTheme,
                                    onOpenAlertsTab = {
                                        librarySubTab = 2
                                        currentDestination = 3
                                    },
                                    onOpenCommunityHub = {
                                        currentDestination = 2
                                    },
                                    onTogglePreferredGenre = viewModel::togglePreferredGenre,
                                    onToggleLikeNovel = viewModel::toggleFavoriteNovelLike,
                                    onToggleFollowAuthor = viewModel::toggleFollowAuthor,
                                    onKeywordChange = viewModel::updateKeywordQuery,
                                    onGenreChange = viewModel::updateGenreFilter,
                                    onAudienceChange = viewModel::updateAudienceFilter,
                                    onLanguageChange = viewModel::updateLanguageFilter,
                                    onMinRatingChange = viewModel::updateMinimumRating,
                                    onToggleCompletedOnly = viewModel::toggleOnlyCompleted,
                                    onClearFilters = viewModel::clearSearchFilters,
                                    onSelectNovel = { novel ->
                                        viewModel.selectNovel(novel)
                                        currentDestination = 1
                                    },
                                    onOpenAuthorProfile = { authorName, authorId ->
                                        viewModel.openAuthorProfile(authorName, authorId)
                                    },
                                    onToggleFollowNovel = viewModel::toggleFollowNovel,
                                    onToggleFollowReader = { reader ->
                                        viewModel.toggleFollowCommunityReader(
                                            readerId = reader.id,
                                            readerName = reader.name,
                                            roleTitle = reader.roleTitle
                                        )
                                    },
                                    onSignOut = {
                                        signOut(context, credentialManager, {}, scope)
                                    }
                                )
                            }
                        }

                        2 -> {
                            CommunityHubScreen(
                                allNovels = allNovels,
                                authorProfiles = authorProfiles,
                                discussionGroups = discussionGroups,
                                communityPosts = communityPosts,
                                userFollows = userFollows,
                                likedPostIds = likedPostIds,
                                currentUserId = currentUserId,
                                currentUserName = currentUserName,
                                onOpenAuthorProfile = { authorName, authorId ->
                                    viewModel.openAuthorProfile(authorName, authorId)
                                },
                                onToggleFollowAuthor = viewModel::toggleFollowAuthor,
                                onSaveAuthorProfile = viewModel::saveMyAuthorProfile,
                                onToggleJoinGroup = viewModel::toggleJoinDiscussionGroup,
                                onCreateDiscussionGroup = viewModel::createNovelDiscussionGroup,
                                onPublishCommunityPost = viewModel::publishCommunityPost,
                                onAnswerQuestion = viewModel::answerReaderQuestion,
                                onLikePost = viewModel::likeCommunityPost,
                                onSelectNovel = { novel ->
                                    viewModel.selectNovel(novel)
                                    currentDestination = 1
                                }
                            )
                        }

                        3 -> {
                            LibraryOfflineAlertsScreen(
                                initialSubTab = librarySubTab,
                                userFollows = userFollows,
                                allNovels = allNovels,
                                offlineChapters = offlineChapters,
                                customAlerts = customAlerts,
                                onOpenNovelById = { novelId ->
                                    val found = allNovels.find { it.id == novelId }
                                    if (found != null) {
                                        viewModel.selectNovel(found)
                                        currentDestination = 1
                                    }
                                },
                                onOpenOfflineChapter = { offlineEntity ->
                                    viewModel.openOfflineChapterForReading(offlineEntity)
                                    currentDestination = 1
                                },
                                onDeleteOfflineChapter = viewModel::deleteOfflineChapter,
                                onUpdateShelfStatus = viewModel::updateNovelShelfStatus,
                                onToggleNotification = viewModel::toggleFollowNotification,
                                onMarkAlertSeen = viewModel::markFollowAlertAsSeen
                            )
                        }

                        4 -> {
                            AuthorStudioScreen(
                                allNovels = allNovels,
                                authorProfiles = authorProfiles,
                                currentUserId = currentUserId,
                                currentUserName = currentUserName,
                                onPublishNewNovel = viewModel::publishNewNovelWithFirstChapter,
                                onPublishAdditionalChapter = viewModel::publishAdditionalChapter,
                                onSaveAuthorProfile = viewModel::saveMyAuthorProfile
                            )
                        }
                    }
                }
            }
        }
    }
}
