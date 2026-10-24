package edu.baylor.csi5324.rams.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationStageHistoryTest {

    private static final LocalDateTime SUBMITTED_AT =
            LocalDateTime.of(2026, 10, 24, 9, 0);

    @Test
    void newApplicationStartsWithSubmittedHistory() {
        Application application =
                new Application(null, null, SUBMITTED_AT);

        List<StageRecord> history = application.getStageHistory();

        assertEquals(1, history.size());
        assertEquals(ApplicationStage.SUBMITTED, history.get(0).getStage());
        assertEquals(SUBMITTED_AT, history.get(0).getChangedAt());
        assertEquals(ApplicationStage.SUBMITTED, application.getStage());
    }

    @Test
    void validTransitionsPreserveHistoryOrderTimesAndUsers() {
        Application application =
                new Application(null, null, SUBMITTED_AT);

        LocalDateTime reviewedAt = SUBMITTED_AT.plusHours(1);
        LocalDateTime offeredAt = SUBMITTED_AT.plusHours(2);
        LocalDateTime acceptedAt = SUBMITTED_AT.plusHours(3);

        application.startReview(reviewedAt, 25L);
        application.makeOffer(offeredAt, 25L);
        application.updateStage(ApplicationStage.ACCEPTED, acceptedAt, 25L);

        List<StageRecord> history = application.getStageHistory();

        assertEquals(
                List.of(
                        ApplicationStage.SUBMITTED,
                        ApplicationStage.UNDER_REVIEW,
                        ApplicationStage.OFFER,
                        ApplicationStage.ACCEPTED
                ),
                history.stream().map(StageRecord::getStage).toList()
        );

        assertEquals(
                List.of(SUBMITTED_AT, reviewedAt, offeredAt, acceptedAt),
                history.stream().map(StageRecord::getChangedAt).toList()
        );

        assertEquals(Long.valueOf(25L), history.get(1).getChangedByUserId());
        assertEquals(Long.valueOf(25L), history.get(2).getChangedByUserId());
        assertEquals(ApplicationStage.ACCEPTED, application.getStage());
        assertEquals(acceptedAt, application.getStageUpdatedAt());
    }

    @Test
    void invalidTransitionsLeaveStageAndHistoryUnchanged() {
        Application application =
                new Application(null, null, SUBMITTED_AT);

        List<StageRecord> originalHistory = application.getStageHistory();

        // An application cannot skip directly from SUBMITTED to OFFER.
        assertThrows(
                IllegalStateException.class,
                () -> application.makeOffer(SUBMITTED_AT.plusHours(1), 25L)
        );

        assertEquals(ApplicationStage.SUBMITTED, application.getStage());
        assertEquals(originalHistory, application.getStageHistory());

        application.startReview(SUBMITTED_AT.plusHours(1), 25L);
        application.makeOffer(SUBMITTED_AT.plusHours(2), 25L);

        List<StageRecord> offeredHistory = application.getStageHistory();

        // An offer response can only be ACCEPTED or DECLINED.
        assertThrows(
                IllegalStateException.class,
                () -> application.updateStage(
                        ApplicationStage.INTERVIEW,
                        SUBMITTED_AT.plusHours(3)
                )
        );

        assertEquals(ApplicationStage.OFFER, application.getStage());
        assertEquals(offeredHistory, application.getStageHistory());
        assertNull(application.getStageUpdatedAt());
    }
}