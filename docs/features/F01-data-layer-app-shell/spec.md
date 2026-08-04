# F01 — Data Layer & App Shell

Status: done
Depends on: —
PRD refs: §4 Data model, §5 Architecture, §9 Security

## Scope

**In scope**
- Room entities for all 6 tables (§4 of PRD): `CardSet`, `Card`, `Tag`,
  `CardTagCrossRef`, `PracticeSession`, `PracticeSessionResult`.
- DAOs for each entity with the base CRUD + observable-query methods needed
  by later features (exact query surface below).
- `FlashCardsDatabase` (Room database class, version 1).
- Hilt `DatabaseModule` providing the `RoomDatabase` instance and each DAO.
- Shared testable utilities: `IdGenerator` (`kotlin.uuid.Uuid` generation) and
  `TimeProvider` (current `Instant`), both interface + real impl, injected
  via Hilt — exist so repositories/viewmodels in later features are
  deterministic under test.
- A Room `TypeConverter` for `kotlin.uuid.Uuid` <-> `String`, so entity
  fields are typed `Uuid` while the underlying column stays `TEXT`.
- Navigation Compose scaffold: `Screen` sealed class/route constants for
  every screen in PRD §5, a `NavHost` in `MainActivity` wired to Hilt
  (`@AndroidEntryPoint`), each destination a placeholder composable
  (`Text("TODO: <ScreenName>")`) until its owning feature fills it in.
- App-level Hilt setup: `FlashCardsApplication : Application()` annotated
  `@HiltAndroidApp`, registered in `AndroidManifest.xml`.
- Release build config: `isMinifyEnabled = true`, `isShrinkResources = true`
  for the `release` build type (PRD §9).
- Confirm `AndroidManifest.xml` requests no permissions (no `INTERNET`).

**Out of scope**
- Any real screen content/CRUD logic — built by F02–F07, each replacing its
  placeholder destination.
- Repository classes — each owned by the feature that needs it (F02+), to
  keep this feature strictly the data/DI/nav foundation.

## Entities (Kotlin, `data/entity` package)

All PKs/FKs are `kotlin.uuid.Uuid`, persisted as `TEXT` via the `Converters`
class below — Room never sees a raw `String` id, and application code never
sees a raw `String` id either; the conversion happens once, at the DB
boundary. See PRD §4 for the full field list — copied here for exact Room
annotations:

```kotlin
@Entity(tableName = "card_sets")
data class CardSetEntity(
    @PrimaryKey val id: Uuid,
    val name: String,
    val createdAt: Instant,
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = CardSetEntity::class, parentColumns = ["id"],
        childColumns = ["setId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("setId")],
)
data class CardEntity(
    @PrimaryKey val id: Uuid,
    val setId: Uuid,
    val front: String,
    val back: String,
    val notes: String?,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastPracticedAt: Instant? = null,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: Uuid,
    val name: String, // unique index
)

@Entity(
    tableName = "card_tag_cross_ref",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("cardId"), Index("tagId")],
)
data class CardTagCrossRef(val cardId: Uuid, val tagId: Uuid)

@Entity(
    tableName = "practice_sessions",
    foreignKeys = [ForeignKey(entity = CardSetEntity::class, parentColumns = ["id"], childColumns = ["setId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("setId")],
)
data class PracticeSessionEntity(
    @PrimaryKey val id: Uuid,
    val setId: Uuid,
    val startedAt: Instant,
    val finishedAt: Instant,
    val requestedCardCount: Int,
)

@Entity(
    tableName = "practice_session_results",
    foreignKeys = [ForeignKey(entity = PracticeSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class PracticeSessionResultEntity(
    @PrimaryKey val id: Uuid,
    val sessionId: Uuid,
    val cardId: Uuid,
    val wasCorrect: Boolean,
)
```

