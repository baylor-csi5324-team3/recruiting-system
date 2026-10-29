# Recruiting and Application Management System

CSI 5324 Software Engineering I, Fall 2026, Team 3 "Five Guys" (Baylor University).

## What it does

A web system for job postings and applications. Applicants search for jobs, apply, and follow
the stage of each application. Recruiters publish job postings and move applications through
the stages. Administrators manage accounts and roles.

## How to set it up and open it

You need Java 21 (JDK), Node.js 22.12 or newer, and Git. Maven is not needed; the project
includes the Maven Wrapper.

1. Unzip the package (or `git clone https://github.com/baylor-csi5324-team3/recruiting-system.git`)
   and open a terminal in the `recruiting-system` folder.
2. Start the back end (keep this terminal open):

   ```bash
   cd backend
   ./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
   ```

3. In a second terminal, start the front end:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Open http://localhost:5173 and click **Sign in**. All sample accounts use the password
   `Demo-pass-2026`:
   - applicant@example.com (applicant)
   - recruiter@example.com (recruiter)
   - admin@example.com (administrator)

Run the tests with `./mvnw test` in `backend` (134 tests) and `npm test` in `frontend`.
