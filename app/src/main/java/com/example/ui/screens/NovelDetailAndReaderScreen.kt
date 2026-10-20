package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.TtsPlaybackState
import com.example.data.local.OfflineChapterEntity
import com.example.data.model.Chapter
import com.example.data.model.NarratorProfile
import com.example.data.model.Novel
import com.example.data.model.NovelReview
import com.example.data.model.ParagraphComment
import com.example.data.model.RawiCatalogConstants
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingProgress
import com.example.data.model.TranslationDisplayMode
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CairoFontFamily
import com.example.ui.viewmodel.ReaderSettingsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovelDetailAndReaderScreen(
    novel: Novel,
    chapters: List<Chapter>,
    selectedChapter: Chapter?,
    commentsForChapter: List<ParagraphComment>,
    reviewsForNovel: List<NovelReview>,
    offlineChapters: List<OfflineChapterEntity>,
    readingProgress: ReadingProgress? = null,
    isFollowed: Boolean,
    readerSettings: ReaderSettingsState,
    ttsState: TtsPlaybackState,
    onBack: () -> Unit,
    onOpenAuthorProfile: () -> Unit = {},
    onSelectChapter: (Chapter) -> Unit,
    onToggleFollowNovel: () -> Unit,
    onToggleMarkChapterRead: () -> Unit = {},
    onSaveReadingPosition: (Int) -> Unit = {},
    onDownloadCurrentChapter: () -> Unit,
    onDownloadAllChapters: () -> Unit,
    onSelectTargetLanguage: (String) -> Unit,
    onTranslateChapterNow: () -> Unit,
    onTranslateSingleParagraph: (Int) -> Unit,
    onChangeTranslationDisplayMode: (TranslationDisplayMode) -> Unit,
    onSelectNarratorProfile: (NarratorProfile) -> Unit,
    onChangeNarratorSpeed: (Float) -> Unit,
    onToggleAudioNarration: (Int, Boolean) -> Unit,
    onStopAudioNarration: () -> Unit,
    onChangeReaderTheme: (ReaderThemeMode) -> Unit,
    onAdjustFontSize: (Int) -> Unit,
    onToggleFontFamily: () -> Unit,
    onAddParagraphComment: (Int, String) -> Unit,
    onSubmitReview: (Int, Int, Int, Int, String) -> Unit,
    onDismissStatusBanner: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showTranslationPanel by remember { mutableStateOf(true) }
    var showNarratorPanel by remember { mutableStateOf(false) }
    var showAppearancePanel by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var activeCommentParagraphIndex by remember { mutableStateOf<Int?>(null) }
    var readTranslatedAudio by remember { mutableStateOf(false) }

    val readerBgColor = Color(readerSettings.themeMode.bgHex)
    val readerTextColor = Color(readerSettings.themeMode.textHex)
    val readerCardColor = Color(readerSettings.themeMode.cardHex)
    val activeFontFamily = if (readerSettings.useClassicSerifFont) AmiriFontFamily else CairoFontFamily

    val isCurrentChapterDownloaded = selectedChapter != null &&
        offlineChapters.any { it.chapterId == selectedChapter.id }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = readerBgColor
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_lazy_column"),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Reader Navigation & Novel Actions Bar
            item {
                Surface(
                    color = readerCardColor,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("reader_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "العودة للمكتبة",
                                        tint = readerTextColor
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = novel.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = readerTextColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${novel.authorName} (ملف المؤلف) • ${novel.genre} • ${novel.targetAudience}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFC89D54),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clickable { onOpenAuthorProfile() }
                                            .testTag("reader_open_author_profile_button")
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onDownloadCurrentChapter,
                                    modifier = Modifier.testTag("download_chapter_button")
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentChapterDownloaded) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                        contentDescription = "تحميل الفصل للقراءة بدون إنترنت",
                                        tint = if (isCurrentChapterDownloaded) Color(0xFF2D6A4F) else readerTextColor
                                    )
                                }
                                IconButton(
                                    onClick = onToggleFollowNovel,
                                    modifier = Modifier.testTag("reader_follow_novel_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFollowed) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "متابعة الرواية",
                                        tint = if (isFollowed) Color(0xFFC89D54) else readerTextColor
                                    )
                                }
                            }
                        }

                        // Tool Toggles Row: Instant AI Translation | Audio Narrator | Night/Appearance | Multi-Criteria Rating
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showTranslationPanel,
                                onClick = { showTranslationPanel = !showTranslationPanel },
                                label = { Text("الترجمة الفورية") },
                                leadingIcon = {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("toggle_translation_panel_chip")
                            )
                            FilterChip(
                                selected = showNarratorPanel || ttsState.isPlaying,
                                onClick = { showNarratorPanel = !showNarratorPanel },
                                label = {
                                    Text(if (ttsState.isPlaying) "يقرأ الآن (${ttsState.selectedNarrator.nameAr})" else "اختيار القارئ الصوتي")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("toggle_narrator_panel_chip")
                            )
                            FilterChip(
                                selected = showAppearancePanel,
                                onClick = { showAppearancePanel = !showAppearancePanel },
                                label = { Text("النمط الليلي والخط") },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("toggle_appearance_panel_chip")
                            )
                            FilterChip(
                                selected = showReviewDialog,
                                onClick = { showReviewDialog = true },
                                label = { Text("تقييم دقيق (${String.format("%.1f", novel.averageRating)}★)") },
                                leadingIcon = {
                                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("open_review_dialog_button")
                            )
                        }
                    }
                }
            }

            // Status Banner Toast inside Reader
            if (readerSettings.statusBannerMessage != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFC89D54).copy(alpha = 0.22f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFC89D54),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = readerSettings.statusBannerMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = readerTextColor
                                )
                            }
                            IconButton(onClick = onDismissStatusBanner) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = readerTextColor)
                            }
                        }
                    }
                }
            }

            // 2. Reading Progress Bar, Mark Chapter as Read & Chapter Selector Strip
            item {
                val totalChaptersCount = chapters.size.coerceAtLeast(novel.chapterCount.coerceAtLeast(1))
                val completedIds = readingProgress?.completedChapterIds ?: emptyList()
                val isCurrentChapterCompleted = selectedChapter != null && selectedChapter.id in completedIds
                val progressFraction = readingProgress?.progressFraction?.toFloat()
                    ?: (completedIds.size.toFloat() / totalChaptersCount.toFloat())
                val progressPercent = (progressFraction * 100).toInt().coerceIn(0, 100)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_progress_tracker_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = readerCardColor)
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
                                        text = "تقدم القراءة: $progressPercent% مكتمل (${completedIds.size}/$totalChaptersCount فصول)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = readerTextColor
                                    )
                                    if (readingProgress != null) {
                                        Text(
                                            text = "آخر موضع محفوظ: الفصل #${readingProgress.lastReadChapterNumber} • الفقرة #${readingProgress.lastReadParagraphIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = readerTextColor.copy(alpha = 0.75f)
                                        )
                                    }
                                }

                                if (selectedChapter != null) {
                                    FilterChip(
                                        selected = isCurrentChapterCompleted,
                                        onClick = onToggleMarkChapterRead,
                                        label = {
                                            Text(if (isCurrentChapterCompleted) "مقروء ✓" else "تحديد الفصل كمقروء")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("mark_chapter_read_button")
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFC89D54),
                                trackColor = readerTextColor.copy(alpha = 0.15f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الفصول المنشورة (${chapters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = readerTextColor
                        )
                        TextButton(
                            onClick = onDownloadAllChapters,
                            modifier = Modifier.testTag("download_all_chapters_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحميل كل الفصول بدون إنترنت")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chapters.forEach { chapter ->
                            val isSelected = selectedChapter?.id == chapter.id
                            val isChapterRead = chapter.id in completedIds
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectChapter(chapter) },
                                label = {
                                    Text(
                                        if (isChapterRead) "✓ ${chapter.chapterNumber}. ${chapter.title}"
                                        else "${chapter.chapterNumber}. ${chapter.title}"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. Instant AI Translation Control Panel
            item {
                AnimatedVisibility(visible = showTranslationPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = readerCardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الترجمة الذكية الفورية للمحتوى (Gemini AI)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = readerTextColor
                                )
                                Text(
                                    text = "الأصل: ${selectedChapter?.language ?: novel.originalLanguage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = readerTextColor.copy(alpha = 0.7f)
                                )
                            }

                            Text(
                                text = "اختر لغة الترجمة الفورية:",
                                style = MaterialTheme.typography.labelLarge,
                                color = readerTextColor
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RawiCatalogConstants.SUPPORTED_LANGUAGES.forEach { lang ->
                                    FilterChip(
                                        selected = readerSettings.targetTranslationLanguage == lang,
                                        onClick = { onSelectTargetLanguage(lang) },
                                        label = { Text(lang) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TranslationDisplayMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = readerSettings.translationDisplayMode == mode,
                                        onClick = { onChangeTranslationDisplayMode(mode) },
                                        label = { Text(mode.labelAr) }
                                    )
                                }
                            }

                            Button(
                                onClick = onTranslateChapterNow,
                                enabled = !readerSettings.isTranslating && selectedChapter != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translate_chapter_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (readerSettings.isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري الترجمة الفورية...")
                                } else {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ترجمة الفصل كاملاً إلى ${readerSettings.targetTranslationLanguage}")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Audio Narrator Selection Panel ("اختيار القراء الصوتيين")
            item {
                AnimatedVisibility(visible = showNarratorPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = readerCardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "اختيار القارئ الصوتي التفاعلي وتظليل الفقرات",
                                style = MaterialTheme.typography.titleMedium,
                                color = readerTextColor
                            )

                            RawiCatalogConstants.NARRATOR_PROFILES.forEach { narrator ->
                                val isSelected = ttsState.selectedNarrator.id == narrator.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectNarratorProfile(narrator) }
                                        .testTag("narrator_option_${narrator.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, Color(0xFFC89D54)) else null,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFC89D54).copy(alpha = 0.16f)
                                        else readerBgColor.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = narrator.nameAr,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = readerTextColor
                                            )
                                            Text(
                                                text = narrator.descriptionAr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = readerTextColor.copy(alpha = 0.75f)
                                            )
                                        }
                                        BadgePill(
                                            text = narrator.badgeAr,
                                            bg = Color(0xFFC89D54),
                                            contentColor = Color(0xFF1F1A17)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سرعة القارئ: ${String.format("%.2f", ttsState.customSpeedMultiplier)}x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = readerTextColor
                                )
                                FilterChip(
                                    selected = readTranslatedAudio,
                                    onClick = { readTranslatedAudio = !readTranslatedAudio },
                                    label = { Text("قراءة النص المترجم إن وجد") }
                                )
                            }

                            Slider(
                                value = ttsState.customSpeedMultiplier,
                                onValueChange = onChangeNarratorSpeed,
                                valueRange = 0.7f..1.75f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onToggleAudioNarration(0, readTranslatedAudio) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("toggle_tts_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (ttsState.isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (ttsState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (ttsState.isPlaying) "إيقاف القراءة الصوتية" else "تشغيل بصوت ${ttsState.selectedNarrator.nameAr}")
                                }
                            }
                        }
                    }
                }
            }

            // 5. Night Reading Mode & Typography Customization Panel
            item {
                AnimatedVisibility(visible = showAppearancePanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = readerCardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "نمط القراءة الليلية والورقية وحجم الخط",
                                style = MaterialTheme.typography.titleMedium,
                                color = readerTextColor
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReaderThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = readerSettings.themeMode == mode,
                                        onClick = { onChangeReaderTheme(mode) },
                                        label = { Text(mode.labelAr) },
                                        modifier = Modifier.testTag("reader_theme_${mode.name}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = readerTextColor)
                                    Text(
                                        text = "حجم الخط: ${readerSettings.fontSizeSp}sp",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = readerTextColor
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onAdjustFontSize(-2) }) {
                                        Text("تصغير A-")
                                    }
                                    OutlinedButton(onClick = { onAdjustFontSize(2) }) {
                                        Text("تكبير A+")
                                    }
                                    OutlinedButton(onClick = onToggleFontFamily) {
                                        Text(if (readerSettings.useClassicSerifFont) "خط أميري" else "خط كايرو")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Chapter Title & Paragraph-by-Paragraph Interactive Canvas
            if (selectedChapter != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedChapter.title,
                            fontFamily = activeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (readerSettings.fontSizeSp + 5).sp,
                            color = readerTextColor
                        )
                        if (readerSettings.translatedTitle.isNotBlank() &&
                            readerSettings.translationDisplayMode != TranslationDisplayMode.ORIGINAL_ONLY
                        ) {
                            Text(
                                text = readerSettings.translatedTitle,
                                fontFamily = activeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = (readerSettings.fontSizeSp + 2).sp,
                                color = Color(0xFFC89D54)
                            )
                        }
                        Text(
                            text = "انقر على أيقونة التعليق بجانب أي فقرة لإضافة أو قراءة التعليقات الجانبية، أو اضغط على رمز الصوت للاستماع من تلك الفقرة.",
                            style = MaterialTheme.typography.labelMedium,
                            color = readerTextColor.copy(alpha = 0.65f)
                        )
                    }
                }

                itemsIndexed(selectedChapter.paragraphs) { index, paragraphText ->
                    val paragraphCommentsCount = commentsForChapter.count { it.paragraphIndex == index }
                    val translatedText = readerSettings.translatedParagraphs[index]
                    val isSpokenNow = ttsState.isPlaying && ttsState.activeParagraphIndex == index
                    val isSavedReadingPosition = readingProgress != null &&
                        (readingProgress.lastReadChapterId == selectedChapter.id ||
                            readingProgress.lastReadChapterNumber == selectedChapter.chapterNumber) &&
                        readingProgress.lastReadParagraphIndex == index

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("paragraph_card_$index"),
                        shape = RoundedCornerShape(16.dp),
                        border = when {
                            isSpokenNow -> BorderStroke(2.dp, Color(0xFFC89D54))
                            isSavedReadingPosition -> BorderStroke(1.5.dp, Color(0xFFC89D54).copy(alpha = 0.8f))
                            else -> null
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSpokenNow) Color(0xFFC89D54).copy(alpha = 0.18f)
                            else readerCardColor.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Marginal Bar for Paragraph Index + Save Position + Audio Play + Instant Single Paragraph Translate + Side Comment Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = readerBgColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "فقرة #${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = readerTextColor.copy(alpha = 0.75f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (isSavedReadingPosition) {
                                        BadgePill(
                                            text = "موضع قراءتك المحفوظ",
                                            bg = Color(0xFFC89D54).copy(alpha = 0.25f),
                                            contentColor = readerTextColor
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Save reading position at this paragraph
                                    IconButton(
                                        onClick = { onSaveReadingPosition(index) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("save_position_button_$index")
                                    ) {
                                        Icon(
                                            imageVector = if (isSavedReadingPosition) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "حفظ موضع القراءة عند هذه الفقرة",
                                            tint = if (isSavedReadingPosition) Color(0xFFC89D54) else readerTextColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Read aloud from this paragraph
                                    IconButton(
                                        onClick = { onToggleAudioNarration(index, readTranslatedAudio) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpokenNow) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "قراءة الفقرة صوتياً",
                                            tint = if (isSpokenNow) Color(0xFFC89D54) else readerTextColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Translate this single paragraph
                                    IconButton(
                                        onClick = { onTranslateSingleParagraph(index) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("translate_paragraph_button_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "ترجمة فورية للفقرة",
                                            tint = if (translatedText != null) Color(0xFFC89D54) else readerTextColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Side Marginal Comment Button with Counter
                                    Surface(
                                        color = if (paragraphCommentsCount > 0)
                                            Color(0xFFC89D54).copy(alpha = 0.25f)
                                        else readerBgColor,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { activeCommentParagraphIndex = index }
                                            .testTag("paragraph_comment_button_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                                contentDescription = "التعليقات الجانبية للفقرة",
                                                tint = if (paragraphCommentsCount > 0) Color(0xFFC89D54) else readerTextColor.copy(alpha = 0.75f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "$paragraphCommentsCount تعليق جانبي",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = readerTextColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Original Paragraph Text
                            if (readerSettings.translationDisplayMode != TranslationDisplayMode.TRANSLATED_ONLY || translatedText == null) {
                                Text(
                                    text = paragraphText,
                                    fontFamily = activeFontFamily,
                                    fontSize = readerSettings.fontSizeSp.sp,
                                    lineHeight = (readerSettings.fontSizeSp * 1.65f).sp,
                                    color = readerTextColor,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.Content
                                    )
                                )
                            }

                            // Translated Paragraph Text (Instant AI Translation)
                            if (translatedText != null &&
                                readerSettings.translationDisplayMode != TranslationDisplayMode.ORIGINAL_ONLY
                            ) {
                                HorizontalDivider(color = readerTextColor.copy(alpha = 0.15f))
                                Surface(
                                    color = Color(0xFFC89D54).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "الترجمة الفورية (${readerSettings.targetTranslationLanguage}):",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFC89D54)
                                        )
                                        Text(
                                            text = translatedText,
                                            fontFamily = activeFontFamily,
                                            fontSize = readerSettings.fontSizeSp.sp,
                                            lineHeight = (readerSettings.fontSizeSp * 1.6f).sp,
                                            color = readerTextColor,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textDirection = TextDirection.Content
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Granular Multi-Criteria Rating Breakdown & Reader Reviews Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = readerCardColor)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "نظام التقييم الدقيق وآراء القراء",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = readerTextColor
                                )
                                Text(
                                    text = "مبني على ${novel.ratingCount} مراجعة تفصيلية من القراء",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = readerTextColor.copy(alpha = 0.7f)
                                )
                            }

                            Button(
                                onClick = { showReviewDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("أضف تقييمك")
                            }
                        }

                        // 4 Criteria Breakdown Bars
                        CriteriaScoreRow("الحبكة والسرد الدرامي", novel.plotRatingAvg, readerTextColor)
                        CriteriaScoreRow("الأسلوب الأدبي والبلاغة", novel.styleRatingAvg, readerTextColor)
                        CriteriaScoreRow("بناء الشخصيات والحوار", novel.charactersRatingAvg, readerTextColor)
                        CriteriaScoreRow("جودة اللغة والترجمة", novel.translationRatingAvg, readerTextColor)

                        if (reviewsForNovel.isNotEmpty()) {
                            HorizontalDivider(color = readerTextColor.copy(alpha = 0.15f))
                            Text(
                                text = "أحدث مراجعات القراء:",
                                style = MaterialTheme.typography.titleMedium,
                                color = readerTextColor
                            )
                            reviewsForNovel.take(5).forEach { review ->
                                Surface(
                                    color = readerBgColor.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = review.userName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = readerTextColor
                                            )
                                            Text(
                                                text = "${String.format("%.1f", review.overallRating)} ★",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color(0xFFC89D54)
                                            )
                                        }
                                        Text(
                                            text = "الحبكة ${review.plotRating}★ • الأسلوب ${review.styleRating}★ • الشخصيات ${review.charactersRating}★ • الترجمة ${review.translationRating}★",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = readerTextColor.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = review.reviewText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = readerTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Marginal Side Comments BottomSheet for a Specific Paragraph
    if (activeCommentParagraphIndex != null && selectedChapter != null) {
        val pIdx = activeCommentParagraphIndex!!
        val paragraphQuote = selectedChapter.paragraphs.getOrNull(pIdx).orEmpty()
        val matchingComments = commentsForChapter.filter { it.paragraphIndex == pIdx }
        var newCommentInput by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { activeCommentParagraphIndex = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "التعليقات الجانبية على الفقرة #${pIdx + 1}",
                    style = MaterialTheme.typography.titleLarge
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "«$paragraphQuote»",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (matchingComments.isEmpty()) {
                    Text(
                        text = "لا توجد تعليقات هامشية على هذه الفقرة بعد. كن أول من يشارك رأيه!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matchingComments, key = { it.id }) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = comment.userName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = comment.commentText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCommentInput,
                        onValueChange = { newCommentInput = it },
                        placeholder = { Text("اكتب تعليقاً جانبياً على هذه الفقرة...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("side_comment_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newCommentInput.isNotBlank()) {
                                onAddParagraphComment(pIdx, newCommentInput)
                                newCommentInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("submit_side_comment_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال التعليق الجانبي",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    // Multi-Criteria Rating Dialog
    if (showReviewDialog) {
        var plotStars by remember { mutableIntStateOf(5) }
        var styleStars by remember { mutableIntStateOf(5) }
        var characterStars by remember { mutableIntStateOf(5) }
        var translationStars by remember { mutableIntStateOf(5) }
        var reviewComment by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Text("تقييم دقيق لرواية «${novel.title}»")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StarCriteriaPicker("الحبكة والسرد:", plotStars) { plotStars = it }
                    StarCriteriaPicker("الأسلوب الأدبي:", styleStars) { styleStars = it }
                    StarCriteriaPicker("بناء الشخصيات:", characterStars) { characterStars = it }
                    StarCriteriaPicker("جودة اللغة والترجمة:", translationStars) { translationStars = it }

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("رأيك التفصيلي في الرواية") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_text_input"),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReview = reviewComment.ifBlank {
                            "رواية متميزة في الحبكة والأسلوب وتستحق القراءة."
                        }
                        onSubmitReview(plotStars, styleStars, characterStars, translationStars, finalReview)
                        showReviewDialog = false
                    },
                    modifier = Modifier.testTag("submit_review_confirm_button")
                ) {
                    Text("إرسال التقييم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun CriteriaScoreRow(
    label: String,
    score: Double,
    textColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor)
            Text(
                text = "${String.format("%.1f", score)} / 5.0",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFC89D54)
            )
        }
        LinearProgressIndicator(
            progress = { (score.toFloat() / 5f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFFC89D54),
            trackColor = textColor.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun StarCriteriaPicker(
    label: String,
    currentStars: Int,
    onSelectStars: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row {
            for (s in 1..5) {
                IconButton(
                    onClick = { onSelectStars(s) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "$s نجوم",
                        tint = if (s <= currentStars) Color(0xFFC89D54) else Color.Gray.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
