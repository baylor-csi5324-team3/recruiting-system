package edu.baylor.csi5324.rams.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    private static final LocalDateTime SUBMIT_DATE =
            LocalDateTime.of(2026, 10, 12, 9, 0);

    private static final LocalDateTime RESPONSE_TIME =
            LocalDateTime.of(2026, 10, 12, 10, 0);

    // 这里只测试状态转换，不保存数据库，也不访问关联对象。
    private Application offeredApplication() {
        Application application =
                new Application(null, null, SUBMIT_DATE);

        application.startReview();
        application.makeOffer();

        return application;
    }

    @Test
    void acceptsOfferAndRecordsResponseTime() {
        Application application = offeredApplication();

        application.recordStage(
                ApplicationStage.ACCEPTED, RESPONSE_TIME);

        assertEquals(ApplicationStage.ACCEPTED, application.getStage());
        assertEquals(RESPONSE_TIME, application.getRespondedAt());
        assertEquals(SUBMIT_DATE, application.getSubmitDate());
    }

    @Test
    void declinesOfferAndRecordsResponseTime() {
        Application application = offeredApplication();

        application.recordStage(
                ApplicationStage.DECLINED, RESPONSE_TIME);

        assertEquals(ApplicationStage.DECLINED, application.getStage());
        assertEquals(RESPONSE_TIME, application.getRespondedAt());
    }

    @Test
    void rejectsResponseWhenApplicationIsNotOffered() {
        Application application =
                new Application(null, null, SUBMIT_DATE);

        assertThrows(IllegalStateException.class,
                () -> application.recordStage(
                        ApplicationStage.ACCEPTED, RESPONSE_TIME));

        assertEquals(ApplicationStage.SUBMITTED, application.getStage());
        assertNull(application.getRespondedAt());
    }

    @Test
    void rejectsInvalidResponseStage() {
        Application application = offeredApplication();

        assertThrows(IllegalArgumentException.class,
                () -> application.recordStage(
                        ApplicationStage.INTERVIEW, RESPONSE_TIME));

        assertEquals(ApplicationStage.OFFER, application.getStage());
        assertNull(application.getRespondedAt());
    }

    @Test
    void rejectsMissingResponseTime() {
        Application application = offeredApplication();

        assertThrows(IllegalArgumentException.class,
                () -> application.recordStage(
                        ApplicationStage.ACCEPTED, null));

        assertEquals(ApplicationStage.OFFER, application.getStage());
        assertNull(application.getRespondedAt());
    }

    @Test
    void preventsChangingAnAlreadyAcceptedOffer() {
        Application application = offeredApplication();

        application.recordStage(
                ApplicationStage.ACCEPTED, RESPONSE_TIME);

        assertThrows(IllegalStateException.class,
                () -> application.recordStage(
                        ApplicationStage.DECLINED,
                        RESPONSE_TIME.plusMinutes(5)));

        assertEquals(ApplicationStage.ACCEPTED, application.getStage());
        assertEquals(RESPONSE_TIME, application.getRespondedAt());
    }
}