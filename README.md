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

## Documentation

See [`docs/`](docs/) for requirements, analysis and design models, and meeting logs.
Documentation is kept in this repository so that it evolves together with the code.

## AI-Assisted Development

Per the project requirements, responsible use of AI-assisted development tools is
permitted for this team project. Every team member remains fully responsible for the
correctness and quality of submitted work and must be able to explain all code, tests,
architecture, and design decisions.

Note that the course syllabus prohibits AI assistance on other coursework unless the
instructor explicitly permits it.
