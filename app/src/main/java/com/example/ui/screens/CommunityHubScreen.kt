package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AuthorProfile
import com.example.data.model.CommunityPost
import com.example.data.model.FollowSubscription
import com.example.data.model.Novel
import com.example.data.model.NovelDiscussionGroup
import com.example.data.model.RawiCatalogConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityHubScreen(
    allNovels: List<Novel>,
    authorProfiles: List<AuthorProfile>,
    discussionGroups: List<NovelDiscussionGroup>,
    communityPosts: List<CommunityPost>,
    userFollows: List<FollowSubscription>,
    likedPostIds: Set<String>,
    currentUserId: String,
    currentUserName: String,
    onOpenAuthorProfile: (authorName: String, authorId: String) -> Unit,
    onToggleFollowAuthor: (authorName: String, authorId: String, subtitle: String) -> Unit,
    onSaveAuthorProfile: (
        displayName: String,
        bio: String,
        specialtyGenres: List<String>,
        websiteUrl: String,
        twitterHandle: String,
        instagramHandle: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onToggleJoinGroup: (NovelDiscussionGroup) -> Unit,
    onCreateDiscussionGroup: (
        novel: Novel?,
        groupTitle: String,
        description: String,
        genre: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onPublishCommunityPost: (
        groupId: String,
        novel: Novel?,
        authorRole: String,
        postType: String,
        content: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onAnswerQuestion: (
        post: CommunityPost,
        answerText: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onLikePost: (CommunityPost) -> Unit,
    onSelectNovel: (Novel) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedGroupFilterId by rememberSaveable { mutableStateOf("ALL") }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showEditAuthorProfileDialog by rememberSaveable { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Combine curated authors and cloud author profiles
    val combinedAuthors = remember(authorProfiles, allNovels) {
        val list = mutableListOf<AuthorProfile>()
        list.addAll(authorProfiles)
        for ((_, profile) in RawiCatalogConstants.CURATED_AUTHOR_BIOS) {
            if (list.none { it.displayName.equals(profile.displayName, ignoreCase = true) }) {
                list.add(profile)
            }
        }
        for (novel in allNovels) {
            if (list.none { it.displayName.equals(novel.authorName, ignoreCase = true) }) {
                list.add(
                    AuthorProfile(
                        id = novel.authorId.ifBlank { novel.authorName },
                        authorId = novel.authorId.ifBlank { novel.authorName },
                        displayName = novel.authorName,
                        bio = "مؤلف رواية «${novel.title}» في تصنيف ${novel.genre}.",
                        specialtyGenres = listOf(novel.genre)
                    )
                )
            }
        }
        list
    }

    if (showCreateGroupDialog) {
        CreateDiscussionGroupDialog(
            allNovels = allNovels,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { novel, title, desc, genre ->
                onCreateDiscussionGroup(novel, title, desc, genre) { ok, msg ->
                    statusBanner = msg
                    if (ok) showCreateGroupDialog = false
                }
            }
        )
    }

    if (showEditAuthorProfileDialog) {
        val myExisting = authorProfiles.find { it.authorId == currentUserId }
        QuickAuthorProfileModal(
            initialName = myExisting?.displayName ?: currentUserName,
            initialBio = myExisting?.bio ?: "",
            initialGenres = myExisting?.specialtyGenres ?: listOf("فانتازيا", "تاريخي"),
            initialWebsite = myExisting?.websiteUrl ?: "",
            initialTwitter = myExisting?.twitterHandle ?: "",
            initialInstagram = myExisting?.instagramHandle ?: "",
            onDismiss = { showEditAuthorProfileDialog = false },
            onSave = { name, bio, genres, web, tw, ig ->
                onSaveAuthorProfile(name, bio, genres, web, tw, ig) { ok, msg ->
                    statusBanner = msg
                    if (ok) showEditAuthorProfileDialog = false
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Community Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مجتمع روي للقرّاء والمؤلفين",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "مجموعات نقاش الروايات، تحديثات المؤلفين، الأسئلة والأجوبة، وتبادل الآراء",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showEditAuthorProfileDialog = true },
                        modifier = Modifier.testTag("open_author_profile_editor_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ملفي كمؤلف")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("مجموعات النقاش (${discussionGroups.size})") },
                        icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                        modifier = Modifier.testTag("community_tab_groups")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("حوارات القراء والمؤلفين") },
                        icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                        modifier = Modifier.testTag("community_tab_discussions")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("تحديثات المؤلفين") },
                        icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                        modifier = Modifier.testTag("community_tab_updates")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("أسئلة وأجوبة Q&A") },
                        icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null) },
                        modifier = Modifier.testTag("community_tab_qa")
                    )
                }
            }
        }

        AnimatedVisibility(visible = statusBanner != null) {
            statusBanner?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { statusBanner = null }) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Author Profiles Strip
            item {
                AuthorProfilesCarouselSection(
                    authors = combinedAuthors,
                    userFollows = userFollows,
                    onOpenAuthorProfile = onOpenAuthorProfile,
                    onToggleFollowAuthor = onToggleFollowAuthor,
                    onEditMyProfile = { showEditAuthorProfileDialog = true }
                )
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "حلقات ومجموعات النقاش الخاصة بالروايات",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "انضم إلى مجموعات الروايات لمناقشة الأحداث مع مؤلفيها والقراء، أو أنشئ مجموعة لروايتك.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showCreateGroupDialog = true },
                                    modifier = Modifier.testTag("create_discussion_group_btn")
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إنشاء مجموعة")
                                }
                            }
                        }
                    }

                    items(discussionGroups, key = { it.id }) { group ->
                        val isJoined = userFollows.any { it.targetType == "GROUP" && it.targetId == group.id }
                        val groupPostsCount = communityPosts.count { it.groupId == group.id }
                        DiscussionGroupCard(
                            group = group,
                            isJoined = isJoined,
                            postsCount = groupPostsCount,
                            onToggleJoin = { onToggleJoinGroup(group) },
                            onOpenDiscussionRoom = {
                                selectedGroupFilterId = group.id
                                selectedTab = 1
                            },
                            onOpenAuthorProfile = {
                                onOpenAuthorProfile(group.authorName, group.authorId)
                            },
                            onOpenNovel = {
                                val novel = allNovels.find { it.id == group.novelId || it.title == group.novelTitle }
                                if (novel != null) onSelectNovel(novel)
                            }
                        )
                    }
                }

                1 -> {
                    item {
                        DiscussionComposerCard(
                            title = "شارك رأيك أو افتح نقاشاً حول رواية",
                            subtitle = "تبادل الآراء والتحليلات مع المؤلفين والقراء في مجموعات النقاش (يساهم في تطوير توصياتك الذكية)",
                            postType = "GROUP_DISCUSSION",
                            allNovels = allNovels,
                            discussionGroups = discussionGroups,
                            selectedGroupFilterId = selectedGroupFilterId,
                            onSelectGroupFilter = { selectedGroupFilterId = it },
                            onPublish = { groupId, novel, role, content ->
                                onPublishCommunityPost(groupId, novel, role, "GROUP_DISCUSSION", content) { _, msg ->
                                    statusBanner = msg
                                }
                            }
                        )
                    }

                    val filteredPosts = communityPosts.filter { post ->
                        (selectedGroupFilterId == "ALL" || post.groupId == selectedGroupFilterId) &&
                            (post.postType == "GROUP_DISCUSSION" || selectedGroupFilterId != "ALL")
                    }

                    items(filteredPosts, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            groupName = discussionGroups.find { it.id == post.groupId }?.groupTitle ?: "ساحة الحوار العامة",
                            onLike = { onLikePost(post) },
                            onOpenAuthorProfile = { onOpenAuthorProfile(post.authorName, post.authorId) },
                            onOpenNovel = {
                                val novel = allNovels.find { it.id == post.novelId || it.title == post.novelTitle }
                                if (novel != null) onSelectNovel(novel)
                            },
                            onSubmitAnswer = { answer ->
                                onAnswerQuestion(post, answer) { _, msg -> statusBanner = msg }
                            }
                        )
                    }
                }

                2 -> {
                    item {
                        DiscussionComposerCard(
                            title = "نشر تحديث رسمي من المؤلف للقراء",
                            subtitle = "أعلن عن الفصول القادمة، كواليس الكتابة، أو أخبار ترجمة روايتك لمتابعيك",
                            postType = "AUTHOR_UPDATE",
                            allNovels = allNovels,
                            discussionGroups = discussionGroups,
                            selectedGroupFilterId = "ALL",
                            onSelectGroupFilter = {},
                            defaultRole = "مؤلف",
                            onPublish = { groupId, novel, _, content ->
                                onPublishCommunityPost(groupId, novel, "مؤلف", "AUTHOR_UPDATE", content) { _, msg ->
                                    statusBanner = msg
                                }
                            }
                        )
                    }

                    val authorUpdates = communityPosts.filter { it.postType == "AUTHOR_UPDATE" }
                    items(authorUpdates, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            groupName = discussionGroups.find { it.id == post.groupId }?.groupTitle ?: "إعلانات المؤلفين",
                            onLike = { onLikePost(post) },
                            onOpenAuthorProfile = { onOpenAuthorProfile(post.authorName, post.authorId) },
                            onOpenNovel = {
                                val novel = allNovels.find { it.id == post.novelId || it.title == post.novelTitle }
                                if (novel != null) onSelectNovel(novel)
                            },
                            onSubmitAnswer = {}
                        )
                    }
                }

                3 -> {
                    item {
                        DiscussionComposerCard(
                            title = "اسأل مؤلفك المفضل (سؤال وجواب Q&A)",
                            subtitle = "اطرح سؤالك حول الحبكة أو الشخصيات ليجيب عليه المؤلف مباشرة في المجتمع",
                            postType = "QA_QUESTION",
                            allNovels = allNovels,
                            discussionGroups = discussionGroups,
                            selectedGroupFilterId = "ALL",
                            onSelectGroupFilter = {},
                            defaultRole = "قارئ",
                            onPublish = { groupId, novel, role, content ->
                                onPublishCommunityPost(groupId, novel, role, "QA_QUESTION", content) { _, msg ->
                                    statusBanner = msg
                                }
                            }
                        )
                    }

                    val qaPosts = communityPosts.filter { it.postType == "QA_QUESTION" }
                    items(qaPosts, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            groupName = discussionGroups.find { it.id == post.groupId }?.groupTitle ?: "لقاء المؤلفين Q&A",
                            onLike = { onLikePost(post) },
                            onOpenAuthorProfile = { onOpenAuthorProfile(post.authorName, post.authorId) },
                            onOpenNovel = {
                                val novel = allNovels.find { it.id == post.novelId || it.title == post.novelTitle }
                                if (novel != null) onSelectNovel(novel)
                            },
                            onSubmitAnswer = { answer ->
                                onAnswerQuestion(post, answer) { _, msg -> statusBanner = msg }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorProfilesCarouselSection(
    authors: List<AuthorProfile>,
    userFollows: List<FollowSubscription>,
    onOpenAuthorProfile: (authorName: String, authorId: String) -> Unit,
    onToggleFollowAuthor: (authorName: String, authorId: String, subtitle: String) -> Unit,
    onEditMyProfile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ملفات المؤلفين المتفاعلين (تابعهم لتحسين توصياتك)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onEditMyProfile) {
                Text("إنشاء ملف مؤلف")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(authors, key = { it.displayName }) { author ->
                val isFollowed = userFollows.any {
                    it.targetType == "AUTHOR" &&
                        (it.targetName.equals(author.displayName, ignoreCase = true) ||
                            (author.authorId.isNotBlank() && it.targetId == author.authorId))
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .width(250.dp)
                        .clickable { onOpenAuthorProfile(author.displayName, author.authorId) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = author.displayName.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = author.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = author.specialtyGenres.joinToString(" • ").ifBlank { "روائي" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = author.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onOpenAuthorProfile(author.displayName, author.authorId) }
                            ) {
                                Text("السيرة والأعمال")
                            }
                            FilterChip(
                                selected = isFollowed,
                                onClick = {
                                    onToggleFollowAuthor(
                                        author.displayName,
                                        author.authorId,
                                        author.specialtyGenres.joinToString(" • ").ifBlank { "مؤلف في منصة روي" }
                                    )
                                },
                                label = { Text(if (isFollowed) "متابَع" else "متابعة") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFollowed) Icons.Default.HowToReg else Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("follow_author_chip_${author.displayName}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionGroupCard(
    group: NovelDiscussionGroup,
    isJoined: Boolean,
    postsCount: Int,
    onToggleJoin: () -> Unit,
    onOpenDiscussionRoom: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    onOpenNovel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discussion_group_card_${group.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = group.genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${group.memberCount} عضو • $postsCount نقاش",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = group.groupTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilterChip(
                    selected = isJoined,
                    onClick = onToggleJoin,
                    label = { Text(if (isJoined) "عضو منضم" else "انضمام للمجموعة") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isJoined) Icons.Default.CheckCircle else Icons.Default.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("join_group_btn_${group.id}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onOpenNovel,
                        label = {
                            Text(
                                text = group.novelTitle.ifBlank { "رواية مرتبطة" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                    AssistChip(
                        onClick = onOpenAuthorProfile,
                        label = { Text(group.authorName) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                }

                Button(
                    onClick = onOpenDiscussionRoom,
                    modifier = Modifier.testTag("open_group_discussions_btn_${group.id}")
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حلقات النقاش")
                }
            }
        }
    }
}

@Composable
private fun DiscussionComposerCard(
    title: String,
    subtitle: String,
    postType: String,
    allNovels: List<Novel>,
    discussionGroups: List<NovelDiscussionGroup>,
    selectedGroupFilterId: String,
    onSelectGroupFilter: (String) -> Unit,
    defaultRole: String = "قارئ",
    onPublish: (groupId: String, novel: Novel?, role: String, content: String) -> Unit
) {
    var contentText by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(defaultRole) }
    var selectedNovelIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeNovel = allNovels.getOrNull(selectedNovelIndex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (postType == "GROUP_DISCUSSION" && discussionGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "تصفية حسب مجموعة النقاش:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupFilterId == "ALL",
                        onClick = { onSelectGroupFilter("ALL") },
                        label = { Text("كل المجموعات") }
                    )
                    discussionGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroupFilterId == grp.id,
                            onClick = { onSelectGroupFilter(grp.id) },
                            label = { Text(grp.groupTitle, maxLines = 1) }
                        )
                    }
                }
            }

            if (allNovels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الرواية محور النقاش:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allNovels.forEachIndexed { idx, novel ->
                        FilterChip(
                            selected = selectedNovelIndex == idx,
                            onClick = { selectedNovelIndex = idx },
                            label = { Text(novel.title, maxLines = 1) }
                        )
                    }
                }
            }

            if (postType != "AUTHOR_UPDATE") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("المشاركة بصفتك:", style = MaterialTheme.typography.labelMedium)
                    FilterChip(
                        selected = selectedRole == "قارئ",
                        onClick = { selectedRole = "قارئ" },
                        label = { Text("قارئ") }
                    )
                    FilterChip(
                        selected = selectedRole == "مؤلف",
                        onClick = { selectedRole = "مؤلف" },
                        label = { Text("مؤلف الرواية") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = {
                    Text(
                        when (postType) {
                            "AUTHOR_UPDATE" -> "اكتب تحديثاً أو إعلاناً لقرائك..."
                            "QA_QUESTION" -> "اكتب سؤالك للمؤلف حول الرواية..."
                            else -> "شارك رأيك أو تحليلك لأحداث الرواية..."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("community_post_input_$postType"),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val targetGroupId = if (selectedGroupFilterId != "ALL") {
                            selectedGroupFilterId
                        } else {
                            discussionGroups.find { it.novelId == activeNovel?.id }?.id
                                ?: discussionGroups.firstOrNull()?.id
                                ?: "general"
                        }
                        onPublish(targetGroupId, activeNovel, selectedRole, contentText)
                        contentText = ""
                    },
                    modifier = Modifier.testTag("submit_community_post_btn_$postType")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (postType) {
                            "AUTHOR_UPDATE" -> "نشر تحديث المؤلف"
                            "QA_QUESTION" -> "إرسال السؤال للمؤلف"
                            else -> "نشر في حلقة النقاش"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    isLiked: Boolean,
    groupName: String,
    onLike: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    onOpenNovel: () -> Unit,
    onSubmitAnswer: (String) -> Unit
) {
    var showAnswerBox by rememberSaveable { mutableStateOf(false) }
    var answerDraft by rememberSaveable { mutableStateOf(post.authorAnswer) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAuthorProfile() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (post.authorRole == "مؤلف")
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = if (post.authorRole == "مؤلف")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (post.authorRole == "مؤلف")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = post.authorRole,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (post.authorRole == "مؤلف")
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (post.postType) {
                            "AUTHOR_UPDATE" -> "تحديث مؤلف"
                            "QA_QUESTION" -> "سؤال وجواب Q&A"
                            else -> "نقاش القراء"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge
            )

            if (post.authorAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إجابة المؤلف (${post.answeredByName.ifBlank { "المؤلف" }}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = post.authorAnswer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.novelTitle.isNotBlank()) {
                    AssistChip(
                        onClick = onOpenNovel,
                        label = {
                            Text(
                                text = post.novelTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (post.postType == "QA_QUESTION") {
                        TextButton(
                            onClick = { showAnswerBox = !showAnswerBox },
                            modifier = Modifier.testTag("toggle_answer_qa_btn_${post.id}")
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (post.authorAnswer.isBlank()) "الرد كمؤلف" else "تحديث إجابة المؤلف")
                        }
                    }

                    FilterChip(
                        selected = isLiked,
                        onClick = onLike,
                        label = { Text("إعجاب (${post.likesCount})") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "إعجاب بالمشاركة",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("like_post_btn_${post.id}")
                    )
                }
            }

            AnimatedVisibility(visible = showAnswerBox) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    OutlinedTextField(
                        value = answerDraft,
                        onValueChange = { answerDraft = it },
                        label = { Text("اكتب إجابتك الرسمية كمؤلف على سؤال القارئ...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAnswerBox = false }) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                onSubmitAnswer(answerDraft)
                                showAnswerBox = false
                            }
                        ) {
                            Text("نشر الإجابة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDiscussionGroupDialog(
    allNovels: List<Novel>,
    onDismiss: () -> Unit,
    onCreate: (novel: Novel?, groupTitle: String, description: String, genre: String) -> Unit
) {
    var selectedNovelIndex by rememberSaveable { mutableIntStateOf(0) }
    var groupTitle by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val selectedNovel = allNovels.getOrNull(selectedNovelIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء مجموعة نقاش خاصة برواية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اختر الرواية المرتبطة بالمجموعة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allNovels.forEachIndexed { index, novel ->
                        FilterChip(
                            selected = selectedNovelIndex == index,
                            onClick = { selectedNovelIndex = index },
                            label = { Text(novel.title, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("اسم مجموعة النقاش") },
                    placeholder = { Text("مثال: نادي قراء ${selectedNovel?.title ?: "الرواية"}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المجموعة وموضوعات النقاش") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        selectedNovel,
                        groupTitle,
                        description,
                        selectedNovel?.genre ?: "فانتازيا"
                    )
                }
            ) {
                Text("إنشاء المجموعة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAuthorProfileModal(
    initialName: String,
    initialBio: String,
    initialGenres: List<String>,
    initialWebsite: String,
    initialTwitter: String,
    initialInstagram: String,
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        bio: String,
        specialtyGenres: List<String>,
        websiteUrl: String,
        twitterHandle: String,
        instagramHandle: String
    ) -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf(initialName) }
    var bio by rememberSaveable { mutableStateOf(initialBio) }
    var selectedGenres by remember { mutableStateOf(initialGenres.toSet()) }
    var websiteUrl by rememberSaveable { mutableStateOf(initialWebsite) }
    var twitterHandle by rememberSaveable { mutableStateOf(initialTwitter) }
    var instagramHandle by rememberSaveable { mutableStateOf(initialInstagram) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ملفي الشخصي كمؤلف في مجتمع روي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم الأدبي للمؤلف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("السيرة الذاتية الأدبية ورسالتك للقراء") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "التصنيفات الأدبية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RawiCatalogConstants.GENRES.filter { it != "الكل" }.forEach { genre ->
                        val selected = genre in selectedGenres
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedGenres = if (selected) selectedGenres - genre else selectedGenres + genre
                            },
                            label = { Text(genre) }
                        )
                    }
                }
                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("الموقع الشخصي (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = twitterHandle,
                        onValueChange = { twitterHandle = it },
                        label = { Text("حساب X / Twitter") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = instagramHandle,
                        onValueChange = { instagramHandle = it },
                        label = { Text("Instagram") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayName.ifBlank { initialName },
                        bio.ifBlank { "مؤلف وروائي يتفاعل مع القراء في مجتمع روي." },
                        selectedGenres.toList(),
                        websiteUrl,
                        twitterHandle,
                        instagramHandle
                    )
                }
            ) {
                Text("حفظ ملف المؤلف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
