# Recruiting and Application Management System

**CSI 5324 Software Engineering I · Fall 2026 · Team 3 "Five Guys"**
Baylor University · Instructor: Dr. Mengfei Ren

A web system for posting jobs, applying for them and following each application through the
recruiting process. Applicants search and apply, recruiters post jobs and move applications
through the stages, administrators manage accounts and roles. Interviews and offer negotiation
happen outside the system; the system records the resulting stage.

Current release: **v2.5.2** (Iteration 2): all 14 use cases implemented, 134 back-end tests.

## Team

| Member | Role | Use cases |
|---|---|---|
| Luyu Niu ([@LuyuNiu](https://github.com/LuyuNiu)) | Project Manager | 4.2.1.1 Search Available Jobs · 4.2.2.1 Create and Publish a Job Posting · 4.2.3.1 Manage Accounts and Roles |
| Xinyi Chen ([@cchenxinyii](https://github.com/cchenxinyii)) | Requirements Engineer | 4.2.1.2 Register Account · 4.2.1.3 User Login · 4.2.1.4 Update Applicant Profile |
| Yuting Mei ([@yutingmeivu](https://github.com/yutingmeivu)) | Design Engineer | 4.2.1.5 Submit Job Application · 4.2.1.6 Save Job Posting · 4.2.2.2 Manage Job Posting |
| Spencer Hammack ([@Spammck](https://github.com/Spammck)) | Librarian | 4.2.1.7 Track Application Stage · 4.2.1.8 Withdraw an Application · 4.2.2.5 Extend Job Offer |
| Zhengshi (Peter) Chen ([@lycoris9029](https://github.com/lycoris9029)) | Quality Assurance | 4.2.2.3 Review Submitted Applications · 4.2.2.4 Update Application Stage |

Project board: Jira, https://csi5324.atlassian.net (project KAN).

## Technology

| Layer | Technology |
|---|---|
| Front end | React + TypeScript (Vite), in `frontend/` |
| Back end | Java 21, Spring Boot 4 (Spring Web MVC), in `backend/` |
| Persistence | Spring Data JPA (Hibernate), H2 database (in memory by default, file with the `demo` profile) |
| Security | Spring Security Crypto (BCrypt password hashes), HTTP session for sign-in |
| Build and tests | Maven Wrapper and JUnit 5 (back end), npm and Vitest (front end) |

## Prerequisites

| Tool | Version | Check with |
|---|---|---|
| JDK | 21 | `java -version` |
| Node.js | 22.12 or newer (with npm) | `node -v` |
| Git | any recent | `git --version` |

You do not need to install Maven. The back end ships the Maven Wrapper (`mvnw` / `mvnw.cmd`),
which downloads Maven on first use.

## Build, run and test

Unzip `Team3_Iteration2_Implementation.zip` and open a terminal in the `recruiting-system` folder, or clone the repository:

```bash
git clone https://github.com/baylor-csi5324-team3/recruiting-system.git
cd recruiting-system
```

**1. Back end tests** (from `backend/`):

```bash
./mvnw test                      # Windows: .\mvnw.cmd test
```

Expected result: `Tests run: 134, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

**2. Back end** (from `backend/`, keep this terminal open):

```bash
./mvnw spring-boot:run           # Windows: .\mvnw.cmd spring-boot:run
```

The back end listens on http://localhost:8080. The sample data loads a few seconds after start.

**3. Front end** (from `frontend/`, in a second terminal):

```bash
npm install                      # once
npm run dev
```

Open http://localhost:5173. The front end forwards every `/api` request to the back end on
port 8080, so the back end must already be running. Front end test: `npm test`.

**4. Sign in.** Click **Sign in** at the top right and use one of the sample accounts.
All share the development password `Demo-pass-2026` (setting `rams.seed.demo-password`).

| Account | Role | Account id |
|---|---|---|
| admin@example.com (Ada Admin) | Administrator | 1 |
| recruiter@example.com (Rhea Recruiter, company Baylor Tech) | Recruiter | 2 |
| applicant@example.com (Peter Chen, two applications in the OFFER stage) | Applicant | 3 |
| dormant@example.com (Dana Dormant) | Applicant, deactivated: sign-in is refused | 4 |

Every start also imports the five postings of `backend/src/main/resources/jobs.csv`
(three open, one expired, one draft).

**Useful extras**

- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:rams`, user `sa`, no password).
- Saved jobs are kept in `backend/data/demo-workspace.json`. For clean data, stop the back end and delete the `backend/data` folder.
- Keep data between restarts: `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`
  (file database in `backend/data/`).
- Port 8080 or 5173 already in use: stop the other program, or start the back end with
  `-Dspring-boot.run.arguments=--server.port=8081` and change the proxy target in `frontend/vite.config.ts`.

## Identity and sign-in

User Login (`POST /api/accounts/login`) checks the email and password against the BCrypt hash and
keeps the user in the HTTP session (`GET /api/accounts/me`, `POST /api/accounts/logout`).
The endpoints read the caller from the `X-Applicant-Id`, `X-Recruiter-Id` and
`X-Administrator-Id` headers (the account id from the table above). `IdentityCheckInterceptor`
rejects a request without a session (401) and a request whose id belongs to another user (403).
The check is on by default (`rams.login.enforce=true`) and off in the tests.

To call the API without signing in, for example from curl or from the two plain pages below:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--rams.login.enforce=false
```

`UserAccount` is an abstract JPA entity (joined inheritance); `Applicant`, `Recruiter` and
`Administrator` extend it, as in the domain model. Changing a role replaces the account object.

## Use cases and endpoints

| Use case | Owner | Endpoints |
|---|---|---|
| 4.2.1.1 Search Available Jobs | Luyu Niu | `GET /api/jobs/search` · `GET /api/jobs/search/results?keyword=&location=&employmentType=` (open postings, newest first; all parameters optional; no sign-in needed) |
| 4.2.1.2 Register Account | Xinyi Chen | `POST /api/accounts/register` |
| 4.2.1.3 User Login | Xinyi Chen | `POST /api/accounts/login` (JSON: email, password) · `GET /api/accounts/me` · `POST /api/accounts/logout` |
| 4.2.1.4 Update Applicant Profile | Xinyi Chen | `GET /api/profile` · `PUT /api/profile` (JSON: name, phone, location, headline, bio; `X-Applicant-Id`) |
| 4.2.1.5 Submit Job Application | Yuting Mei | `GET, POST /api/applicants/{id}/resumes` · `POST /api/applications` · `GET /api/applications?applicantId=` |
| 4.2.1.6 Save Job Posting | Yuting Mei | `GET, PUT /api/demo/saved/{applicantId}` |
| 4.2.1.7 Track Application Stage | Spencer Hammack | `GET /api/applications/tracking` · `GET /api/applications/{id}` (`X-Applicant-Id`) |
| 4.2.1.8 Withdraw an Application | Spencer Hammack | `POST /api/applications/{id}/withdraw` (`X-Applicant-Id`) |
| 4.2.2.1 Create and Publish a Job Posting | Luyu Niu | `GET /api/postings/new` · `POST /api/postings` (draft) · `POST /api/postings/{id}/publish` (`X-Recruiter-Id`) |
| 4.2.2.2 Manage Job Posting | Yuting Mei | `PUT /api/postings/{id}` (edit) · `POST /api/postings/{id}/close` (`X-Recruiter-Id`) |
| 4.2.2.3 Review Submitted Applications | Peter Chen | `GET /api/demo/applications?recruiterId=` · `GET /api/applications/{id}/resume` (`X-Recruiter-Id`) |
| 4.2.2.4 Update Application Stage | Peter Chen | `PATCH /api/applications/{id}/stage` (JSON: `{"stage": ...}`; `X-Recruiter-Id`) |
| 4.2.2.5 Extend Job Offer | Spencer Hammack | `POST /api/applications/{id}/extend-offer` (`X-Recruiter-Id`) |
| 4.2.3.1 Manage Accounts and Roles | Luyu Niu | `GET /api/accounts/change` · `GET /api/accounts/lookup?email=` · `PUT /api/accounts/{id}/role-status` (JSON: newRole and/or newStatus, companyName for a new recruiter; `X-Administrator-Id`) |

Business-rule failures return HTTP 400 with `{"message": ...}`; unknown ids or emails return 404.
Class and method names follow the sequence diagrams and the design class diagram of the project
documentation.

## Plain HTML pages (Submit Application and My Applications)

Two pages served by the back end itself show the Submit Job Application flow without the
React front end. They do not sign in, so start the back end with the identity check off:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments=--rams.login.enforce=false
```

1. Open http://localhost:8080/submit-application.html, choose an applicant and an open posting,
   select or upload a PDF/DOCX resume, add a cover letter, review and confirm.
2. Open http://localhost:8080/applications.html to read the stored applications back.
3. Stop Spring Boot (Ctrl+C), start it again with the same command, and reload the page: the
   application is still there (file database).

The seed data already has OFFER-stage applications for two postings; use an open posting not yet
applied to (for example `Frontend Developer`). Applying twice to the same posting is refused.
Design notes: [`docs/design/SUBMIT_APPLICATION_INTEGRATION.md`](docs/design/SUBMIT_APPLICATION_INTEGRATION.md).

## Repository layout

```
recruiting-system/
├── backend/    Spring Boot application (Maven Wrapper, JUnit 5)
├── frontend/   React application (Vite, Vitest)
├── docs/       Course material and design notes
└── README.md   This file
```

## Working rules

- `main` always builds and passes the tests; changes arrive only through reviewed pull requests.
- One feature branch per use case or fix.
- Releases are tagged on `main` (v1.0 = Iteration 1, v2.0 to v2.5.2 = Iteration 2).
