# Security Specification (Phase 0 TDD)

## 1. Data Invariants
- **Authentication**: Every read and write operation across `/novels`, `/chapters`, `/paragraph_comments`, `/reviews`, and `/follows` requires `request.auth != null`.
- **Ownership & Identity Integrity**:
  - A `Novel` can only be created or deleted by `incoming().authorId == request.auth.uid` (and `existing().authorId == request.auth.uid`).
  - A `Chapter` can only be created, updated, or deleted if `authorId == request.auth.uid` and the parent `/novels/$(incoming().novelId)` exists and belongs to `request.auth.uid`.
  - A `ParagraphComment` can only be created if `userId == request.auth.uid` and `/chapters/$(incoming().chapterId)` exists.
  - A `NovelReview` can only be created if `userId == request.auth.uid`, ratings are between 1 and 5, and `/novels/$(incoming().novelId)` exists.
  - A `FollowSubscription` is strictly isolated to its owner (`userId == request.auth.uid`) for `get`, `list`, `create`, `update`, and `delete`.
- **Query Enforcement**: No blanket `allow list: if isSignedIn();`. Every `list` rule evaluates `resource.data` (`existing().visibility == 'PUBLIC'` or `existing().userId == request.auth.uid` or `existing().authorId == request.auth.uid`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("novels").where("visibility", "==", "PUBLIC").get()` -> Reject.
2. **Unfiltered Query Scraping**: `aliceDb.collection("follows").get()` without `.where("userId", "==", ALICE_UID)` -> Reject.
3. **Cross-User Follow Read**: `aliceDb.collection("follows").doc("bob_follow").get()` -> Reject.
4. **Identity Spoofing on Novel Creation**: Alice creates a novel with `authorId: BOB_UID` -> Reject.
5. **Shadow Field Injection**: Alice updates a novel with `{ isFeaturedAdmin: true }` -> Reject via `hasOnly()`.
6. **Orphaned Chapter Creation**: Alice creates a chapter referencing non-existent `novelId: "missing_novel"` -> Reject via `exists()`.
7. **Unauthorized Chapter Injection**: Bob creates a chapter under Alice's novel -> Reject because parent novel `authorId != BOB_UID`.
8. **Out-of-Bounds Rating**: Alice submits a `NovelReview` with `plotRating: 99` -> Reject via `isValidNovelReview()`.
9. **Immutable Field Tampering**: Alice updates `createdAt` or `authorId` on an existing novel -> Reject.
10. **Oversized String / Resource Poisoning**: Comment with `commentText` > 2000 chars or invalid ID -> Reject.
11. **Unbounded Array Poisoning**: Novel with 25 keywords (limit 10) -> Reject.
12. **Cross-User Comment Deletion**: Bob attempts to delete Alice's paragraph comment -> Reject.
