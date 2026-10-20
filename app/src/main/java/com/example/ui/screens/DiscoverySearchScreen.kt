package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AuthorProfile
import com.example.data.model.FeaturedCommunityReader
import com.example.data.model.FollowSubscription
import com.example.data.model.Novel
import com.example.data.model.PersonalizedRecommendation
import com.example.data.model.RawiCatalogConstants
import com.example.data.model.ReadingProgress
import com.example.data.model.RecommendationLearningStats
import com.example.ui.viewmodel.CustomAlertItem
import com.example.ui.viewmodel.SearchFilterState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverySearchScreen(
    novelsState: UiState<List<Novel>>,
    personalizedRecommendations: List<PersonalizedRecommendation>,
    recommendationLearningStats: RecommendationLearningStats = RecommendationLearningStats(),
    preferredGenres: Set<String>,
    likedNovelIds: Set<String> = emptySet(),
    readingProgressList: List<ReadingProgress>,
    searchFilter: SearchFilterState,
    userFollows: List<FollowSubscription>,
    customAlerts: List<CustomAlertItem>,
    isDarkTheme: Boolean,
    userName: String,
    onToggleDarkTheme: () -> Unit,
    onOpenAlertsTab: () -> Unit,
    onOpenCommunityHub: () -> Unit = {},
    onTogglePreferredGenre: (String) -> Unit,
    onToggleLikeNovel: (Novel) -> Unit = {},
    onToggleFollowAuthor: (authorName: String, authorId: String, subtitle: String) -> Unit = { _, _, _ -> },
    onKeywordChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onAudienceChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onMinRatingChange: (Float) -> Unit,
    onToggleCompletedOnly: () -> Unit,
    onClearFilters: () -> Unit,
    onSelectNovel: (Novel) -> Unit,
    onOpenAuthorProfile: (authorName: String, authorId: String) -> Unit,
    onToggleFollowNovel: (Novel) -> Unit,
    onToggleFollowReader: (FeaturedCommunityReader) -> Unit,
    onSignOut: () -> Unit
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showPreferredGenresCustomize by remember { mutableStateOf(false) }
    val unreadAlertsCount = customAlerts.count { it.isUnread }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discovery_lazy_column"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Editorial Banner & Quick Actions
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "مكتبة روي العالمية",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFC89D54).copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "مرحباً، $userName",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF1F1A17),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onOpenAlertsTab,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        .testTag("alerts_shortcut_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadAlertsCount > 0) {
                                                Badge { Text(unreadAlertsCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "التنبيهات المخصصة",
                                            tint = Color(0xFFFFE5A3)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onToggleDarkTheme,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        .testTag("toggle_night_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "تبديل النمط الليلي",
                                        tint = Color.White
                                    )
                                }

                                TextButton(onClick = onSignOut) {
                                    Text(
                                        text = "خروج",
                                        color = Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "روي — عالم الروايات بلا حدود لغوية",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "اقرأ بالعربية ومختلف اللغات مع الترجمة الذكية الفورية، اختيار القراء الصوتيين، والتوصيات المخصصة لذوقك.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Reading Progress Tracker Section ("استئناف القراءة وتقدمك في الروايات")
        if (readingProgressList.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "متابعة القراءة وتقدمك في الروايات",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${readingProgressList.size} رواية قيد التتبع",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(readingProgressList, key = { it.id }) { progress ->
                            val percent = (progress.progressFraction * 100).toInt().coerceIn(0, 100)
                            ElevatedCard(
                                modifier = Modifier
                                    .width(290.dp)
                                    .testTag("reading_progress_card_${progress.novelId}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = progress.novelTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        BadgePill(
                                            text = "$percent% مكتمل",
                                            bg = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Text(
                                        text = "آخر موضع: الفصل #${progress.lastReadChapterNumber} • الفقرة #${progress.lastReadParagraphIndex + 1} (${progress.completedChapterIds.size}/${progress.totalChapters} فصول مقروءة)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )

                                    LinearProgressIndicator(
                                        progress = { progress.progressFraction.toFloat().coerceIn(0.05f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    if (novelsState is UiState.Success) {
                                        val targetNovel = novelsState.data.find { it.id == progress.novelId }
                                        if (targetNovel != null) {
                                            Button(
                                                onClick = { onSelectNovel(targetNovel) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("resume_novel_button_${progress.novelId}"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("استئناف القراءة من آخر موضع")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Personalized Recommendation System Section ("توصيات مخصصة لك")
        if (personalizedRecommendations.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personalized_recommendations_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFC89D54),
                                modifier = Modifier.size(22.dp)
                            )
                             Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "توصيات ذكية مخصصة تتعلم من تفاعلاتك",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "تقترح روايات جديدة بناءً على مفضلاتك، المؤلفين الذين تتابعهم، وتتعلم من إعجاباتك وتعليقاتك وإكمالك للروايات",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = { showPreferredGenresCustomize = !showPreferredGenresCustomize },
                            modifier = Modifier.testTag("customize_preferred_genres_button")
                        ) {
                            Text(if (showPreferredGenresCustomize) "إخفاء التفضيلات" else "تخصيص الأنواع")
                        }
                    }

                    // Adaptive Self-Learning Telemetry Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgePill(
                            text = "مفضلات وإعجابات: ${recommendationLearningStats.favoriteNovelsCount}",
                            bg = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        BadgePill(
                            text = "مؤلفون متابَعون: ${recommendationLearningStats.followedAuthorsCount}",
                            bg = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        BadgePill(
                            text = "أنواع مفضلة: ${recommendationLearningStats.preferredGenresCount}",
                            bg = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        BadgePill(
                            text = "روايات مكتملة: ${recommendationLearningStats.completedNovelsCount}",
                            bg = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BadgePill(
                            text = "إشارات التعلم (تعليقات/إعجابات): ${recommendationLearningStats.userInteractionsCount}",
                            bg = Color(0xFFC89D54).copy(alpha = 0.22f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = showPreferredGenresCustomize) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "اختر أنواعك الأدبية المفضلة لتحديث محرك التوصيات الذكي فوراً:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RawiCatalogConstants.GENRES.filter { it != "الكل" }.forEach { genre ->
                                        val isSelected = genre in preferredGenres
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onTogglePreferredGenre(genre) },
                                            label = { Text(genre) },
                                            modifier = Modifier.testTag("pref_genre_chip_$genre")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(personalizedRecommendations, key = { "rec_${it.novel.id}" }) { rec ->
                            val isLiked = rec.novel.id in likedNovelIds ||
                                userFollows.any { it.targetType == "NOVEL" && it.targetId == rec.novel.id && it.readingStatus == "FAVORITE" }
                            PersonalizedRecommendationCard(
                                recommendation = rec,
                                isLiked = isLiked,
                                onToggleLike = { onToggleLikeNovel(rec.novel) },
                                onSelectNovel = { onSelectNovel(rec.novel) },
                                onOpenAuthor = {
                                    onOpenAuthorProfile(rec.novel.authorName, rec.novel.authorId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Advanced Keyword Search & Multi-Facet Filter Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchFilter.keywordQuery,
                        onValueChange = onKeywordChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_keyword_input"),
                        placeholder = {
                            Text("ابحث بالكلمات المفتاحية، اسم الرواية، المؤلف، أو الوسوم...")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        },
                        trailingIcon = {
                            if (searchFilter.keywordQuery.isNotEmpty()) {
                                IconButton(onClick = { onKeywordChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    IconButton(
                        onClick = { showAdvancedFilters = !showAdvancedFilters },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (showAdvancedFilters) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("toggle_advanced_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "البحث المتقدم والفلاتر",
                            tint = if (showAdvancedFilters) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Keyword Suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كلمات مفتاحية شائعة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("قرطبة", "مخطوطات", "فضاء", "غرناطة", "دمشق", "فانتازيا", "Kepler").forEach { kw ->
                        AssistChip(
                            onClick = {
                                if (searchFilter.keywordQuery == kw) onKeywordChange("")
                                else onKeywordChange(kw)
                            },
                            label = { Text("#$kw") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (searchFilter.keywordQuery == kw)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                // Expandable Advanced Filters Panel
                AnimatedVisibility(visible = showAdvancedFilters) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "البحث المتقدم والتصنيف الدقيق",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = onClearFilters) {
                                    Text("إعادة ضبط الكل")
                                }
                            }

                            Text(
                                text = "الجمهور المستهدف:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RawiCatalogConstants.TARGET_AUDIENCES.forEach { audience ->
                                    FilterChip(
                                        selected = searchFilter.selectedAudience == audience,
                                        onClick = { onAudienceChange(audience) },
                                        label = { Text(audience) }
                                    )
                                }
                            }

                            Text(
                                text = "اللغة الأصلية للرواية:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (listOf("الكل") + RawiCatalogConstants.SUPPORTED_LANGUAGES).forEach { lang ->
                                    FilterChip(
                                        selected = searchFilter.selectedLanguage == lang,
                                        onClick = { onLanguageChange(lang) },
                                        label = { Text(lang) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الحد الأدنى للتقييم: ${String.format("%.1f", searchFilter.minimumRating)} ★",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                FilterChip(
                                    selected = searchFilter.onlyCompleted,
                                    onClick = onToggleCompletedOnly,
                                    label = { Text("الروايات المكتملة فقط") }
                                )
                            }

                            Slider(
                                value = searchFilter.minimumRating,
                                onValueChange = onMinRatingChange,
                                valueRange = 0f..4.5f,
                                steps = 8
                            )
                        }
                    }
                }
            }
        }

        // 5. Genre Horizontal Filter Strip
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تصفح حسب النوع الأدبي",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RawiCatalogConstants.GENRES) { genre ->
                        FilterChip(
                            selected = searchFilter.selectedGenre == genre,
                            onClick = { onGenreChange(genre) },
                            label = { Text(genre) },
                            modifier = Modifier.testTag("genre_chip_$genre")
                        )
                    }
                }
            }
        }

        // 6. Featured Authors Strip (Quick Access to Author Profiles)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملفات المؤلفين وأعمالهم المنشورة",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "اضغط لعرض السيرة والروابط",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(RawiCatalogConstants.CURATED_AUTHOR_BIOS.values.toList(), key = { it.id }) { author ->
                        ElevatedCard(
                            modifier = Modifier
                                .width(245.dp)
                                .clickable { onOpenAuthorProfile(author.displayName, author.authorId) }
                                .testTag("author_profile_card_${author.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = author.displayName.take(1),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = author.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = author.specialtyGenres.joinToString(" • "),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = author.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "عرض السيرة الذاتية والأعمال والروابط ←",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Reader Selection Section (Community Readers & Audio Narrators Preview)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اختيار القراء والنقاد المميزين",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "تابع ترشيحاتهم ومراجعاتهم",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(RawiCatalogConstants.FEATURED_READERS, key = { it.id }) { reader ->
                        val isFollowed = userFollows.any { it.targetType == "READER" && it.targetId == reader.id }
                        ElevatedCard(
                            modifier = Modifier.width(275.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reader.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = reader.roleTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleFollowReader(reader) },
                                        modifier = Modifier.testTag("follow_reader_${reader.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFollowed) Icons.Default.HowToReg else Icons.Default.PersonAdd,
                                            contentDescription = "متابعة القارئ",
                                            tint = if (isFollowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = reader.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "يرشح: ${reader.recommendedNovelTitle}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${reader.reviewsCount} مراجعة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Novels Library Catalog List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المكتبة العالمية والروايات المنشورة",
                    style = MaterialTheme.typography.titleLarge
                )
                if (novelsState is UiState.Success) {
                    Text(
                        text = "${novelsState.data.size} رواية",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        when (novelsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = novelsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (novelsState.data.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "لا توجد روايات مطابقة لمعايير البحث الحالية",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "جرب تغيير الكلمات المفتاحية أو إعادة ضبط الفلاتر لعرض جميع الروايات.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onClearFilters) {
                                    Text("إظهار كافة الروايات")
                                }
                            }
                        }
                    }
                } else {
                    items(novelsState.data, key = { it.id }) { novel ->
                        val isFollowed = userFollows.any { it.targetType == "NOVEL" && it.targetId == novel.id }
                        val novelProgress = readingProgressList.find { it.novelId == novel.id }
                        NovelCatalogCard(
                            novel = novel,
                            isFollowed = isFollowed,
                            readingProgress = novelProgress,
                            onSelect = { onSelectNovel(novel) },
                            onOpenAuthor = { onOpenAuthorProfile(novel.authorName, novel.authorId) },
                            onToggleFollow = { onToggleFollowNovel(novel) },
                            onKeywordClick = { kw -> onKeywordChange(kw) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizedRecommendationCard(
    recommendation: PersonalizedRecommendation,
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {},
    onSelectNovel: () -> Unit,
    onOpenAuthor: () -> Unit
) {
    val novel = recommendation.novel
    val coverColor = remember(novel.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(novel.coverColorHex))
        } catch (e: Exception) {
            Color(0xFF8C2D19)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .width(305.dp)
            .clickable(onClick = onSelectNovel)
            .testTag("recommendation_card_${novel.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFC89D54).copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFC89D54),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${recommendation.matchScorePercent}% تطابق مع ذوقك",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("like_rec_novel_btn_${novel.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "إعجاب بالرواية لتحسين التوصيات",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFC89D54),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format("%.1f", if (novel.averageRating > 0) novel.averageRating else 5.0),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 78.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(coverColor, Color(0xFF1A1412))
                            )
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = novel.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "المؤلف: ${novel.authorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenAuthor() }
                    )
                    Text(
                        text = recommendation.reasonAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (recommendation.matchingFactors.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recommendation.matchingFactors.forEach { factor ->
                        BadgePill(
                            text = factor,
                            bg = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovelCatalogCard(
    novel: Novel,
    isFollowed: Boolean,
    readingProgress: ReadingProgress? = null,
    onSelect: () -> Unit,
    onOpenAuthor: () -> Unit,
    onToggleFollow: () -> Unit,
    onKeywordClick: (String) -> Unit
) {
    val coverColor = remember(novel.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(novel.coverColorHex))
        } catch (e: Exception) {
            Color(0xFF8C2D19)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onSelect)
            .testTag("novel_card_${novel.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Literary Book Cover (2:3 ratio)
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(144.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(coverColor, coverColor.copy(alpha = 0.75f), Color(0xFF1A1412))
                        )
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = novel.originalLanguage,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFE5A3),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = novel.genre,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            // Novel Metadata & Interactive Controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = novel.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenAuthor() }
                                .padding(vertical = 2.dp)
                                .testTag("open_author_button_${novel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "ملف المؤلف",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تأليف: ${novel.authorName} (عرض الملف)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFollow,
                        modifier = Modifier.testTag("follow_novel_button_${novel.id}")
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isFollowed) "إلغاء المتابعة" else "متابعة الرواية وتفعيل التنبيهات",
                            tint = if (isFollowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Multi-badges: Genre, Target Audience, Language, Translation badge
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BadgePill(text = novel.genre, bg = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    BadgePill(text = "الجمهور: ${novel.targetAudience}", bg = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    BadgePill(
                        text = if (novel.isCompleted) "مكتملة (${novel.chapterCount} فصول)" else "مستمرة (${novel.chapterCount} فصول)",
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = novel.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Visual Reading Progress Indicator if started
                if (readingProgress != null) {
                    val pct = (readingProgress.progressFraction * 100).toInt().coerceIn(0, 100)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تقدم القراءة: الفصل #${readingProgress.lastReadChapterNumber} (الفقرة #${readingProgress.lastReadParagraphIndex + 1})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { readingProgress.progressFraction.toFloat().coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Granular Rating Row + AI Translation Ready Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFC89D54),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (novel.ratingCount > 0) "${String.format("%.1f", novel.averageRating)} (${novel.ratingCount})" else "جديد",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ترجمة فورية + قراءة صوتية",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Keywords / Tags
                if (novel.keywords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        novel.keywords.take(4).forEach { kw ->
                            Text(
                                text = "#$kw",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onKeywordClick(kw) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("read_novel_button_${novel.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (readingProgress != null) "متابعة القراءة والترجمة الفورية" else "قراءة الفصول والترجمة الفورية")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthorProfileDialog(
    authorProfile: AuthorProfile,
    publishedWorks: List<Novel>,
    isFollowed: Boolean = false,
    onToggleFollowAuthor: () -> Unit = {},
    onOpenCommunityHub: () -> Unit = {},
    onDismiss: () -> Unit,
    onSelectNovel: (Novel) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = authorProfile.displayName.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = authorProfile.displayName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (authorProfile.specialtyGenres.isNotEmpty()) {
                            Text(
                                text = authorProfile.specialtyGenres.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                FilterChip(
                    selected = isFollowed,
                    onClick = onToggleFollowAuthor,
                    label = { Text(if (isFollowed) "متابَع" else "متابعة") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFollowed) Icons.Default.HowToReg else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("author_profile_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "السيرة الذاتية للمؤلف:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = authorProfile.bio,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Social Media & Website Links
                if (authorProfile.websiteUrl.isNotBlank() ||
                    authorProfile.twitterHandle.isNotBlank() ||
                    authorProfile.instagramHandle.isNotBlank()
                ) {
                    HorizontalDivider()
                    Text(
                        text = "روابط التواصل مع المؤلف:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (authorProfile.websiteUrl.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    runCatching { uriHandler.openUri(authorProfile.websiteUrl) }
                                },
                                label = { Text("الموقع الرسمي") },
                                leadingIcon = {
                                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        if (authorProfile.twitterHandle.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    val clean = authorProfile.twitterHandle.removePrefix("@")
                                    runCatching { uriHandler.openUri("https://x.com/$clean") }
                                },
                                label = { Text("X: ${authorProfile.twitterHandle}") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        if (authorProfile.instagramHandle.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    val clean = authorProfile.instagramHandle.removePrefix("@")
                                    runCatching { uriHandler.openUri("https://instagram.com/$clean") }
                                },
                                label = { Text("Instagram: ${authorProfile.instagramHandle}") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                // Published Works in App
                HorizontalDivider()
                Text(
                    text = "الأعمال المنشورة في التطبيق (${publishedWorks.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (publishedWorks.isEmpty()) {
                    Text(
                        text = "لا توجد أعمال منشورة حالياً لهذا المؤلف.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        publishedWorks.forEach { work ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onSelectNovel(work)
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = work.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${work.genre} • ${work.originalLanguage} • ${work.chapterCount} فصول",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "قراءة ←",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenCommunityHub()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مجموعات نقاش وتحديثات المؤلف في المجتمع")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun BadgePill(
    text: String,
    bg: Color,
    contentColor: Color
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
