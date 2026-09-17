# Architecture & Build Blueprint

Companion to `SPEC.md`. This is the scaffolding — class names, responsibilities,
method signatures, endpoints, dependencies, and the order to build in. Method
bodies are left for you; the algorithms for the two tricky bits are described in
words in §8.

---

## 1. Layered shape (backend)

```
Controller  →  Service  →  Repository  →  Database
   (HTTP)      (logic)     (Spring Data)     (JPA)
                  │
                  └──→ OpenLibraryClient (external API)
DTOs cross the Controller boundary; entities stay inside Service/Repository.
```

Keep entities out of your controllers — map to/from DTOs so you don't leak
`passwordHash` or couple your API to your table shape.

---

## 2. Dependencies

### Backend (Maven/Gradle starters)
| Dependency | Purpose | Phase |
| --- | --- | --- |
| `spring-boot-starter-web` | REST controllers, JSON, built-in `RestClient` | from start |
| `spring-boot-starter-data-jpa` | Entities & repositories | from start |
| `org.postgresql:postgresql` | Postgres driver | from start |
| `com.h2database:h2` | In-memory DB for quick dev/tests | optional |
| `spring-boot-starter-validation` | `@Valid` on request DTOs | from start |
| `spring-boot-starter-security` | Auth, BCrypt, endpoint locking | **security phase** |
| `org.projectlombok:lombok` | Cuts getter/setter/constructor boilerplate | optional |
| `flyway-core` | Versioned DB migrations | optional but recommended |
| `spring-boot-starter-test` | JUnit, MockMvc | included |

Notes:
- **Open Library needs no dependency** — use the `RestClient` that ships with the web starter.
- Prefer **sessions** over JWT for a first web app (no extra deps, secure defaults). Only reach for `jjwt` if you later go stateless.

### Frontend
| Dependency | Purpose |
| --- | --- |
| `vite` + `react` + `react-dom` | App scaffold |
| `react-router-dom` | Page routing |
| `axios` (or `fetch`) | API calls |
| `@tanstack/react-query` | Server state/caching | *optional but very nice for this app* |
| a styling choice (Tailwind, CSS modules, etc.) | your call |

---

## 3. Entities (`@Entity`)

- **User** — `id, username, email, passwordHash, createdAt`
- **Book** — `id, openLibraryId, title, author, coverUrl, pageCount, publishedYear, isbn, createdAt`
- **Entry** — `id, user, book, status, rating, reviewText, visibility, startedAt, finishedAt, totalPagesOverride, createdAt, updatedAt`
- **ReadingSession** — `id, entry, date, currentPage, createdAt`
- **Enums** — `ReadingStatus { WANT_TO_READ, READING, READ }`, `Visibility { PUBLIC, PRIVATE }`

Constraints to declare: unique `User.username`, unique `User.email`, unique
`Book.openLibraryId`, unique `Entry(user, book)`, unique `ReadingSession(entry, date)`.

---

## 4. Repositories (Spring Data JPA interfaces)

Each extends `JpaRepository<T, IdType>`. Custom finders you'll want:

- **UserRepository** — `findByEmail`, `findByUsername`, `existsByEmail`, `existsByUsername`
- **BookRepository** — `findByOpenLibraryId`
- **EntryRepository** — `findByUser`, `findByUserAndStatus`, `findByUserAndBook`, `findByIdAndUser` (the last one is your authorization workhorse)
- **ReadingSessionRepository** — `findByEntryOrderByDateAsc`, `findByEntry_UserAndDateBetween` (for the heatmap/streak), `findTopByEntryOrderByDateDesc` (previous page for delta)

---

## 5. Services

Signatures are sketches — return types can be entities or DTOs as you prefer.

### OpenLibraryClient
- `List<BookSearchResult> search(String query)` — hit the search endpoint, map JSON → results
- `BookDetails fetchByOpenLibraryId(String olid)` — for cover/pages when adding

### BookService
- `Book getOrCreate(String openLibraryId)` — dedupe: return existing or create canonical row
- `Book createManual(ManualBookRequest req)` — the not-in-Open-Library fallback

### EntryService
- `Entry addBook(User user, AddBookRequest req)` — get-or-create Book, then create Entry (guard against duplicates via the unique constraint)
- `List<Entry> listForUser(User user, ReadingStatus filter)`
- `Entry get(User user, long entryId)` — must use `findByIdAndUser`
- `Entry update(User user, long entryId, UpdateEntryRequest req)` — rating, review, status, page override; on status transitions set `startedAt`/`finishedAt`
- `void delete(User user, long entryId)`

### ReadingLogService
- `ReadingSession logSession(User user, long entryId, LogSessionRequest req)` — validate ownership + date + page; upsert on `(entry, date)`
- `ProgressResponse progressFor(long entryId)` — current page, total pages, %, active-day count
- `StreakResponse globalStreak(User user)` — current + longest streak
- `List<HeatmapCell> heatmap(User user, int year)` — date → pages/intensity

