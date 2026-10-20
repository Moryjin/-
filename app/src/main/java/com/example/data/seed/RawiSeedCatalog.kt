package com.example.data.seed

import com.example.data.repository.RawiFirestoreRepository

data class SeedNovelTemplate(
    val authorName: String,
    val title: String,
    val synopsis: String,
    val originalLanguage: String,
    val genre: String,
    val targetAudience: String,
    val keywords: List<String>,
    val coverColorHex: String,
    val isCompleted: Boolean,
    val chapters: List<Pair<String, List<String>>>,
    val sampleComments: List<Pair<Int, String>>
)

object RawiSeedCatalog {
    val INITIAL_NOVELS = listOf(
        SeedNovelTemplate(
            authorName = "د. طارق الأندلسي",
            title = "أسرار مكتبة قرطبة",
            synopsis = "رواية تاريخية مشوقة تأخذك إلى أروقة قرطبة في القرن العاشر، حيث يعثر الوراق حسن بن يحيى على مخطوطة نادرة مكتوبة بأربع لغات تكشف سر خزانة الحكمة المفقودة.",
            originalLanguage = "العربية",
            genre = "تاريخي",
            targetAudience = "عام",
            keywords = listOf("قرطبة", "مخطوطات", "الأندلس", "غموض", "تاريخ"),
            coverColorHex = "#8C2D19",
            isCompleted = false,
            chapters = listOf(
                "الفصل الأول: المخطوطة ذات الألسنة الأربعة" to listOf(
                    "في ليلةٍ مقمرةٍ من ليالي قرطبة القديمة، وقف الوراق «حسن بن يحيى» أمام الخزانة الخشبية المزخرفة بماء الذهب، يتأمل المخطوطة التي وصلت لتوها من سمرقند. لم تكن مجرد أوراقٍ من الكتان، بل كانت تضم أربعة ألسنةٍ تتجاور على الصفحة الواحدة.",
                    "حين مرّر أنامله فوق الحبر القرمزي، شعر بدفءٍ غريب يسري في أطرافه، وكأن الكلمات المكتوبة منذ قرون لم تجفّ بعد، بل تنتظر قارئاً ينطق بها لتستعيد حياتها.",
                    "قال لتلميذه الصغير وهو يرفع السراج النحاسي: «يا بني، إن الروايات لا تموت حين تُطوى الصفحات، بل حين ينقطع الجسر بين لغة الكاتب وقلب القارئ».",
                    "وعلى شرفة القصر المطلّ على الوادي الكبير، كانت نسائم الفجر تحمل عطر النارنج، معلنةً بداية رحلةٍ ستغير مصير الخزانة إلى الأبد."
                ),
                "الفصل الثاني: شفرة الإسطرلاب النحاسي" to listOf(
                    "مع بزوغ شمس الصباح في سوق الوراقين، اجتمع العلماء والمترجمون حول الطاولة الرخامية لفك رموز الهامش الذهبي المرسوم في الصفحة السابعة.",
                    "كانت دوائر الإسطرلاب تشير إلى ثلاث مدنٍ تتقاطع فيها خطوط الطول مع أبيات شعرية قديمة، لا تكتمل معانيها إلا بجمع الترجمات الأربع معاً.",
                    "ابتسم حسن وقال: «كل قارئ يضيف في الهامش بصمته، ومن مجموع هذه الهوامش يولد الفهم الحقيقي للكتاب»."
                )
            ),
            sampleComments = listOf(
                0 to "وصف مكتبة قرطبة في الفقرة الأولى يجعلك تشعر وكأنك تتنفس عبق الورق القديم!",
                2 to "اقتباس رائع جداً عن الجسر بين لغة الكاتب وقلب القارئ."
            )
        ),
        SeedNovelTemplate(
            authorName = "Elena Vance",
            title = "Echoes of the Last Orbit — صدى المدار الأخير",
            synopsis = "A multilingual sci-fi odyssey aboard deep-space station Kepler-442b, where an astrophysicist intercepts an ancient poetic transmission bridging Earth's civilizations.",
            originalLanguage = "English",
            genre = "خيال علمي",
            targetAudience = "شباب وبالغين (18+)",
            keywords = listOf("فضاء", "مستقبل", "Kepler", "SciFi", "ترجمة"),
            coverColorHex = "#1B4965",
            isCompleted = false,
            chapters = listOf(
                "Chapter 1: Signal from the Golden Nebula" to listOf(
                    "In the quiet observatory orbiting Kepler-442b, Commander Elena adjusted the quantum receiver. For seven years, the deep space array had only whispered static, until tonight—when a melody composed in ancient Andalusian cadence echoed across the stars.",
                    "She leaned closer to the holographic console, watching golden glyphs materialize in mid-air. Each symbol carried coordinates not of a distant galaxy, but of a forgotten library buried beneath the sands of time.",
                    "\"If these signals are real,\" whispered navigator Tariq beside her, \"then humanity was never the first traveler to write its stories among the constellations.\""
                )
            ),
            sampleComments = listOf(
                0 to "جربت ميزة الترجمة الفورية للعربية هنا وكانت صياغة الفقرة مذهلة جداً!",
                2 to "شخصية الملاح طارق تضيف بعداً ثقافياً جميلاً للرواية."
            )
        ),
        SeedNovelTemplate(
            authorName = "Claire Moreau",
            title = "Les Jardins de Grenade — حدائق غرناطة المنسية",
            synopsis = "Un roman poétique et mystérieux où un jeune relieur découvre une carte tracée à l'encre de safran entre les pages d'un carnet andalou.",
            originalLanguage = "Français",
            genre = "أدب عالمي مترجم",
            targetAudience = "يافعين (13+)",
            keywords = listOf("غرناطة", "فرنسي", "مذكرات", "شعر", "Grenade"),
            coverColorHex = "#2D6A4F",
            isCompleted = true,
            chapters = listOf(
                "Chapitre 1 : L'Encre de Safran" to listOf(
                    "Sous les lanternes dorées du vieux quartier de Grenade, le jeune relieur découvrit entre les pages d'un manuscrit oublié une carte tracée à l'encre de safran. Chaque rue dessinée menait à un jardin secret que personne n'avait vu depuis cinq siècles.",
                    "Lorsque la pluie nocturne commença à tomber sur les toits d'argile, les lettres du parchemin s'illuminèrent doucement, révélant le journal intime d'une poétesse qui parlait aux étoiles."
                )
            ),
            sampleComments = listOf(
                0 to "الأجواء الفرنسية الأندلسية رائعة، ومع العرض المزدوج للترجمة أتعلم مفردات جديدة أثناء القراءة!"
            )
        ),
        SeedNovelTemplate(
            authorName = "مريم الشامي",
            title = "حارس قناديل دمشق",
            synopsis = "رواية فانتازيا ودراما اجتماعية تدور في حارات دمشق العتيقة، حيث يمتلك فانوس نحاسي قديم القدرة على إظهار الذكريات المنسية لكل من يقرأ النقوش المحفورة على زجاجه.",
            originalLanguage = "العربية",
            genre = "فانتازيا",
            targetAudience = "عائلي",
            keywords = listOf("دمشق", "فانتازيا", "قناديل", "ذكريات", "حارة"),
            coverColorHex = "#6D2E46",
            isCompleted = true,
            chapters = listOf(
                "الفصل الأول: قنديل الياسمين" to listOf(
                    "عند منعطف زقاق الياسمين، كان العمّ «أبو نزار» يشعل قناديل الحارة واحداً تلو الآخر، غير أن القنديل السابع كان يضيء بلونٍ كهرماني دافئ كلما مرّت بجانبه حكاية لم تكتمل.",
                    "اجتمع أطفال الحارة وشبابها حول الدكة الحجرية يستمعون إلى صوت الحكواتي، بينما كانت شجرة النارنج تتمايل مع نسيم المساء كأنها تصغي معهم لكل فصلٍ جديد."
                )
            ),
            sampleComments = listOf(
                0 to "تصلح جداً للاستماع بصوت الحكواتي الكلاسيكي في النمط الليلي!"
            )
        )
    )

