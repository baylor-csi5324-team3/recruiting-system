# Recruiting and Application Management System

**CSI5324 – Software Engineering · Fall 2026 · Team 3**
Baylor University · Instructor: Dr. Mengfei Ren

A web-based system that supports the major activities of recruiting and applying for
employment opportunities. It models a realistic recruiting environment with multiple
interacting user roles, role-based access control, business rules beyond basic CRUD,
persistent data, and a coherent end-to-end recruiting workflow.

## User Roles

| Role | Responsibility |
|---|---|
| **Applicant** | Maintains a profile, explores opportunities, submits applications, tracks progress |
| **Recruiter / HR** | Manages job postings, reviews applications, advances candidates, records decisions |
| **Administrator** | Manages accounts, roles and permissions, and system configuration |

## Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| Presentation | React | Component model suits role-specific dashboards |
| Backend | Java 21 · Spring Boot / Spring MVC | Recommended stack; mature layering and testing support |
| Business logic | Spring service layer | Keeps workflow rules out of controllers and entities |
| Persistence | Spring Data JPA | Clean separation between business logic and data access |
| Database | H2 (development) · PostgreSQL (production) | H2 needs no install for early iterations; JPA makes the swap cheap |
| Build | Maven (backend) · npm (frontend) | Maven is required by the course |
| Testing | JUnit 5 | Required by the course |
| Version control | Git · GitHub | Required by the course |
| Issue tracking | GitHub Issues · GitHub Projects | Required by the course |

## Repository Layout

```
recruiting-system/
├── backend/    Spring Boot application (Maven, JUnit)
├── frontend/   React application (npm)
├── docs/       Requirements, design models, meeting logs
├── .gitignore  Excludes build output and secrets from version control
└── README.md   This file
```

## Prerequisites

| Tool | Version | Check with |
|---|---|---|
| JDK | 21 (LTS) | `java -version` |
| Node.js | 20 or newer | `node -v` |
| Git | any recent | `git --version` |

Maven does **not** need to be installed — the backend ships with the Maven Wrapper
(`mvnw` / `mvnw.cmd`), which downloads the correct Maven version automatically.

## Getting Started

```bash
git clone https://github.com/baylor-csi5324-team3/recruiting-system.git
cd recruiting-system
```

**Backend** (from `backend/`):

```bash
./mvnw spring-boot:run     # Windows: .\mvnw.cmd spring-boot:run
```

**Backend tests:**

```bash
./mvnw test                # Windows: .\mvnw.cmd test
```

**Frontend** (from `frontend/`):

```bash
npm install
npm run dev
```

## Team

