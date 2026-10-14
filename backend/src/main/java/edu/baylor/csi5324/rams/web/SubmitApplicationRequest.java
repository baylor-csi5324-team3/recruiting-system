package edu.baylor.csi5324.rams.web;

import jakarta.validation.constraints.NotNull;

/** Confirmed materials for the Submit Job Application use case. */
public record SubmitApplicationRequest(
        @NotNull Long applicantId,
        @NotNull Long jobPostingId,
        @NotNull Long resumeId,
        String coverLetter) {}
