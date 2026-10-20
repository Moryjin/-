package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Novel(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val synopsis: String = "",
    val originalLanguage: String = "العربية",
    val genre: String = "فانتازيا",
    val targetAudience: String = "عام",
    val keywords: List<String> = emptyList(),
    val coverColorHex: String = "#8C2D19",
    val chapterCount: Int = 0,
    val ratingCount: Int = 0,
    val averageRating: Double = 0.0,
    val plotRatingAvg: Double = 0.0,
    val styleRatingAvg: Double = 0.0,
    val charactersRatingAvg: Double = 0.0,
    val translationRatingAvg: Double = 0.0,
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Chapter(
    @DocumentId val id: String = "",
    val novelId: String = "",
    val authorId: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val language: String = "العربية",
    val wordCount: Int = 0,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ParagraphComment(
    @DocumentId val id: String = "",
    val novelId: String = "",
    val chapterId: String = "",
    val paragraphIndex: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val commentText: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NovelReview(
    @DocumentId val id: String = "",
    val novelId: String = "",
    val userId: String = "",
    val userName: String = "",
    val plotRating: Int = 5,
    val styleRating: Int = 5,
    val charactersRating: Int = 5,
    val translationRating: Int = 5,
    val overallRating: Double = 5.0,
    val reviewText: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "NOVEL", // NOVEL, AUTHOR, READER
    val targetName: String = "",
    val targetSubtitle: String = "",
    val notificationsEnabled: Boolean = true,
    val lastKnownChapterCount: Int = 0,
    val readingStatus: String = "READING", // READING, COMPLETED, FAVORITE
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuthorProfile(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val displayName: String = "",
    val bio: String = "",
    val specialtyGenres: List<String> = emptyList(),
    val websiteUrl: String = "",
    val twitterHandle: String = "",
    val instagramHandle: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReadingProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val novelId: String = "",
    val novelTitle: String = "",
    val genre: String = "",
    val lastReadChapterId: String = "",
    val lastReadChapterNumber: Int = 1,
    val lastReadParagraphIndex: Int = 0,
    val completedChapterIds: List<String> = emptyList(),
    val totalChapters: Int = 1,
    val progressFraction: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NovelDiscussionGroup(
    @DocumentId val id: String = "",
    val novelId: String = "",
    val novelTitle: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val groupTitle: String = "",
    val description: String = "",
    val genre: String = "فانتازيا",
    val memberCount: Int = 1,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val groupId: String = "general",
    val novelId: String = "",
    val novelTitle: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "قارئ", // مؤلف or قارئ
    val postType: String = "GROUP_DISCUSSION", // AUTHOR_UPDATE, QA_QUESTION, GROUP_DISCUSSION
    val content: String = "",
    val authorAnswer: String = "",
    val answeredByName: String = "",
    val likesCount: Int = 0,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PersonalizedRecommendation(
    val novel: Novel,
    val matchScorePercent: Int,
    val reasonAr: String,
    val matchingFactors: List<String> = emptyList()
)

data class RecommendationLearningStats(
    val favoriteNovelsCount: Int = 0,
    val followedAuthorsCount: Int = 0,
    val preferredGenresCount: Int = 0,
    val completedNovelsCount: Int = 0,
    val userInteractionsCount: Int = 0
)

enum class ReaderThemeMode(val labelAr: String, val bgHex: Long, val textHex: Long, val cardHex: Long) {
    DAY_PAPER("نهاري عاجي", 0xFFFAF6F0, 0xFF1F1A17, 0xFFF2ECE1),
    WARM_SEPIA("ورق قديم دافئ", 0xFFF4ECD8, 0xFF3B2F2F, 0xFFE8DEC5),
    VELVET_NIGHT("ليلي مريح للعين", 0xFF0F141C, 0xFFEAE4DC, 0xFF18202C),
    AMOLED_DARK("ليلي حالك OLED", 0xFF07090D, 0xFFD8D2C9, 0xFF12161F)
}

enum class TranslationDisplayMode(val labelAr: String) {
    ORIGINAL_ONLY("النص الأصلي"),
    TRANSLATED_ONLY("النص المترجم فقط"),
    BILINGUAL_SIDE_BY_SIDE("مزدوج (فقرة بفقرة)")
}

data class NarratorProfile(
    val id: String,
    val nameAr: String,
    val descriptionAr: String,
    val pitch: Float,
    val speechRate: Float,
    val badgeAr: String
)

data class FeaturedCommunityReader(
    val id: String,
    val name: String,
    val roleTitle: String,
    val favoriteGenres: List<String>,
    val languages: List<String>,
    val bio: String,
    val reviewsCount: Int,
    val recommendedNovelTitle: String
)

object RawiCatalogConstants {
    val GENRES = listOf(
        "الكل",
        "فانتازيا",
        "خيال علمي",
        "تاريخي",
        "غموض وتشويق",
        "دراما اجتماعية",
        "أدب عالمي مترجم",
        "رومانسي"
    )

    val TARGET_AUDIENCES = listOf(
        "الكل",
        "عام",
        "يافعين (13+)",
        "شباب وبالغين (18+)",
        "عائلي"
    )

    val SUPPORTED_LANGUAGES = listOf(
        "العربية",
        "English",
        "Français",
        "Español",
        "Türkçe",
        "Deutsch",
        "日本語"
    )

    val NARRATOR_PROFILES = listOf(
        NarratorProfile(
            id = "classic_hakawati",
            nameAr = "الحكواتي الكلاسيكي",
            descriptionAr = "نبرة عميقة ومتأنية تناسب الروايات التاريخية والأدب الكلاسيكي",
            pitch = 0.90f,
            speechRate = 0.92f,
            badgeAr = "عميق ومتأنٍ"
        ),
        NarratorProfile(
            id = "dramatic_narrator",
            nameAr = "الراوي الدرامي المعبر",
            descriptionAr = "إلقاء حيوي متوازن يبرز الحوارات والمشاهد المشوقة",
            pitch = 1.05f,
            speechRate = 1.0f,
            badgeAr = "حيوي وتفاعلي"
        ),
        NarratorProfile(
            id = "calm_night_reader",
            nameAr = "قارئ السمر الليلي",
            descriptionAr = "إيقاع هادئ وناعم مثالي للقراءة الليلية قبل النوم",
            pitch = 0.95f,
            speechRate = 0.84f,
            badgeAr = "هادئ ليلي"
        ),
        NarratorProfile(
            id = "brisk_modern_reader",
            nameAr = "القارئ السريع المعاصر",
            descriptionAr = "سرعة إلقاء عالية لمحبي التصفح السريع والفصول الطويلة",
            pitch = 1.0f,
            speechRate = 1.25f,
            badgeAr = "سريع وواضح"
        )
    )

    val FEATURED_READERS = listOf(
        FeaturedCommunityReader(
            id = "reader_layla_critic",
            name = "ليلى المنصور",
            roleTitle = "ناقدة أدبية وقارئة نهمة",
            favoriteGenres = listOf("تاريخي", "فانتازيا"),
            languages = listOf("العربية", "Français"),
            bio = "أراجع الروايات التاريخية والمترجمة وأركز على عمق الحبكة وبناء الشخصيات.",
            reviewsCount = 148,
            recommendedNovelTitle = "أسرار مكتبة قرطبة"
        ),
        FeaturedCommunityReader(
            id = "reader_omar_scifi",
            name = "عمر الكيلاني",
            roleTitle = "مترجم وقارئ خيال علمي",
            favoriteGenres = listOf("خيال علمي", "غموض وتشويق"),
            languages = listOf("العربية", "English", "日本語"),
            bio = "مهتم بروايات المستقبل والذكاء الاصطناعي والترجمات الأدبية المقارنة.",
            reviewsCount = 94,
            recommendedNovelTitle = "Echoes of the Last Orbit — صدى المدار الأخير"
        ),
        FeaturedCommunityReader(
            id = "reader_sara_world",
            name = "سارة الأندلسي",
            roleTitle = "منسقة قوائم القراءة العالمية",
            favoriteGenres = listOf("أدب عالمي مترجم", "دراما اجتماعية"),
            languages = listOf("العربية", "Español", "English"),
            bio = "أشارك تعليقات هامشية تحليلية على الفصول وأرشح أفضل الروايات للشباب.",
            reviewsCount = 215,
            recommendedNovelTitle = "Les Jardins de Grenade — حدائق غرناطة المنسية"
        )
    )

    val CURATED_AUTHOR_BIOS = mapOf(
        "د. طارق الأندلسي" to AuthorProfile(
            id = "author_tariq",
            authorId = "author_tariq",
            displayName = "د. طارق الأندلسي",
            bio = "روائي وباحث في التاريخ الأندلسي والمخطوطات الشرقية. صدرت له عدة أعمال تجمع بين التحقيق التاريخي والتشويق الأدبي.",
            specialtyGenres = listOf("تاريخي", "غموض وتشويق"),
            websiteUrl = "https://rawi-authors.org/tariq-andalusi",
            twitterHandle = "@tariq_andalusi",
            instagramHandle = "@dr.tariq.books"
        ),
        "Elena Vance" to AuthorProfile(
            id = "author_elena",
            authorId = "author_elena",
            displayName = "Elena Vance",
            bio = "Astrophysicist and speculative fiction novelist exploring the intersection of ancient human languages and deep-space exploration.",
            specialtyGenres = listOf("خيال علمي", "غموض وتشويق"),
            websiteUrl = "https://elenavance-scifi.example.com",
            twitterHandle = "@elena_orbit",
            instagramHandle = "@elenavance.writes"
        ),
        "Claire Moreau" to AuthorProfile(
            id = "author_claire",
            authorId = "author_claire",
            displayName = "Claire Moreau",
            bio = "Romancière française passionnée par l'histoire méditerranéenne, la poésie andalouse et les récits initiatiques.",
            specialtyGenres = listOf("أدب عالمي مترجم", "تاريخي"),
            websiteUrl = "https://clairemoreau-livres.example.fr",
            twitterHandle = "@claire_moreau_lit",
            instagramHandle = "@claire.jardins"
        ),
        "مريم الشامي" to AuthorProfile(
            id = "author_mariam",
            authorId = "author_mariam",
            displayName = "مريم الشامي",
            bio = "كاتبة سورية تمزج بين الواقعية السحرية والذاكرة الدمشقية الدافئة في قصص موجهة للعائلة والشباب.",
            specialtyGenres = listOf("فانتازيا", "دراما اجتماعية"),
            websiteUrl = "https://mariamshami.example.com",
            twitterHandle = "@mariam_lanterns",
            instagramHandle = "@mariam.shami.stories"
        )
    )
}
