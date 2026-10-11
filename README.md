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

The backend starts with an in-memory H2 database seeded with one company, one recruiter
(id 1), one administrator (id 1) and a few postings, so every endpoint below can be called
right after `mvnw spring-boot:run`. Sign in is not implemented yet: a recruiter is identified
by the `X-Recruiter-Id` header and an administrator by `X-Administrator-Id`.

| Use case | Owner | Endpoints |
|---|---|---|
| UC-01 Search Available Jobs | Luyu Niu | `GET /api/jobs/search` (asks for criteria) · `GET /api/jobs/search/results?keyword=&location=&employmentType=` (open postings, newest first; all parameters optional) |
| UC-09 Create and Publish a Job Posting | Luyu Niu | `GET /api/postings/new` · `POST /api/postings` (JSON: title, description, location, employmentType, closingDate → DRAFT) · `POST /api/postings/{id}/publish` |
| UC-15 Manage Accounts and Roles | Luyu Niu | `GET /api/accounts/change` · `GET /api/accounts/lookup?email=` · `PUT /api/accounts/{id}/role-status` (JSON: newRole and/or newStatus) |

Business-rule failures return `400 {"message": ...}` and unknown ids or emails return `404`.
The code follows the sequence diagrams and the design class diagram in the project
documentation: one `*Controller` per use case, `JobPostingService` / `AccountService` for the
rules, and `JobPosting` / `UserAccount` / `Company` / `Recruiter` / `Administrator` as entities.

## Documentation

See [`docs/`](docs/) for requirements, analysis and design models, and meeting logs.
Documentation is kept in this repository so that it evolves together with the code.

