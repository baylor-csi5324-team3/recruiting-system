package edu.baylor.csi5324.rams.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationWithdrawalTest {

    private static final LocalDateTime SUBMITTED_AT =
            LocalDateTime.of(2026, 10, 24, 9, 0);

    private static final LocalDateTime WITHDRAWN_AT =
            SUBMITTED_AT.plusHours(3);

    private Application applicationAt(ApplicationStage target) {
        Application application =
                new Application(null, null, SUBMITTED_AT);

        switch (target) {
            case SUBMITTED -> {
                // Already submitted.
            }
            case UNDER_REVIEW -> application.startReview(
                    SUBMITTED_AT.plusHours(1), 25L
            );
            case INTERVIEW -> {
                application.startReview(SUBMITTED_AT.plusHours(1), 25L);
                application.updateStage(
                        ApplicationStage.INTERVIEW,
                        SUBMITTED_AT.plusHours(2),
                        25L
                );
            }
            case OFFER, ACCEPTED, DECLINED -> {
                application.startReview(SUBMITTED_AT.plusHours(1), 25L);
                application.makeOffer(SUBMITTED_AT.plusHours(2), 25L);

                if (target != ApplicationStage.OFFER) {
                    application.updateStage(
                            target,
                            SUBMITTED_AT.plusMinutes(150),
                            25L
                    );
                }
            }
            case REJECTED -> application.updateStage(
                    ApplicationStage.REJECTED,
                    SUBMITTED_AT.plusHours(1),
                    25L
            );
            case WITHDRAWN -> application.withdraw(
                    SUBMITTED_AT.plusHours(1), 10L
            );
        }

        return application;
    }

    @Test
    void withdrawsFromEachEligibleStageAndPreservesHistory() {
        for (ApplicationStage initial : List.of(
                ApplicationStage.SUBMITTED,
                ApplicationStage.UNDER_REVIEW,
                ApplicationStage.INTERVIEW)) {

            Application application = applicationAt(initial);
            List<StageRecord> previous = application.getStageHistory();

            assertTrue(application.checkWithdrawable(), initial.name());

            application.withdraw(WITHDRAWN_AT, 10L);

            List<StageRecord> history = application.getStageHistory();

            assertEquals(ApplicationStage.WITHDRAWN, application.getStage());
            assertEquals(WITHDRAWN_AT, application.getStageUpdatedAt());
            assertEquals(previous.size() + 1, history.size());
            assertEquals(previous, history.subList(0, previous.size()));

            StageRecord latest = history.get(history.size() - 1);

            assertEquals(ApplicationStage.WITHDRAWN, latest.getStage());
            assertEquals(WITHDRAWN_AT, latest.getChangedAt());
            assertEquals(Long.valueOf(10L), latest.getChangedByUserId());
            assertFalse(application.checkWithdrawable());
        }
    }

    @Test
    void rejectsWithdrawalFromOfferAndFinalStagesWithoutChanges() {
        for (ApplicationStage initial : List.of(
                ApplicationStage.OFFER,
                ApplicationStage.ACCEPTED,
                ApplicationStage.DECLINED,
                ApplicationStage.REJECTED,
                ApplicationStage.WITHDRAWN)) {

            Application application = applicationAt(initial);
            List<StageRecord> previous = application.getStageHistory();
            LocalDateTime previousUpdate = application.getStageUpdatedAt();

            assertFalse(application.checkWithdrawable(), initial.name());

            assertThrows(
                    IllegalStateException.class,
                    () -> application.withdraw(WITHDRAWN_AT, 10L),
                    initial.name()
            );

            assertEquals(initial, application.getStage());
            assertEquals(previous, application.getStageHistory());
            assertEquals(previousUpdate, application.getStageUpdatedAt());
        }
    }

    @Test
    void rejectsMissingWithdrawalTimeWithoutChanges() {
        Application application =
                applicationAt(ApplicationStage.SUBMITTED);

        List<StageRecord> previous = application.getStageHistory();

        assertThrows(
                IllegalArgumentException.class,
                () -> application.withdraw(null, 10L)
        );

        assertEquals(ApplicationStage.SUBMITTED, application.getStage());
        assertEquals(previous, application.getStageHistory());
        assertNull(application.getStageUpdatedAt());
    }

    @Test
    void withdrawnApplicationCannotContinueRecruiting() {
        Application application =
                applicationAt(ApplicationStage.WITHDRAWN);

        List<StageRecord> previous = application.getStageHistory();

        for (ApplicationStage next : ApplicationStage.values()) {
            assertThrows(
                    IllegalStateException.class,
                    () -> application.updateStage(
                            next, WITHDRAWN_AT.plusHours(1), 25L
                    ),
                    next.name()
            );
        }

        assertEquals(ApplicationStage.WITHDRAWN, application.getStage());
        assertEquals(previous, application.getStageHistory());
    }

    @Test
    void genericStageUpdateAlsoRejectsWithdrawalFromOffer() {
        Application application =
                applicationAt(ApplicationStage.OFFER);

        List<StageRecord> previous = application.getStageHistory();

        assertThrows(
                IllegalStateException.class,
                () -> application.updateStage(
                        ApplicationStage.WITHDRAWN,
                        WITHDRAWN_AT,
                        10L
                )
        );

        assertEquals(ApplicationStage.OFFER, application.getStage());
        assertEquals(previous, application.getStageHistory());
    }
}