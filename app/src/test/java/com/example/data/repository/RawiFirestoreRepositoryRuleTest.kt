package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertFailsWith

class RawiFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createNovelAndChapter_authenticatedAuthor_succeedsAndEmitsUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = RawiFirestoreRepository(firestore, auth)

        val novelResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createNovel(
                authorName = "أليس المنصور",
                title = "أسرار مكتبة قرطبة",
                synopsis = "رواية تاريخية مشوقة",
                originalLanguage = "العربية",
                genre = "تاريخي",
                targetAudience = "عام",
                keywords = listOf("قرطبة", "تاريخ"),
                coverColorHex = "#8C2D19"
            )
        }
        assertThat(novelResult.isSuccess).isTrue()
        val novelId = novelResult.getOrThrow()
        assertThat(novelId).isNotEmpty()

        val chapterResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChapter(
                novelId = novelId,
                chapterNumber = 1,
                title = "الفصل الأول: المخطوطة المفقودة",
                paragraphs = listOf("في ليلة ماطرة من ليالي قرطبة القديمة، فتح الوراق باب الخزانة السرية."),
                language = "العربية"
            )
        }
        assertThat(chapterResult.isSuccess).isTrue()
        val chapterId = chapterResult.getOrThrow()

        val commentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addParagraphComment(
                novelId = novelId,
                chapterId = chapterId,
                paragraphIndex = 0,
                userName = "أليس",
                commentText = "بداية ساحرة للفصل!"
            )
        }
        assertThat(commentResult.isSuccess).isTrue()

        val emittedNovels = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicNovels().first { list -> list.any { it.id == novelId } }
        }
        assertThat(emittedNovels.map { it.id }).contains(novelId)
    }

    @Test
    fun followSubscription_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RawiFirestoreRepository(firestore, auth)
        val followId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertFollowSubscription(
                targetId = "novel_test_1",
                targetType = "NOVEL",
                targetName = "أسرار قرطبة",
                targetSubtitle = "أليس المنصور"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = RawiFirestoreRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFollowById(followId)
        }
        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull() as? FirebaseFirestoreException
        assertThat(exception?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun unauthenticatedUser_queryNovels_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val exception = assertFailsWith<FirebaseFirestoreException> {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("novels")
                    .whereEqualTo("visibility", "PUBLIC")
                    .get()
                    .await()
            }
        }
        assertThat(exception.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun authorProfileAndReadingProgress_saveAndObserve_succeedsWithIsolation() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RawiFirestoreRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveAuthorProfile(
                displayName = "د. طارق الأندلسي",
                bio = "روائي وباحث في التاريخ الأندلسي والمخطوطات الشرقية.",
                specialtyGenres = listOf("تاريخي", "غموض وتشويق"),
                websiteUrl = "https://rawi.app/tariq",
                twitterHandle = "@tariq_andalusi",
                instagramHandle = "@dr.tariq.books"
            )
        }
        assertThat(profileRes.isSuccess).isTrue()

        val progressRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveReadingProgress(
                novelId = "novel_test_1",
                novelTitle = "أسرار مكتبة قرطبة",
                genre = "تاريخي",
                lastReadChapterId = "ch_1",
                lastReadChapterNumber = 1,
                lastReadParagraphIndex = 2,
                completedChapterIds = listOf("ch_1"),
                totalChapters = 2
            )
        }
        assertThat(progressRes.isSuccess).isTrue()

        val emittedProgress = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeUserReadingProgress().first { it.isNotEmpty() }
        }
        assertThat(emittedProgress.first().novelTitle).isEqualTo("أسرار مكتبة قرطبة")
        assertThat(emittedProgress.first().progressFraction).isEqualTo(0.5)
    }

    @Test
    fun communityDiscussionGroupsAndPosts_createAnswerAndLike_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RawiFirestoreRepository(firestore, auth)

        val groupRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDiscussionGroup(
                novelId = "novel_test_1",
                novelTitle = "أسرار مكتبة قرطبة",
                authorName = "د. طارق الأندلسي",
                groupTitle = "حلقة نقاش أسرار قرطبة",
                description = "مجموعة لمناقشة أحداث الرواية مع المؤلف والقراء",
                genre = "تاريخي"
            )
        }
        assertThat(groupRes.isSuccess).isTrue()
        val groupId = groupRes.getOrThrow()

        val postRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCommunityPost(
                groupId = groupId,
                novelId = "novel_test_1",
                novelTitle = "أسرار مكتبة قرطبة",
                authorName = "قارئ شغوف",
                authorRole = "قارئ",
                postType = "QA_QUESTION",
                content = "هل هناك جزء ثانٍ من الرواية؟"
            )
        }
        assertThat(postRes.isSuccess).isTrue()
        val postId = postRes.getOrThrow()

        val answerRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.answerCommunityQuestion(
                postId = postId,
                answerText = "نعم، نعمل على الجزء الثاني حالياً!",
                answeredByName = "د. طارق الأندلسي"
            )
        }
        assertThat(answerRes.isSuccess).isTrue()

        val likeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.incrementCommunityPostLike(postId, 0)
        }
        assertThat(likeRes.isSuccess).isTrue()
    }

    private companion object {
        const val ALICE_EMAIL = "alice@rawi.test"
        const val BOB_EMAIL = "bob@rawi.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