    suspend fun seedInitialCatalogIfEmpty(repository: RawiFirestoreRepository) {
        val existing = repository.getPublicNovels().getOrNull() ?: return
        if (existing.isEmpty()) {
            for (seed in INITIAL_NOVELS) {
                val novelId = repository.createNovel(
                    authorName = seed.authorName,
                    title = seed.title,
                    synopsis = seed.synopsis,
                    originalLanguage = seed.originalLanguage,
                    genre = seed.genre,
                    targetAudience = seed.targetAudience,
                    keywords = seed.keywords,
                    coverColorHex = seed.coverColorHex,
                    isCompleted = seed.isCompleted
                ).getOrNull() ?: continue

                var firstChapterId: String? = null
                seed.chapters.forEachIndexed { idx, (chTitle, paragraphs) ->
                    val chId = repository.createChapter(
                        novelId = novelId,
                        chapterNumber = idx + 1,
                        title = chTitle,
                        paragraphs = paragraphs,
                        language = seed.originalLanguage
                    ).getOrNull()
                    if (idx == 0) firstChapterId = chId
                }
                repository.updateNovelChapterCount(novelId, seed.chapters.size, seed.isCompleted)

                val createdNovel = repository.getPublicNovels().getOrNull()?.find { it.id == novelId }
                if (createdNovel != null) {
                    repository.submitNovelReview(
                        novel = createdNovel,
                        userName = "ليلى المنصور (ناقدة أدبية)",
                        plotRating = 5,
                        styleRating = 5,
                        charactersRating = 5,
                        translationRating = 5,
                        reviewText = "عمل أدبي متكامل يجمع بين جمال السرد ودقة البناء الدرامي وسلاسة الترجمة."
                    )
                }

                if (firstChapterId != null) {
                    for ((pIdx, commentText) in seed.sampleComments) {
                        repository.addParagraphComment(
                            novelId = novelId,
                            chapterId = firstChapterId!!,
                            paragraphIndex = pIdx,
                            userName = "قارئ مميز",
                            commentText = commentText
                        )
                    }
                }
            }
        }

        val existingGroups = repository.getDiscussionGroups().getOrNull() ?: return
        if (existingGroups.isEmpty()) {
            val allNovels = repository.getPublicNovels().getOrNull().orEmpty()
            val cordobaNovel = allNovels.find { it.title.contains("قرطبة") } ?: allNovels.firstOrNull()
            val orbitNovel = allNovels.find { it.title.contains("Orbit") || it.title.contains("المدار") }
            val damascusNovel = allNovels.find { it.title.contains("دمشق") }

            val group1Id = repository.createDiscussionGroup(
                novelId = cordobaNovel?.id ?: "seed_cordoba",
                novelTitle = cordobaNovel?.title ?: "أسرار مكتبة قرطبة",
                authorName = cordobaNovel?.authorName ?: "د. طارق الأندلسي",
                groupTitle = "مجلس وراقي قرطبة — نقاش المخطوطات والتاريخ",
                description = "حلقة نقاش رسمية بإشراف د. طارق الأندلسي لتحليل شفرات الإسطرلاب النحاسي ومناقشة أحداث فصول الرواية التاريخية.",
                genre = cordobaNovel?.genre ?: "تاريخي"
            ).getOrNull() ?: "general"

            val group2Id = repository.createDiscussionGroup(
                novelId = orbitNovel?.id ?: "seed_orbit",
                novelTitle = orbitNovel?.title ?: "Echoes of the Last Orbit — صدى المدار الأخير",
                authorName = orbitNovel?.authorName ?: "Elena Vance",
                groupTitle = "نادي المدار الأخير والخيال العلمي المترجم",
                description = "مجموعة نقاش للقراء والمترجمين المهتمين بالفيزياء الفلكية والرسائل الكونية المشفرة في محطة Kepler-442b.",
                genre = orbitNovel?.genre ?: "خيال علمي"
            ).getOrNull() ?: "general"

            val group3Id = repository.createDiscussionGroup(
                novelId = damascusNovel?.id ?: "seed_damascus",
                novelTitle = damascusNovel?.title ?: "حارس قناديل دمشق",
                authorName = damascusNovel?.authorName ?: "مريم الشامي",
                groupTitle = "سمَر حارة الياسمين والواقعية السحرية",
                description = "مساحة دافئة لتبادل الآراء مع الكاتبة مريم الشامي حول رمزية القناديل السبعة والذكريات الدمشقية.",
                genre = damascusNovel?.genre ?: "فانتازيا"
            ).getOrNull() ?: "general"

            repository.updateDiscussionGroupMemberCount(group1Id, 42)
            repository.updateDiscussionGroupMemberCount(group2Id, 29)
            repository.updateDiscussionGroupMemberCount(group3Id, 35)

            // Seed Author Update
            repository.createCommunityPost(
                groupId = group1Id,
                novelId = cordobaNovel?.id ?: "",
                novelTitle = cordobaNovel?.title ?: "أسرار مكتبة قرطبة",
                authorName = "د. طارق الأندلسي",
                authorRole = "مؤلف",
                postType = "AUTHOR_UPDATE",
                content = "أهلاً بكم في مجلس قرطبة الأدبي! أعمل حالياً على الفصل الثالث بعنوان «خزانة الحكمة السرية» وسيتضمن وثيقة تاريخية مترجمة من اللاتينية والعربية. شاركوني توقعاتكم حول مصير الوراق حسن بن يحيى!"
            )

            // Seed Q&A Question with Author Answer
            repository.createCommunityPost(
                groupId = group1Id,
                novelId = cordobaNovel?.id ?: "",
                novelTitle = cordobaNovel?.title ?: "أسرار مكتبة قرطبة",
                authorName = "ليلى المنصور",
                authorRole = "قارئ",
                postType = "QA_QUESTION",
                content = "سؤال للمؤلف د. طارق: هل شخصية الوراق حسن بن يحيى مستوحاة من شخصية تاريخية حقيقية في عهد الخليفة الحكم المستنصر؟",
                authorAnswer = "نعم يا ليلى! الشخصية مستلهمة من كبير ناسخي خزانة قرطبة في القرن الرابع الهجري مع دمج خطوط درامية خيالية حول المخطوطة الرباعية.",
                answeredByName = "د. طارق الأندلسي"
            )

            // Seed Q&A Question open for Author Answer
            repository.createCommunityPost(
                groupId = group2Id,
                novelId = orbitNovel?.id ?: "",
                novelTitle = orbitNovel?.title ?: "Echoes of the Last Orbit — صدى المدار الأخير",
                authorName = "عمر الكيلاني",
                authorRole = "قارئ",
                postType = "QA_QUESTION",
                content = "كيف تم الدمج بين الإيقاعات الأندلسية القديمة وإشارات الراديو الكمومية في الفصل الأول من صدى المدار الأخير؟"
            )

            // Seed Group Discussion Post
            repository.createCommunityPost(
                groupId = group3Id,
                novelId = damascusNovel?.id ?: "",
                novelTitle = damascusNovel?.title ?: "حارس قناديل دمشق",
                authorName = "سارة الأندلسي",
                authorRole = "قارئ",
                postType = "GROUP_DISCUSSION",
                content = "فكرة القنديل السابع الذي يضيء بلون كهرماني كلما مرت بجانبه حكاية لم تكتمل جعلتني أعيد قراءة الفصل مرتين! ما رأيكم في رمزية شجرة النارنج في نهاية المشهد؟"
            )
        }
    }
}
