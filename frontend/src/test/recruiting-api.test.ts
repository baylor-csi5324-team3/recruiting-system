
import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from "vitest";

import {
    loadApplicantApplications,
    loadRecruiterApplications,
    convertApplication,
    toBackendStage,
    type Actor,
} from "@/lib/recruiting-api";

describe("KAN-80: My Applications real backend", () => {
    const applicant: Actor = {
        id: 1,
        name: "Test Applicant",
        email: "applicant@example.com",
        role: "Applicant",
        company: null,
    };

    const submission = {
        applicationId: 10,
        applicantId: 1,
        jobPostingId: 5,
        jobTitle: "Software Engineer",
        companyName: "Example Company",
        stage: "SUBMITTED",
        submittedAt: "2026-11-18T10:00:00",
        resumeFilename: "resume.pdf",
    };

    const tracking = {
        applicationId: 10,
        jobTitle: "Software Engineer",
        companyName: "Example Company",
        stage: "SUBMITTED",
        stageUpdatedAt: "2026-11-18T10:00:00",
        stageHistory: [
            {
                stage: "SUBMITTED",
                changedAt: "2026-11-18T10:00:00",
                changedByUserId: 1,
            },
        ],
    };

    // Mock a backend HTTP response
    const respond = (data: unknown, status = 200): Response =>
        ({
            ok: status >= 200 && status < 300,
            status,
            json: async () => data,
        }) as Response;

    beforeEach(() => {
        vi.stubGlobal("fetch", vi.fn());
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // TEST 1: Successfully retrieve applications
    it("loads applications from real endpoints instead of demo", async () => {
        const mockFetch = vi.mocked(fetch);

        mockFetch
            .mockResolvedValueOnce(respond([submission]))
            .mockResolvedValueOnce(respond([tracking]));

        const apps = await loadApplicantApplications(applicant);

        expect(apps).toHaveLength(1);
        expect(apps[0].id).toBe(10);
        expect(apps[0].jobId).toBe(5);
        expect(apps[0].stage).toBe("SUBMITTED");
        expect(apps[0].resumeFilename).toBe("resume.pdf");
        expect(apps[0].history).toHaveLength(1);

        // Verify real application endpoint
        expect(mockFetch).toHaveBeenCalledWith(
            "/api/applications?applicantId=1",
            undefined
        );

        // Verify real tracking endpoint
        expect(mockFetch).toHaveBeenCalledWith(
            "/api/applications/tracking",
            {
                headers: {
                    "X-Applicant-Id": "1",
                },
            }
        );

        // Verify demo endpoint is NOT used
        expect(
            mockFetch.mock.calls.some(([url]) =>
                String(url).includes("/api/demo/applications")
            )
        ).toBe(false);
    });

    // TEST 2: Applicant sees updated stage after recruiter changes it
    it("shows updated stage after recruiter changes application status", async () => {
        const updatedTracking = {
            ...tracking,
            stage: "UNDER_REVIEW",
            stageUpdatedAt: "2026-11-18T11:00:00",
            stageHistory: [
                ...tracking.stageHistory,
                {
                    stage: "UNDER_REVIEW",
                    changedAt: "2026-11-18T11:00:00",
                    changedByUserId: 2,
                },
            ],
        };

        const mockFetch = vi.mocked(fetch);

        // First request: stage is SUBMITTED
        mockFetch
            .mockResolvedValueOnce(respond([submission]))
            .mockResolvedValueOnce(respond([tracking]))

            // Second request: recruiter has changed stage
            .mockResolvedValueOnce(respond([submission]))
            .mockResolvedValueOnce(respond([updatedTracking]));

        const before = await loadApplicantApplications(applicant);

        expect(before).toHaveLength(1);
        expect(before[0].stage).toBe("SUBMITTED");

        // Applicant refreshes and retrieves updated data
        const after = await loadApplicantApplications(applicant);

        expect(after).toHaveLength(1);
        expect(after[0].stage).toBe("UNDER_REVIEW");

        // Verify stage history also updates
        expect(after[0].history).toHaveLength(2);
        expect(after[0].history[1].stage).toBe("UNDER_REVIEW");
        expect(after[0].history[1].changedByUserId).toBe(2);
    });

    // TEST 3: Applicant has no applications
    it("returns empty list when applicant has no applications", async () => {
        const mockFetch = vi.mocked(fetch);

        mockFetch
            .mockResolvedValueOnce(respond([]))
            .mockResolvedValueOnce(respond([]));

        const apps = await loadApplicantApplications(applicant);

        expect(apps).toEqual([]);
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    // TEST 4: Backend returns an error
    it("handles backend failure without returning fake data", async () => {
        const mockFetch = vi.mocked(fetch);

        mockFetch
            .mockResolvedValueOnce(
                respond({ message: "Forbidden" }, 403)
            )
            .mockResolvedValueOnce(respond([]));

        await expect(
            loadApplicantApplications(applicant)
        ).rejects.toThrow("Forbidden");

        // The application should not silently use demo data
        expect(
            mockFetch.mock.calls.some(([url]) =>
                String(url).includes("/api/demo/applications")
            )
        ).toBe(false);
    });
});


describe("KAN-78 and KAN-79: candidate pipeline", () => {
    afterEach(() => vi.unstubAllGlobals());
    it("loads candidates through the real recruiter endpoint and converts shortlisted history", async () => {
        const row = { id: 10, jobId: 5, name: "Candidate", email: "a@example.com", stage: "SHORTLISTED", submittedAt: "2026-11-18T10:00:00", resumeFilename: "resume.pdf", history: [{ stage: "SHORTLISTED", changedAt: "2026-11-18T11:00:00", changedByUserId: 2 }] };
        const mockFetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => [row] });
        vi.stubGlobal("fetch", mockFetch);
        const rows = await loadRecruiterApplications({ id: 2, name: "Recruiter", email: "r@example.com", role: "Recruiter", company: null });
        expect(mockFetch).toHaveBeenCalledExactlyOnceWith("/api/applications/candidates", { headers: { "X-Recruiter-Id": "2" } });
        expect(convertApplication(rows[0]).stage).toBe("Shortlisted");
        expect(convertApplication(rows[0]).history[0].stage).toBe("Shortlisted");
        expect(toBackendStage.Shortlisted).toBe("SHORTLISTED");
    });
});
