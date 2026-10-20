package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AuthorProfile
import com.example.data.model.Novel
import com.example.data.model.RawiCatalogConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthorStudioScreen(
    allNovels: List<Novel>,
    authorProfiles: List<AuthorProfile> = emptyList(),
    currentUserId: String,
    currentUserName: String,
    onPublishNewNovel: (
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
    ) -> Unit,
    onPublishAdditionalChapter: (
        novel: Novel,
        chapterTitle: String,
        chapterContent: String,
        markNovelCompleted: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onSaveAuthorProfile: (
        displayName: String,
        bio: String,
        specialtyGenres: List<String>,
        websiteUrl: String,
        twitterHandle: String,
        instagramHandle: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var modeTab by remember { mutableIntStateOf(0) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    // New Novel Form State
    var novelTitle by remember { mutableStateOf("") }
    var novelSynopsis by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("العربية") }
    var selectedGenre by remember { mutableStateOf("فانتازيا") }
    var selectedAudience by remember { mutableStateOf("عام") }
    var keywordsInput by remember { mutableStateOf("تشويق، مغامرة، خيال") }
    var selectedCoverHex by remember { mutableStateOf("#8C2D19") }
    var firstChapterTitle by remember { mutableStateOf("الفصل الأول: البداية") }
    var firstChapterBody by remember { mutableStateOf("") }

    // Add Chapter to Existing Novel State
    val authorNovels = allNovels.filter { it.authorId == currentUserId || it.authorName == currentUserName }
    var selectedExistingNovel by remember(authorNovels) { mutableStateOf(authorNovels.firstOrNull()) }
    var newChapterTitle by remember { mutableStateOf("") }
    var newChapterBody by remember { mutableStateOf("") }
    var markCompleted by remember { mutableStateOf(false) }

    // Author Profile & Social Media Links State
    val existingMyProfile = authorProfiles.find { it.authorId == currentUserId }
    var profileDisplayName by remember(existingMyProfile, currentUserName) {
        mutableStateOf(existingMyProfile?.displayName ?: currentUserName)
    }
    var profileBio by remember(existingMyProfile) {
        mutableStateOf(
            existingMyProfile?.bio
                ?: "روائي ومؤلف ينشر أعماله الأدبية عبر منصة روي للروايات العربية والعالمية."
        )
    }
    var profileSelectedGenres by remember(existingMyProfile) {
        mutableStateOf(existingMyProfile?.specialtyGenres?.toSet() ?: setOf("فانتازيا", "تاريخي"))
    }
    var profileWebsite by remember(existingMyProfile) {
        mutableStateOf(existingMyProfile?.websiteUrl ?: "https://rawi.app/authors")
    }
    var profileTwitter by remember(existingMyProfile) {
        mutableStateOf(existingMyProfile?.twitterHandle ?: "@rawi_author")
    }
    var profileInstagram by remember(existingMyProfile) {
        mutableStateOf(existingMyProfile?.instagramHandle ?: "@rawi.novels")
    }

    val coverPalette = listOf("#8C2D19", "#1B4965", "#2D6A4F", "#6D2E46", "#5F4B8B", "#7F4F24")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = modeTab) {
            Tab(
                selected = modeTab == 0,
                onClick = { modeTab = 0 },
                text = { Text("نشر رواية جديدة") },
                icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                modifier = Modifier.testTag("studio_tab_new_novel")
            )
            Tab(
                selected = modeTab == 1,
                onClick = { modeTab = 1 },
                text = { Text("إضافة فصل (${authorNovels.size})") },
                icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                modifier = Modifier.testTag("studio_tab_add_chapter")
            )
            Tab(
                selected = modeTab == 2,
                onClick = { modeTab = 2 },
                text = { Text("ملفي كمؤلف") },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.testTag("studio_tab_author_profile")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (feedbackBanner != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feedbackBanner!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            when (modeTab) {
                0 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "استوديو المؤلف — نشر رواية متعددة اللغات",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "انشر روايتك بالعربية أو بأي لغة عالمية، وسيتمكن القراء من ترجمتها فورياً والتعليق على كل فقرة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = novelTitle,
                                    onValueChange = { novelTitle = it },
                                    label = { Text("عنوان الرواية") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_novel_title_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = novelSynopsis,
                                    onValueChange = { novelSynopsis = it },
                                    label = { Text("نبذة الرواية (الملخص التشويقي)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_novel_synopsis_input"),
                                    minLines = 3
                                )

                                Text("لغة الكتابة الأصلية:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RawiCatalogConstants.SUPPORTED_LANGUAGES.forEach { lang ->
                                        FilterChip(
                                            selected = selectedLanguage == lang,
                                            onClick = { selectedLanguage = lang },
                                            label = { Text(lang) }
                                        )
                                    }
                                }

                                Text("النوع الأدبي:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RawiCatalogConstants.GENRES.filter { it != "الكل" }.forEach { genre ->
                                        FilterChip(
                                            selected = selectedGenre == genre,
                                            onClick = { selectedGenre = genre },
                                            label = { Text(genre) }
                                        )
                                    }
                                }

                                Text("الجمهور المستهدف:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RawiCatalogConstants.TARGET_AUDIENCES.filter { it != "الكل" }.forEach { aud ->
                                        FilterChip(
                                            selected = selectedAudience == aud,
                                            onClick = { selectedAudience = aud },
                                            label = { Text(aud) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = keywordsInput,
                                    onValueChange = { keywordsInput = it },
                                    label = { Text("الكلمات المفتاحية للبحث المتقدم (افصل بينها بفاصلة)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_keywords_input"),
                                    singleLine = true
                                )

                                Text("لون غلاف الرواية:", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    coverPalette.forEach { hex ->
                                        val c = Color(android.graphics.Color.parseColor(hex))
                                        val isSelected = selectedCoverHex == hex
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) Color(0xFFC89D54) else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedCoverHex = hex }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "محتوى الفصل الأول (افصل بين الفقرات بسطر جديد لتفعيل التعليقات الجانبية لكل فقرة):",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                OutlinedTextField(
                                    value = firstChapterTitle,
                                    onValueChange = { firstChapterTitle = it },
                                    label = { Text("عنوان الفصل الأول") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = firstChapterBody,
                                    onValueChange = { firstChapterBody = it },
                                    label = { Text("نص الفصل مقسماً إلى فقرات...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_first_chapter_body_input"),
                                    minLines = 6
                                )

                                Button(
                                    onClick = {
                                        if (novelTitle.isNotBlank() && novelSynopsis.isNotBlank() && firstChapterBody.isNotBlank()) {
                                            onPublishNewNovel(
                                                novelTitle,
                                                novelSynopsis,
                                                selectedLanguage,
                                                selectedGenre,
                                                selectedAudience,
                                                keywordsInput,
                                                selectedCoverHex,
                                                firstChapterTitle,
                                                firstChapterBody
                                            ) { ok, msg ->
                                                feedbackBanner = msg
                                                if (ok) {
                                                    novelTitle = ""
                                                    novelSynopsis = ""
                                                    firstChapterBody = ""
                                                }
                                            }
                                        } else {
                                            feedbackBanner = "يرجى إدخال عنوان الرواية والنبذة ونص الفصل الأول."
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("studio_publish_novel_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Publish, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("نشر الرواية والفصل الأول في المكتبة")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "إضافة فصل جديد وإشعار المتابعين",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (authorNovels.isEmpty()) {
                                    Text(
                                        text = "لم تقم بنشر رواية خاصة بك بعد. انتقل إلى تبويب «نشر رواية جديدة» لإطلاق روايتك الأولى.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text("اختر الرواية:", style = MaterialTheme.typography.labelLarge)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        authorNovels.forEach { novel ->
                                            FilterChip(
                                                selected = selectedExistingNovel?.id == novel.id,
                                                onClick = { selectedExistingNovel = novel },
                                                label = { Text("${novel.title} (${novel.chapterCount} فصول)") }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newChapterTitle,
                                        onValueChange = { newChapterTitle = it },
                                        label = { Text("عنوان الفصل الجديد") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = newChapterBody,
                                        onValueChange = { newChapterBody = it },
                                        label = { Text("فقرات الفصل الجديد (كل فقرة في سطر مستقل)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 6
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("تحديد الرواية كمكتملة بعد هذا الفصل", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = markCompleted, onCheckedChange = { markCompleted = it })
                                    }

                                    Button(
                                        onClick = {
                                            val target = selectedExistingNovel
                                            if (target != null && newChapterBody.isNotBlank()) {
                                                onPublishAdditionalChapter(
                                                    target,
                                                    newChapterTitle,
                                                    newChapterBody,
                                                    markCompleted
                                                ) { ok, msg ->
                                                    feedbackBanner = msg
                                                    if (ok) {
                                                        newChapterTitle = ""
                                                        newChapterBody = ""
                                                    }
                                                }
                                            } else {
                                                feedbackBanner = "يرجى اختيار الرواية وكتابة نص الفصل."
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Publish, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("نشر الفصل وإرسال تنبيه للمتابعين")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "ملف المؤلف الشخصي وروابط التواصل الاجتماعي",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "أضف سيرتك الذاتية وروابط صفحاتك الاجتماعية ليتمكن القراء من التواصل معك والاطلاع على قائمة أعمالك المنشورة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = profileDisplayName,
                                    onValueChange = { profileDisplayName = it },
                                    label = { Text("الاسم الأدبي للمؤلف") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("author_profile_name_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = profileBio,
                                    onValueChange = { profileBio = it },
                                    label = { Text("السيرة الذاتية للمؤلف (نبذة عن أسلوبك ومسيرتك)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("author_profile_bio_input"),
                                    minLines = 3
                                )

                                Text("الأنواع الأدبية التي تكتب فيها:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RawiCatalogConstants.GENRES.filter { it != "الكل" }.forEach { genre ->
                                        val isSelected = genre in profileSelectedGenres
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                profileSelectedGenres = if (isSelected) {
                                                    profileSelectedGenres - genre
                                                } else {
                                                    profileSelectedGenres + genre
                                                }
                                            },
                                            label = { Text(genre) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = profileWebsite,
                                    onValueChange = { profileWebsite = it },
                                    label = { Text("رابط الموقع الرسمي أو المدونة (اختياري)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("author_profile_website_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = profileTwitter,
                                    onValueChange = { profileTwitter = it },
                                    label = { Text("حساب X / تويتر (اختياري)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("author_profile_twitter_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = profileInstagram,
                                    onValueChange = { profileInstagram = it },
                                    label = { Text("حساب إنستغرام (اختياري)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("author_profile_instagram_input"),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (profileDisplayName.isNotBlank() && profileBio.isNotBlank()) {
                                            onSaveAuthorProfile(
                                                profileDisplayName,
                                                profileBio,
                                                profileSelectedGenres.toList(),
                                                profileWebsite,
                                                profileTwitter,
                                                profileInstagram
                                            ) { _, msg ->
                                                feedbackBanner = msg
                                            }
                                        } else {
                                            feedbackBanner = "يرجى إدخال الاسم الأدبي والسيرة الذاتية للمؤلف."
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("save_author_profile_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("حفظ ملف المؤلف وروابط التواصل")
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Text(
                                    text = "أعمالي المنشورة في التطبيق (${authorNovels.size}):",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (authorNovels.isEmpty()) {
                                    Text(
                                        text = "لم تقم بنشر أعمال بعد. ستظهر جميع رواياتك المنشورة هنا وفي ملفك العام للقراء.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    authorNovels.forEach { work ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = work.title,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "${work.genre} • ${work.originalLanguage} • ${work.chapterCount} فصول • التقييم ${String.format("%.1f", work.averageRating)}★",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}
