# Submit Job Application — Integration Notes

This integration extends the **team's existing** `edu.baylor.csi5324.rams` package, rather than introducing a competing application model. Existing Job Search, Job Posting, Account, and Offer Response services remain in place.

## Flow

1. In the React front end (http://localhost:5173), sign in as an applicant and open an open job.
2. Upload or select a PDF/DOCX resume; optionally write a cover letter.
3. Review, then confirm. `POST /api/applications` validates the applicant, resume ownership, job status/deadline, and duplicate rule.
4. The service saves a **new `Application`** with stage `SUBMITTED` and a snapshot of the resume and cover letter.
5. Open My applications. It calls `GET /api/applications?applicantId=<id>` and re-reads the same `job_applications` records from the database.

The existing Offer Response implementation continues to operate on the same `Application` table. Older seeded OFFER applications may have no resume snapshot, so the UI displays `(legacy record)` for them.

## Run

See the README: create `backend/.env` with the team PostgreSQL values, run `./mvnw spring-boot:run` in `backend/` and `npm run dev` in `frontend/`. Data stays in PostgreSQL after a restart.

The seeded applicant already has two OFFER applications for the first two jobs; to demonstrate a **new** successful submission, choose the third open posting (`Frontend Developer`) and upload a resume. Submitting the same job again should be rejected.

## Important integration limits

- The applicant ID is supplied by the demo UI; **this is not authentication or authorization**. Do not deploy publicly until endpoints use verified logged-in identity.
- No recruiter-facing review screen was added. Existing offer response routes remain unchanged.
- The sample CSV (`backend/src/main/resources/jobs-sample.csv`) is a **reference test fixture**, not an automatic import. The team's `DevDataSeeder` continues to create sample jobs.
- Resume snapshots are stored as bytes on `Application`; existing legacy applications remain valid with null snapshot fields.
- The team's frontend folder remains unchanged and empty. The two demo pages are served by Spring Boot from `backend/src/main/resources/static/` to avoid choosing a React implementation for the team.
