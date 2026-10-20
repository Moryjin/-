package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.OfflineChapterEntity
import com.example.data.model.FollowSubscription
import com.example.data.model.Novel
import com.example.ui.viewmodel.CustomAlertItem

@Composable
fun LibraryOfflineAlertsScreen(
    initialSubTab: Int = 0,
    userFollows: List<FollowSubscription>,
    allNovels: List<Novel>,
    offlineChapters: List<OfflineChapterEntity>,
    customAlerts: List<CustomAlertItem>,
    onOpenNovelById: (String) -> Unit,
    onOpenOfflineChapter: (OfflineChapterEntity) -> Unit,
    onDeleteOfflineChapter: (String) -> Unit,
    onUpdateShelfStatus: (FollowSubscription, String) -> Unit,
    onToggleNotification: (FollowSubscription) -> Unit,
    onMarkAlertSeen: (FollowSubscription, Int) -> Unit
) {
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var selectedShelfFilter by remember { mutableStateOf("الكل") }
    var notificationPermissionGranted by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("مكتبتي (${userFollows.size})") },
                icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_follows")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("بدون إنترنت (${offlineChapters.size})") },
                icon = { Icon(Icons.Default.CloudOff, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_offline")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("التنبيهات (${customAlerts.size})") },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_alerts")
            )
        }

        when (selectedTab) {
            // TAB 0: Organized Library & Reading Lists
            0 -> {
                val shelfFilters = listOf("الكل", "READING" to "قيد القراءة", "FAVORITE" to "المفضلة", "COMPLETED" to "المكتملة")
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "الكل" to "الكل",
                                "READING" to "قيد القراءة",
                                "FAVORITE" to "المفضلة",
                                "COMPLETED" to "المكتملة"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedShelfFilter == key,
                                    onClick = { selectedShelfFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    val filteredFollows = userFollows.filter {
                        selectedShelfFilter == "الكل" || it.readingStatus == selectedShelfFilter
                    }

                    if (filteredFollows.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "قائمة المتابعات فارغة في هذا التصنيف",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "أضف الروايات أو القراء المفضلين من شاشة الاكتشاف لتنظيم مكتبتك الشخصية.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredFollows, key = { it.id }) { follow ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = follow.targetName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${if (follow.targetType == "NOVEL") "رواية" else "قارئ/ناقد"} • ${follow.targetSubtitle}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (follow.targetType == "NOVEL") {
                                            Button(onClick = { onOpenNovelById(follow.targetId) }) {
                                                Text("قراءة")
                                            }
                                        }
                                    }

                                    if (follow.targetType == "NOVEL") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "READING" to "قيد القراءة",
                                                "FAVORITE" to "المفضلة",
                                                "COMPLETED" to "مكتملة"
                                            ).forEach { (statusKey, statusLabel) ->
                                                FilterChip(
                                                    selected = follow.readingStatus == statusKey,
                                                    onClick = { onUpdateShelfStatus(follow, statusKey) },
                                                    label = { Text(statusLabel) }
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

            // TAB 1: Offline Downloaded Chapters (Room SQLite Persistence)
            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جميع الفصول المحفوظة هنا مخزنة محلياً على جهازك مع ترجماتها الفورية لتتمكن من قراءتها في أي وقت بدون اتصال بالإنترنت.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (offlineChapters.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "لا توجد فصول محمّلة حالياً",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "اضغط على أيقونة التحميل السحابي داخل أي فصل لحفظه مع ترجمته للقراءة بدون إنترنت.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(offlineChapters, key = { it.chapterId }) { entity ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offline_chapter_card_${entity.chapterId}"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${entity.novelTitle} — ${entity.chapterTitle}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "تأليف: ${entity.authorName} • اللغة: ${entity.originalLanguage}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (entity.translatedLanguage != null) {
                                                Text(
                                                    text = "يتضمن ترجمة محفوظة إلى: ${entity.translatedLanguage}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDeleteOfflineChapter(entity.chapterId) },
                                            modifier = Modifier.testTag("delete_offline_${entity.chapterId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف الفصل المحمل",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onOpenOfflineChapter(entity) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح الفصل للقراءة بدون إنترنت")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: Custom Notifications & Follow Alerts Center
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "مركز التنبيهات المخصصة للمتابعات",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "تحكم في تنبيهات الفصول الجديدة لكل رواية أو مؤلف أو قارئ تتابعه.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    OutlinedButton(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    ) {
                                        Text("تفعيل إشعارات النظام")
                                    }
                                }
                            }
                        }
                    }

                    // Active Alert Notifications
                    items(customAlerts, key = { it.id }) { alert ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (alert.isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = alert.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (alert.novelId.isNotBlank()) {
                                    val follow = userFollows.find { it.targetId == alert.novelId }
                                    val novel = allNovels.find { it.id == alert.novelId }
                                    if (follow != null && novel != null && alert.isUnread) {
                                        IconButton(
                                            onClick = { onMarkAlertSeen(follow, novel.chapterCount) }
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "تمت القراءة")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Per-Subscription Notification Switches
                    item {
                        Text(
                            text = "إعدادات التنبيهات لكل متابعة:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(userFollows, key = { "pref_${it.id}" }) { follow ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (follow.notificationsEnabled)
                                            Icons.Default.NotificationsActive
                                        else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (follow.notificationsEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = follow.targetName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (follow.notificationsEnabled) "إشعارات الفصول الجديدة مفعّلة" else "التنبيهات مكتومة",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = follow.notificationsEnabled,
                                    onCheckedChange = { onToggleNotification(follow) },
                                    modifier = Modifier.testTag("alert_switch_${follow.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