| Name | GitHub | Project Role |
|---|---|---|
| Luyu Niu | [@LuyuNiu](https://github.com/LuyuNiu) | TBD |
| TBD | TBD | TBD |
| TBD | TBD | TBD |
| TBD | TBD | TBD |

Every member also serves as a developer/tester for at least three use cases.

## Branching Model

- `main` — always working; changes arrive only through pull requests
- `feature/<short-description>` — one branch per use case or issue
- Open a pull request, get at least one teammate's review, then merge

## Implemented Use Cases (backend)

By default, the backend starts with an **in-memory H2 database** seeded with demo users
and job postings. For the Submit Application persistence demonstration, run the `demo`
profile to use a **file-backed H2 database** (see instructions below). Sign in is not
implemented yet: recruiter and administrator demo endpoints use the `X-Recruiter-Id`
and `X-Administrator-Id` headers, while the applicant demo uses a selected applicant ID.
These identifiers are for development only, not secure authentication.

| Use case | Owner | Endpoints |
|---|---|---|
| UC-01 Search Available Jobs | Luyu Niu | `GET /api/jobs/search` (asks for criteria) · `GET /api/jobs/search/results?keyword=&location=&employmentType=` (open postings, newest first; all parameters optional) |
| UC-09 Create and Publish a Job Posting | Luyu Niu | `GET /api/postings/new` · `POST /api/postings` (JSON: title, description, location, employmentType, closingDate → DRAFT) · `POST /api/postings/{id}/publish` |
| UC-15 Manage Accounts and Roles | Luyu Niu | `GET /api/accounts/change` · `GET /api/accounts/lookup?email=` · `PUT /api/accounts/{id}/role-status` (JSON: newRole and/or newStatus) |
| Submit Job Application | Yuting Mei | `POST /api/applications` (submit an application after validation) · `GET /api/applications?applicantId={id}` (retrieve submitted applications) · `POST /api/applicants/{id}/resumes` (upload a resume) |

Business-rule failures return `400 {"message": ...}` and unknown ids or emails return `404`.
The code follows the sequence diagrams and the design class diagram in the project
documentation: one `*Controller` per use case, `JobPostingService` / `AccountService` for the
rules, and `JobPosting` / `UserAccount` / `Company` / `Recruiter` / `Administrator` as entities.

## Documentation

See [`docs/`](docs/) for requirements, analysis and design models, and meeting logs.
Documentation is kept in this repository so that it evolves together with the code.


## Submit Job Application and My Applications (Yuting Mei)

This implementation reuses the team's existing `Application`, `JobPosting`, and `Applicant`
entities, services, and persistence layer rather than introducing a separate application
model. The applicant can select an open job, choose or upload a PDF/DOCX resume, add a cover
letter, review the application, and confirm submission.

**Submission workflow:**

1. Select an applicant and an open job posting.
2. Select an existing resume or upload one; optionally enter a cover letter.
3. Review and confirm the application.
4. `POST /api/applications` validates resume ownership, job availability/closing date, and
   the one-application-per-applicant-per-job rule.
5. The backend saves an `Application` with stage `SUBMITTED`, including a snapshot of the
   submitted resume and cover letter. A successful response confirms creation.
6. The **My Applications** page calls `GET /api/applications?applicantId={id}` to read
   records back from the backend database and display the application stage and details.

| Feature | URL / endpoint | Purpose |
|---|---|---|
| Submit Application UI | `http://localhost:8080/submit-application.html` | Select materials, review, and submit |
| My Applications UI | `http://localhost:8080/applications.html` | Query and display saved applications |
| Submit API | `POST /api/applications` | Validate and persist a new application |
| Application history API | `GET /api/applications?applicantId=1` | Read stored records for a selected applicant |
| Resume upload API | `POST /api/applicants/{id}/resumes` | Upload a PDF/DOCX resume for an applicant |

### Run and verify database persistence

From `backend/`, start the application using the **demo profile**:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

The demo profile uses file-backed H2 (`backend/data/rams.mv.db` when started from
`backend/`), unlike the default in-memory H2 configuration. To verify persistence:

1. Open `http://localhost:8080/submit-application.html` and submit a new application.
2. Open `http://localhost:8080/applications.html` to retrieve the stored application.
3. Refresh the page to confirm it is reloaded from the backend, not browser-only state.
4. Stop Spring Boot with `Ctrl+C`, restart using the **same command and working directory**,
   and revisit My Applications. The submitted application should still be present.
5. Optionally inspect `GET /api/applications?applicantId=1` or the H2 console to verify
   the stored record directly.

The demo seed data already includes OFFER-stage applications for some postings. Select an
open job not yet applied to (for the current seed, `Frontend Developer`) to test a new
submission; submitting the same job again should be rejected.

**Current scope and limitations:** The demo does not implement authenticated applicant
sessions or a recruiter-facing application review screen. The `frontend/` React workspace
is unchanged; these demonstration pages are served directly by Spring Boot on port 8080.
The existing Offer Response use case continues to use the same `Application` records.
The sample jobs CSV at `backend/src/main/resources/jobs-sample.csv` is a reference fixture,
not an automatic CSV importer.

For design decisions, API details, and integration constraints, see
[`docs/design/SUBMIT_APPLICATION_INTEGRATION.md`](docs/design/SUBMIT_APPLICATION_INTEGRATION.md).
