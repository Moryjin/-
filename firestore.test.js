const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_author_123";
const BOB_UID = "bob_reader_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read novels, follows, author_profiles, or reading_progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("novels").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("follows").get());
  await assertFails(unauthDb.collection("author_profiles").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("reading_progress").get());
});

test("Authenticated user: can create and query public novels and author profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("author_profiles").doc(ALICE_UID).set({
      authorId: ALICE_UID,
      displayName: "أليس الكاتبة",
      bio: "روائية متخصصة في الأدب التاريخي والمخطوطات الأندلسية.",
      specialtyGenres: ["تاريخي", "فانتازيا"],
      websiteUrl: "https://rawi.example.com/alice",
      twitterHandle: "@alice_writes",
      instagramHandle: "@alice_novels",
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("novels").doc("novel_1").set({
      authorId: ALICE_UID,
      authorName: "أليس الكاتبة",
      title: "مدن الرماد والذهب",
      synopsis: "رواية خيال تاريخي تدور في مكتبة بغداد القديمة.",
      originalLanguage: "العربية",
      genre: "تاريخي",
      targetAudience: "بالغين",
      keywords: ["بغداد", "مخطوطات", "تاريخ"],
      coverColorHex: "#8C2D19",
      chapterCount: 1,
      ratingCount: 0,
      averageRating: 0,
      plotRatingAvg: 0,
      styleRatingAvg: 0,
      charactersRatingAvg: 0,
      translationRatingAvg: 0,
      isCompleted: false,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("novels").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(bobDb.collection("author_profiles").where("visibility", "==", "PUBLIC").get());
});

test("Authenticated user: reading progress is strictly isolated per user", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    bobDb.collection("reading_progress").doc("bob_novel_1").set({
      userId: BOB_UID,
      novelId: "novel_1",
      novelTitle: "مدن الرماد والذهب",
      genre: "تاريخي",
      lastReadChapterId: "ch_1",
      lastReadChapterNumber: 1,
      lastReadParagraphIndex: 2,
      completedChapterIds: ["ch_1"],
      totalChapters: 2,
      progressFraction: 0.5,
      createdAt: now,
      updatedAt: now,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("reading_progress").doc("bob_novel_1").get());
  await assertSucceeds(bobDb.collection("reading_progress").where("userId", "==", BOB_UID).get());
});

test("Authenticated user: can create discussion groups and community posts (updates, Q&A, discussions)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("discussion_groups").doc("group_1").set({
      novelId: "novel_1",
      novelTitle: "مدن الرماد والذهب",
      authorId: ALICE_UID,
      authorName: "أليس الكاتبة",
      groupTitle: "نادي نقاش مدن الرماد",
      description: "حلقة نقاش رسمية مع المؤلفة والقراء.",
      genre: "تاريخي",
      memberCount: 1,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("community_posts").doc("post_qa_1").set({
      groupId: "group_1",
      novelId: "novel_1",
      novelTitle: "مدن الرماد والذهب",
      authorId: BOB_UID,
      authorName: "بوب القارئ",
      authorRole: "قارئ",
      postType: "QA_QUESTION",
      content: "متى يصدر الفصل القادم من الرواية؟",
      authorAnswer: "",
      answeredByName: "",
      likesCount: 0,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("community_posts").doc("post_qa_1").update({
      authorAnswer: "سيصدر الفصل القادم يوم الجمعة القادم!",
      answeredByName: "أليس الكاتبة",
      updatedAt: new Date(),
    })
  );
});

