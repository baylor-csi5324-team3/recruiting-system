
package edu.baylor.csi5324.rams.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    private static final LocalDateTime SUBMIT_DATE =
            LocalDateTime.of(2026, 10, 12, 9, 0);

    private static final LocalDateTime UPDATE_TIME =
            LocalDateTime.of(2026, 10, 12, 10, 0);

    // Create a new application without database dependencies.
    private Application newApplication() {
        return new Application(null, null, SUBMIT_DATE);
    }

    // Create an application that has reached the OFFER stage.
    private Application offeredApplication() {
        Application application = newApplication();

        application.updateStage(
                ApplicationStage.UNDER_REVIEW,
                SUBMIT_DATE.plusMinutes(10));

        application.updateStage(
                ApplicationStage.OFFER,
                SUBMIT_DATE.plusMinutes(30));

        return application;
    }

    /**
     * Test 1:
     * Successfully update from SUBMITTED to UNDER_REVIEW.
     */
    @Test
    void updatesSubmittedApplicationToUnderReview() {

        Application application = newApplication();

        application.updateStage(
                ApplicationStage.UNDER_REVIEW,
                UPDATE_TIME);

        assertEquals(
                ApplicationStage.UNDER_REVIEW,
                application.getStage());

        assertEquals(
                UPDATE_TIME,
                application.getStageUpdatedAt());

        assertEquals(
                SUBMIT_DATE,
                application.getSubmitDate());
    }

    /**
     * Test 2:
     * Successfully progress through multiple recruitment stages.
     */
    @Test
    void progressesThroughInterviewAndOffer() {

        Application application = newApplication();

        application.updateStage(
                ApplicationStage.UNDER_REVIEW,
                SUBMIT_DATE.plusMinutes(10));

        application.updateStage(
                ApplicationStage.INTERVIEW,
                SUBMIT_DATE.plusMinutes(20));

        application.updateStage(
                ApplicationStage.OFFER,
                SUBMIT_DATE.plusMinutes(30));

        assertEquals(
                ApplicationStage.OFFER,
                application.getStage());

        assertEquals(
                SUBMIT_DATE.plusMinutes(30),
                application.getStageUpdatedAt());
    }

    /**
     * Test 3:
     * Recruiter records an accepted or declined offer
     * after receiving the applicant's decision offline.
     */
    @Test
    void recordsAcceptedAndDeclinedOffer() {

        Application acceptedApplication = offeredApplication();

        acceptedApplication.updateStage(
                ApplicationStage.ACCEPTED,
                UPDATE_TIME);

        assertEquals(
                ApplicationStage.ACCEPTED,
                acceptedApplication.getStage());

        assertEquals(
                UPDATE_TIME,
                acceptedApplication.getStageUpdatedAt());

        Application declinedApplication = offeredApplication();

        declinedApplication.updateStage(
                ApplicationStage.DECLINED,
                UPDATE_TIME);

        assertEquals(
                ApplicationStage.DECLINED,
                declinedApplication.getStage());

        assertEquals(
                UPDATE_TIME,
                declinedApplication.getStageUpdatedAt());
    }

    /**
     * Test 4:
     * Reject an invalid stage transition.
     */
    @Test
    void rejectsInvalidStageTransition() {

        Application application = newApplication();

        assertThrows(
                IllegalStateException.class,
                () -> application.updateStage(
                        ApplicationStage.ACCEPTED,
                        UPDATE_TIME));

        assertEquals(
                ApplicationStage.SUBMITTED,
                application.getStage());

        assertNull(application.getStageUpdatedAt());
    }

    /**
     * Test 5:
     * Reject null stage or missing update time.
     */
    @Test
    void rejectsMissingStageOrUpdateTime() {

        Application application = offeredApplication();

        LocalDateTime previousUpdateTime =
                application.getStageUpdatedAt();

        assertThrows(
                IllegalArgumentException.class,
                () -> application.updateStage(
                        null,
                        UPDATE_TIME));

        assertThrows(
                IllegalArgumentException.class,
                () -> application.updateStage(
                        ApplicationStage.ACCEPTED,
                        null));

        assertEquals(
                ApplicationStage.OFFER,
                application.getStage());

        assertEquals(
                previousUpdateTime,
                application.getStageUpdatedAt());
    }

    /**
     * Test 6:
     * Terminal stages cannot be changed again.
     */
    @Test
    void preventsChangingTerminalStage() {

        Application application = offeredApplication();

        application.updateStage(
                ApplicationStage.ACCEPTED,
                UPDATE_TIME);

        assertThrows(
                IllegalStateException.class,
                () -> application.updateStage(
                        ApplicationStage.DECLINED,
                        UPDATE_TIME.plusMinutes(5)));

        assertEquals(
                ApplicationStage.ACCEPTED,
                application.getStage());

        assertEquals(
                UPDATE_TIME,
                application.getStageUpdatedAt());
    }
}
