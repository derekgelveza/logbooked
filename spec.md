MVP Specification — Reading Tracker (working title)

A "Letterboxd for books": a web app where you add books, rate and review them, organise them onto shelves, and log the days you actually read so you build a streak and a reading history.

This document describes what the MVP does. The companion ARCHITECTURE.md describes how to build it (classes, methods, endpoints, dependencies, build order).

1. Tech stack
   Layer	Choice	Why
   Backend	Java + Spring Boot	Batteries-included; Spring Security & Spring Data JPA remove most auth/DB work
   Data access	Spring Data JPA	Define entities as classes; queries generated for you
   Database	PostgreSQL (SQLite/H2 fine while developing)	Relational data with clear relationships
   Book data	Open Library API	Free, no API key, search + covers + metadata
   Frontend	React (JS)	Largest ecosystem and help availability
   Auth	Session-based, email + password	Simplest secure option for a web app
2. Core data model

Four entities. Everything hangs off these.

User
Field	Type	Notes
id	long / UUID	primary key
username	string, unique	stable, public-facing handle (needed for future profiles)
email	string, unique	login identifier
passwordHash	string	BCrypt hash — never store plain text
createdAt	timestamp
Book (canonical — one shared record per book)
Field	Type	Notes
id	long / UUID	internal primary key
openLibraryId	string, unique	Open Library work/edition key; used to dedupe on add
title	string
author	string	comma-joined if multiple
coverUrl	string, nullable	from Open Library cover id
pageCount	int, nullable	from Open Library; often missing/edition-dependent
publishedYear	int, nullable
isbn	string, nullable
createdAt	timestamp

The first time anyone adds a given book, you create one Book row keyed by openLibraryId. Everyone's entries point at that same row (this is what enables book pages and rating averages later).

Entry (a user's relationship to a book — the log/review)
Field	Type	Notes
id	long / UUID	primary key
userId	FK → User
bookId	FK → Book
status	enum	WANT_TO_READ / READING / READ
rating	int, nullable	1–10 where each unit = half a star; null = unrated
reviewText	string, nullable	optional written review
visibility	enum	PUBLIC (default) / PRIVATE — present now, enforced later
startedAt	date, nullable	first day read
finishedAt	date, nullable	day marked READ
totalPagesOverride	int, nullable	user's own page count when Open Library's is wrong/missing
createdAt / updatedAt	timestamp

Constraint: unique (userId, bookId) — one entry per user per book for the MVP. (Re-reads come later.)

ReadingSession (the reading log — one row per day you read a book)
Field	Type	Notes
id	long / UUID	primary key
entryId	FK → Entry
date	date	the calendar day read
currentPage	int	cumulative page reached that day (not pages-that-day)
createdAt	timestamp

Constraint: unique (entryId, date) — one log per book per day.

Everything about progress and streaks is derived from these rows:

pages that day = currentPage − previous session's currentPage
progress % = currentPage ÷ (totalPagesOverride ?? book.pageCount)
per-book active days = number of sessions for that entry
global streak = consecutive calendar days that have at least one session across all the user's entries
heatmap = sessions grouped by date
Relationships
User 1───* Entry *───1 Book
│
1
│
*
ReadingSession
3. Features
   3.1 Sign up & Login
   Register with username + email + password.
   Log in with email + password; session keeps you logged in.
   Password reset flow included.
   Email verification: minimal or skipped for MVP (avoids email infra).
   Google / social login: deferred (email/password only for v1).
   3.2 Security

Not a standalone screen — a posture woven through everything:

Passwords hashed with BCrypt.
Session-based authentication.
Authorization: a user can only view/edit/delete their own entries and sessions.
Input validation on all write endpoints.
HTTPS in deployment.

(See ARCHITECTURE.md → build order: the hardening work is the final phase, but user identity is stubbed from the start.)

3.3 Add a book
Search by title (and/or ISBN) → calls Open Library → shows results with cover, author, year.
Selecting a result gets-or-creates the canonical Book, then creates an Entry for the current user with a chosen initial status.
Fallback: if a book isn't in Open Library, allow manual entry (title, author, page count).
3.4 Review a book
Rating: 5 stars with half-stars (stored as int 1–10; 0.5★ = 1, 5★ = 10). Optional.
Review text: optional.
Visibility: public by default.
3.5 Shelves (reading status)
Every entry has a status: Want to read / Reading / Read.
Moving to READING sets startedAt; moving to READ sets finishedAt.
Library view can filter by status.
3.6 Reading log (days read)
On a READING book, log a session: pick a date (today or backfilled) and the current page you've reached.
Derived and displayed:
Progress bar (current page ÷ total pages).
Per-book active days ("read on 8 different days").
Global streak ("12 days in a row") — across all books.
Calendar heatmap of the reading year.
Backfilling past dates is allowed.
4. Decisions log (with rationale)
   Decision	Choice	Why
   Book source	Open Library	Free, no key, good coverage
   Book storage	Canonical shared records	Enables book pages, averages, discovery later
   Rating scale	5★ + half, stored 1–10 int	Avoids decimals; Letterboxd-style
   Rating/review	Optional	Log without judging, like Letterboxd
   Review visibility	Public default, PRIVATE field present	Social direction, private accounts cheap to add later
   Days read	Active-day logging	Streak + heatmap; more distinctive than elapsed days
   Session model	Cumulative current page	Self-correcting; free progress bar
   Streak scope	Global and per-book	Global is the hook, per-book is the stat
   Page count	Open Library + user override	OL data is inconsistent
   Entries	One per user-book	Simpler MVP; re-reads later
   Social	Solo MVP, social-ready data	Cheap to layer on (see below)
   Auth	Session-based, email/password	Simplest secure web option
5. Deferred (post-MVP)

Data model is already prepared for most of these — they're additive, not rewrites.

Viewing other users' profiles and book pages (rows already exist).
Following + activity feed (one new join table).
Likes, comments, notifications (defer hardest — moderation/spam overhead).
Google / social login.
Email verification.
Private accounts (the visibility field is already there).
Re-reads (relax the unique (userId, bookId) constraint).
Spoiler flags on reviews.
Aggregate book pages: average rating, "readers also logged".
Discovery / search across users, recommendations.
Import from Goodreads/StoryGraph.
Native mobile app.
6. Social-readiness guardrails (keep these in the MVP even though it's solo)

Starting solo costs almost no rework if you preserve three things now:

Give every user a stable username immediately.
Keep the visibility field on entries (default public) from day one.
Keep "can this user act on this resource?" authorization clean from the start.

Do those and "add social" becomes a feature you layer on, not a rebuild.