# Submit Job Application — Integration Notes

This integration extends the **team's existing** `edu.baylor.csi5324.rams` package, rather than introducing a competing application model. Existing Job Search, Job Posting, Account, and Offer Response services remain in place.

## Flow

1. Open `/submit-application.html` on port 8080; choose a seeded applicant and an open job.
2. Upload or select a PDF/DOCX resume; optionally write a cover letter.
3. Review, then confirm. `POST /api/applications` validates the applicant, resume ownership, job status/deadline, and duplicate rule.
4. The service saves a **new `Application`** with stage `SUBMITTED` and a snapshot of the resume and cover letter.
5. Open `/applications.html`. It calls `GET /api/applications?applicantId=<id>` and re-reads the same `job_applications` records from the database.

The existing Offer Response implementation continues to operate on the same `Application` table. Older seeded OFFER applications may have no resume snapshot, so the UI displays `(legacy record)` for them.

## Run

From `backend/`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

The `demo` profile uses a **file-backed** H2 database (`backend/data/rams.mv.db`) so submissions can survive a server restart. The default profile remains the team's existing **in-memory** H2 database. Keep the same working directory when restarting. The dev seeder is idempotent if accounts already exist.

- Submit UI: http://localhost:8080/submit-application.html
- History UI: http://localhost:8080/applications.html
- GET API: http://localhost:8080/api/applications?applicantId=1
- H2 console: http://localhost:8080/h2-console (demo JDBC URL `jdbc:h2:file:./data/rams`, user `sa`, blank password)

The seeded applicant already has two OFFER applications for the first two jobs; to demonstrate a **new** successful submission, choose the third open posting (`Frontend Developer`) and upload a resume. Submitting the same job again should be rejected.

## Important integration limits

- The applicant ID is supplied by the demo UI; **this is not authentication or authorization**. Do not deploy publicly until endpoints use verified logged-in identity.
- No recruiter-facing review screen was added. Existing offer response routes remain unchanged.
- The sample CSV (`backend/src/main/resources/jobs-sample.csv`) is a **reference test fixture**, not an automatic import. The team's `DevDataSeeder` continues to create sample jobs.
- Resume snapshots are stored as bytes on `Application`; existing legacy applications remain valid with null snapshot fields.
- The team's frontend folder remains unchanged and empty. The two demo pages are served by Spring Boot from `backend/src/main/resources/static/` to avoid choosing a React implementation for the team.