Note: `CardSet` deletion cascades to `Card` and `PracticeSession`;
`PracticeSession` deletion cascades to `PracticeSessionResult`; `Card`/`Tag`
deletion cascades the cross-ref. Deleting a `Card` also removes any
`PracticeSessionResult` rows referencing it (FK on `cardId` is enforced at
`Card` level too — see F06 spec for the resulting history edge case).

`kotlin.uuid.Uuid` is marked `@ExperimentalUuidApi` in the Kotlin stdlib —
opt in module-wide via a compiler flag (see plan.md) rather than annotating
every file.

`Converters` (`@TypeConverter`):

```kotlin
class Converters {
    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun fromUuid(value: Uuid?): String? = value?.toString()
    @TypeConverter fun toUuid(value: String?): Uuid? = value?.let(Uuid::parse)
}
```

## DAOs (`data/dao` package)

Minimal surface needed by later features — each feature may add its own
`@Query` methods to these interfaces when it needs one (documented in that
feature's spec), rather than F01 guessing every future query:

- `CardSetDao`: `insert`, `update`, `delete`, `getById(id): CardSetEntity?`,
  `getAll(): Flow<List<CardSetEntity>>`.
- `CardDao`: `insert`, `update`, `delete`, `getById`,
  `getBySetId(setId): Flow<List<CardEntity>>`, `updateStats(id, timesCorrect, timesIncorrect, lastPracticedAt)`.
- `TagDao`: `insert`, `delete`, `getByName(name): TagEntity?`,
  `getAll(): Flow<List<TagEntity>>`.
- `CardTagCrossRefDao`: `insert`, `deleteByCardId(cardId)`,
  `getTagsForCard(cardId): Flow<List<TagEntity>>`.
- `PracticeSessionDao`: `insert`, `insertResults(results: List<PracticeSessionResultEntity>)`,
  `getSessionsBySetId(setId): Flow<List<PracticeSessionEntity>>`,
  `getResultsBySessionId(sessionId): Flow<List<PracticeSessionResultEntity>>`.

## DI

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FlashCardsDatabase = ...

    @Provides fun provideCardSetDao(db: FlashCardsDatabase) = db.cardSetDao()
    // ... one provider per DAO
}
```

`IdGenerator` (`newId(): Uuid`, real impl `Uuid.random()`) / `TimeProvider`
bound via `@Binds` in a `UtilModule` so tests
can substitute fakes (`FakeIdGenerator` returning a fixed sequence,
`FakeTimeProvider` returning a fixed `Instant`) without touching real Room
or `System`.

## Navigation scaffold

`Screen` sealed class with one object per route from PRD §5 (SetList,
SetDetail, CardEditor, SessionConfig, SessionPlay, SessionResults,
HistoryList, HistoryDetail, ImportExport). `NavHost(startDestination = Screen.SetList.route)`
in `MainActivity`, each `composable(route) { Text("TODO") }` until replaced.
Nav Compose route arguments are always `String` (Navigation has no `Uuid`
`NavType`) — any screen taking an id arg (`setId`, `cardId`, `sessionId`)
parses it with `Uuid.parse(arg)` immediately, at the screen/ViewModel entry
point, so `Uuid` is the type used everywhere past that boundary. Later
features follow this convention rather than re-deciding it per screen.

## Acceptance criteria
- [ ] App builds and launches to a placeholder Set List screen with no crash.
- [ ] All 6 entities + DAOs compile, Room schema exported (`exportSchema = true`,
      schema JSON checked into `app/schemas/`).
- [ ] Cascade deletes verified by DAO tests (see plan.md).
- [ ] `IdGenerator`/`TimeProvider` are the only sources of UUIDs/timestamps
      anywhere in the data layer (no direct `Uuid.random()` or
      `Instant.now()` calls outside their real implementations).
- [ ] No entity, DAO signature, or repository/viewmodel contract in this or
      later features exposes a raw `String` id — always `Uuid`.
- [ ] Release build config has minify + resource shrinking on.
- [ ] No `INTERNET` permission in the manifest.