### AuthService / UserService  *(security phase, but stub the "current user" now)*
- `User register(RegisterRequest req)` — check uniqueness, hash password, save
- `User loadByEmail(String email)` — for Spring Security
- `User currentUser()` — **stub** early (return a fixed dev user), replace with the real security-context lookup later

---

## 6. Controllers & REST endpoints

| Method | Path | Purpose | Phase |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Create account | security |
| POST | `/api/auth/login` | Log in (session) | security |
| POST | `/api/auth/logout` | Log out | security |
| GET | `/api/books/search?q=` | Proxy Open Library search | features |
| POST | `/api/entries` | Add book to shelves (get-or-create Book + Entry) | features |
| GET | `/api/entries?status=` | List my entries, optional filter | features |
| GET | `/api/entries/{id}` | One entry | features |
| PATCH | `/api/entries/{id}` | Update status/rating/review/pages | features |
| DELETE | `/api/entries/{id}` | Remove entry | features |
| POST | `/api/entries/{id}/sessions` | Log a reading day | features |
| GET | `/api/entries/{id}/sessions` | Sessions + progress for a book | features |
| DELETE | `/api/sessions/{id}` | Remove a logged day | features |
| GET | `/api/me/streak` | Global streak | features |
| GET | `/api/me/heatmap?year=` | Calendar data | features |
| GET | `/api/me/profile` | Dashboard aggregate | features |

Controllers: `AuthController`, `BookController`, `EntryController`,
`ReadingLogController`, `ProfileController`.

---

## 7. DTOs (suggested)

Requests: `RegisterRequest`, `LoginRequest`, `AddBookRequest` (olid + initial
status), `ManualBookRequest`, `UpdateEntryRequest`, `LogSessionRequest` (date +
currentPage).

Responses: `BookSearchResult`, `EntryResponse`, `SessionResponse`,
`ProgressResponse`, `StreakResponse`, `HeatmapCell`, `ProfileResponse`.

---

## 8. The two computations worth thinking through

**Pages read on a given day.** Sessions store the *cumulative* page reached. For
any session, pages-that-day = its `currentPage` minus the `currentPage` of the
previous session for that entry (ordered by date). The very first session's
delta is just its `currentPage`. Progress % is `currentPage ÷ effectiveTotal`,
where `effectiveTotal = totalPagesOverride ?? book.pageCount` (guard against a
null/zero total so you don't divide by zero).

**Global streak.** Collect the set of *distinct dates* on which the user logged
any session (across all entries). Sort them. Walk from today backwards: the
current streak is the run of consecutive calendar days ending today (or
yesterday, if you want to be forgiving about "haven't logged yet today"). Longest
streak is the longest consecutive run anywhere in the set. Working from a
`Set<LocalDate>` keeps this simple and avoids double-counting two books read the
same day.

---

## 9. Frontend structure

**Pages:** `SignUp`, `Login`, `SearchAndAdd`, `Library` (shelves, filter by
status), `EntryDetail` (rating, review, status, progress bar, session logger),
`Dashboard` (streak, heatmap, stats).

**Reusable components:** `StarRating` (half-star aware), `ProgressBar`,
`ReadingHeatmap` (calendar grid), `StreakBadge`, `SessionLogger`, `BookCard`,
`StatusPicker`.

**One API client module** wrapping every endpoint in §6, so components never
build URLs themselves.

---

## 10. Recommended build order

Feature-first, security-hardening-last — with user identity stubbed from day one
so ownership is baked in (see the note in the chat / `SPEC.md` §2).

0. **Setup** — Spring Boot project, DB connection, the four entities + repositories, and a **stubbed `currentUser()`** returning a fixed dev user. Seed one user row.
1. **Add a book (vertical slice)** — `OpenLibraryClient` → `BookService.getOrCreate` → `POST /api/entries` → search-and-add UI. *Do this first: Open Library is your riskiest external dependency, so prove it early.*
2. **Shelves** — status on entries, `Library` page with status filter.
3. **Review** — rating (`StarRating`) + review text on `EntryDetail`.
4. **Reading log** — sessions, progress bar, per-book active days, global streak, heatmap. The meatiest feature; §8 is the tricky part.
5. **Dashboard** — aggregate streak + heatmap + stats.
6. **Security phase (hardening)** — see checklist below. This is where you replace the stub with real auth.

---

## 11. Security phase checklist (the final phase)

- Add `spring-boot-starter-security`.
- Real `register` + `login`; hash passwords with `BCryptPasswordEncoder`.
- Wire `UserDetailsService` to `UserRepository`; enable session-based auth.
- Replace `currentUser()` stub with the security-context lookup.
- Lock down every endpoint except register/login/book-search.
- Enforce **ownership** everywhere: every entry/session read or write goes through `findByIdAndUser` (never trust an id from the client alone).
- `@Valid` on all request DTOs; sensible field constraints.
- Configure CORS for the React dev origin; handle CSRF for the SPA (or use same-site session cookies).
- Add the password-reset flow.
- Confirm no entity leaks `passwordHash` through a DTO.