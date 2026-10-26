
export async function submitApplication(
    applicantId: number,
    jobPostingId: number,
    resumeFile: File,
    coverLetter: string
) {
    // Step 1: Upload resume to backend
    const formData = new FormData();
    formData.append("file", resumeFile);

    const uploadResponse = await fetch(
        `/api/applicants/${applicantId}/resumes`,
        {
            method: "POST",
            body: formData,
        }
    );

    if (!uploadResponse.ok) {
        throw new Error(await uploadResponse.text());
    }

    const resume: { id: number; filename: string } =
        await uploadResponse.json();

    // Step 2: Submit application
    const response = await fetch("/api/applications", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            applicantId,
            jobPostingId,
            resumeId: resume.id,
            coverLetter,
        }),
    });

    if (!response.ok) {
        throw new Error(await response.text());
    }

    return response.json();
}
