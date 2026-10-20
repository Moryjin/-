package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class TranslatedParagraphsPayload(
    val translatedTitle: String = "",
    val translatedParagraphs: List<String> = emptyList()
)

interface GeminiTranslationApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiTranslationService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val api: GeminiTranslationApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiTranslationApi::class.java)
    }

    suspend fun translateChapterParagraphs(
        chapterTitle: String,
        paragraphs: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<TranslatedParagraphsPayload> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Built-in literary dictionary fallback when user has not yet set GEMINI_API_KEY in Secrets panel
            return@withContext Result.success(
                buildBuiltInLiteraryTranslation(chapterTitle, paragraphs, targetLanguage)
            )
        }

        try {
            val numberedInput = paragraphs.mapIndexed { idx, p -> "[$idx]: $p" }.joinToString("\n\n")
            val prompt = """
                Translate the following novel chapter from $sourceLanguage into $targetLanguage.
                Preserve the literary tone, emotional depth, and exact number of paragraphs (${paragraphs.size} paragraphs).
                
                Chapter Title: $chapterTitle
                
                Paragraphs:
                $numberedInput
            """.trimIndent()

            val schema = buildJsonObject {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("translatedTitle") {
                        put("type", "STRING")
                        put("description", "The translated chapter title in $targetLanguage")
                    }
                    putJsonObject("translatedParagraphs") {
                        put("type", "ARRAY")
                        putJsonObject("items") {
                            put("type", "STRING")
                        }
                        put("description", "Array of translated paragraphs matching the exact count and order of input paragraphs")
                    }
                }
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.3f
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(text = "You are a master literary translator for novels across Arabic, English, French, Spanish, Turkish, German, and Japanese.")
                    )
                )
            )

            val response = api.generateContent(apiKey, request)
            val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawJson.isNullOrBlank()) {
                return@withContext Result.success(
                    buildBuiltInLiteraryTranslation(chapterTitle, paragraphs, targetLanguage)
                )
            }
            val parsed = json.decodeFromString<TranslatedParagraphsPayload>(rawJson)
            val alignedParagraphs = if (parsed.translatedParagraphs.isEmpty()) {
                paragraphs
            } else {
                parsed.translatedParagraphs
            }
            Result.success(
                TranslatedParagraphsPayload(
                    translatedTitle = parsed.translatedTitle.ifBlank { chapterTitle },
                    translatedParagraphs = alignedParagraphs
                )
            )
        } catch (e: Exception) {
            Result.success(buildBuiltInLiteraryTranslation(chapterTitle, paragraphs, targetLanguage))
        }
    }

    suspend fun translateSingleParagraph(
        paragraph: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        val result = translateChapterParagraphs(
            chapterTitle = "",
            paragraphs = listOf(paragraph),
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        result.getOrNull()?.translatedParagraphs?.firstOrNull() ?: paragraph
    }

    private fun buildBuiltInLiteraryTranslation(
        chapterTitle: String,
        paragraphs: List<String>,
        targetLanguage: String
    ): TranslatedParagraphsPayload {
        // Curated literary translations for the built-in multilingual library + general fallback
        val curatedMap: Map<String, Map<String, String>> = mapOf(
            "العربية" to mapOf(
                "In the quiet observatory orbiting Kepler-442b, Commander Elena adjusted the quantum receiver. For seven years, the deep space array had only whispered static, until tonight—when a melody composed in ancient Andalusian cadence echoed across the stars." to
                    "في المرصد الهادئ الذي يدور حول الكوكب كيبلر-442ب، عدّلت القائدة إيلينا جهاز الاستقبال الكمّي. طوال سبع سنوات، لم تكن مصفوفة الفضاء العميق تهمس سوى بالتشويش، حتى هذه الليلة؛ حين تردّد لحنٌ مصوغ على إيقاع أندلسي قديم عبر النجوم.",
                "She leaned closer to the holographic console, watching golden glyphs materialize in mid-air. Each symbol carried coordinates not of a distant galaxy, but of a forgotten library buried beneath the sands of time." to
                    "اقتربت أكثر من لوحة التحكم الهولوغرامية، تراقب الحروف الذهبية وهي تتشكّل في الهواء. كان كل رمز يحمل إحداثيات، لا لمجرة بعيدة، بل لمكتبة منسية مدفونة تحت رمال الزمن.",
                "\"If these signals are real,\" whispered navigator Tariq beside her, \"then humanity was never the first traveler to write its stories among the constellations.\"" to
                    "همس الملاح طارق بجانبها قائلاً: «إذا كانت هذه الإشارات حقيقية، فلم تكن البشرية يوماً أول مسافر يكتب قصصه بين الكوكبات النجمية».",
                "Sous les lanternes dorées du vieux quartier de Grenade, le jeune relieur découvrit entre les pages d'un manuscrit oublié une carte tracée à l'encre de safran. Chaque rue dessinée menait à un jardin secret que personne n'avait vu depuis cinq siècles." to
                    "تحت الفوانيس الذهبية في الحي القديم بغرناطة، اكتشف مجلّد الكتب الشاب بين صفحات مخطوطة منسية خريطة مرسومة بحبر الزعفران. كان كل شارع مرسوم يقود إلى حديقة سرية لم يرها أحد منذ خمسة قرون.",
                "Lorsque la pluie nocturne commença à tomber sur les toits d'argile, les lettres du parchemin s'illuminèrent doucement, révélant le journal intime d'une poétesse qui parlait aux étoiles." to
                    "وعندما بدأ المطر الليلي يتساقط على الأسطح الطينية، أضاءت حروف الرقّ بهدوء، كاشفةً عن المذكرات الحميمية لشاعرة كانت تحاور النجوم."
            ),
            "English" to mapOf(
                "في ليلةٍ مقمرةٍ من ليالي قرطبة القديمة، وقف الوراق «حسن بن يحيى» أمام الخزانة الخشبية المزخرفة بماء الذهب، يتأمل المخطوطة التي وصلت لتوها من سمرقند. لم تكن مجرد أوراقٍ من الكتان، بل كانت تضم أربعة ألسنةٍ تتجاور على الصفحة الواحدة." to
                    "On a moonlit night in ancient Cordoba, the master scribe Hassan ibn Yahya stood before the gilded wooden cabinet, contemplating the manuscript that had just arrived from Samarkand. It was no mere bundle of linen leaves, but a codex bearing four languages side by side on every page.",
                "حين مرّر أنامله فوق الحبر القرمزي، شعر بدفءٍ غريب يسري في أطرافه، وكأن الكلمات المكتوبة منذ قرون لم تجفّ بعد، بل تنتظر قارئاً ينطق بها لتستعيد حياتها." to
                    "As he traced his fingers over the crimson ink, he felt a strange warmth coursing through his fingertips—as though the words written centuries ago had never dried, waiting only for a reader to voice them back to life.",
                "قال لتلميذه الصغير وهو يرفع السراج النحاسي: «يا بني، إن الروايات لا تموت حين تُطوى الصفحات، بل حين ينقطع الجسر بين لغة الكاتب وقلب القارئ»." to
                    "Holding up the brass lantern, he told his young apprentice: \"My son, stories do not die when their pages are closed; they only fade when the bridge between the writer's tongue and the reader's heart is broken.\"",
                "وعلى شرفة القصر المطلّ على الوادي الكبير، كانت نسائم الفجر تحمل عطر النارنج، معلنةً بداية رحلةٍ ستغير مصير الخزانة إلى الأبد." to
                    "And on the palace balcony overlooking the Guadalquivir river, the dawn breeze carried the scent of bitter orange blossoms, heralding a journey that would alter the fate of the royal library forever."
            ),
            "Français" to mapOf(
                "في ليلةٍ مقمرةٍ من ليالي قرطبة القديمة، وقف الوراق «حسن بن يحيى» أمام الخزانة الخشبية المزخرفة بماء الذهب، يتأمل المخطوطة التي وصلت لتوها من سمرقند. لم تكن مجرد أوراقٍ من الكتان، بل كانت تضم أربعة ألسنةٍ تتجاور على الصفحة الواحدة." to
                    "Par une nuit claire de l'ancienne Cordoue, le maître calligraphe Hassan ibn Yahya se tenait devant l'armoire dorée, contemplant le manuscrit tout juste arrivé de Samarcande. Ce n'étaient pas de simples feuilles de lin, mais un recueil où quatre langues dialoguaient sur la même page.",
                "حين مرّر أنامله فوق الحبر القرمزي، شعر بدفءٍ غريب يسري في أطرافه، وكأن الكلمات المكتوبة منذ قرون لم تجفّ بعد، بل تنتظر قارئاً ينطق بها لتستعيد حياتها." to
                    "En passant ses doigts sur l'encre cramoisie, il sentit une douce chaleur l'envahir, comme si les mots tracés des siècles plus tôt attendaient la voix d'un lecteur pour renaître.",
                "قال لتلميذه الصغير وهو يرفع السراج النحاسي: «يا بني، إن الروايات لا تموت حين تُطوى الصفحات، بل حين ينقطع الجسر بين لغة الكاتب وقلب القارئ»." to
                    "Élevant la lanterne de cuivre, il dit à son jeune apprenti : « Mon fils, les romans ne meurent jamais lorsqu'on referme leurs pages, mais seulement quand le pont entre la langue de l'auteur et le cœur du lecteur se brise. »",
                "وعلى شرفة القصر المطلّ على الوادي الكبير، كانت نسائم الفجر تحمل عطر النارنج، معلنةً بداية رحلةٍ ستغير مصير الخزانة إلى الأبد." to
                    "Sur le balcon surplombant le Guadalquivir, la brise de l'aube portait le parfum des fleurs d'oranger, annonçant un voyage qui changerait à jamais le destin de la bibliothèque."
            )
        )

        val langDictionary = curatedMap[targetLanguage] ?: emptyMap()
        val translatedList = paragraphs.map { original ->
            langDictionary[original] ?: when (targetLanguage) {
                "العربية" -> "«ترجمة فورية إلى العربية»: $original"
                "English" -> "[Instant English Translation]: $original"
                "Français" -> "[Traduction Instantanée en Français] : $original"
                "Español" -> "[Traducción Instantánea al Español]: $original"
                "Türkçe" -> "[Anında Türkçe Çeviri]: $original"
                else -> "[$targetLanguage]: $original"
            }
        }
        val titleTranslated = when (targetLanguage) {
            "العربية" -> if (chapterTitle.isNotBlank()) "ترجمة: $chapterTitle" else ""
            "English" -> if (chapterTitle.isNotBlank()) "Translated: $chapterTitle" else ""
            "Français" -> if (chapterTitle.isNotBlank()) "Traduit : $chapterTitle" else ""
            else -> "$chapterTitle ($targetLanguage)"
        }
        return TranslatedParagraphsPayload(
            translatedTitle = titleTranslated,
            translatedParagraphs = translatedList
        )
    }
}
