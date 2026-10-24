package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class StageRecord {

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_name", nullable = false)
    private ApplicationStage stage;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    private Long changedByUserId;

    protected StageRecord() {
    }

    public StageRecord(
            ApplicationStage stage,
            LocalDateTime changedAt,
            Long changedByUserId) {

        this.stage = Objects.requireNonNull(stage);
        this.changedAt = Objects.requireNonNull(changedAt);
        this.changedByUserId = changedByUserId;
    }

    public ApplicationStage getStage() {
        return stage;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }
}