package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationStage;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStageRequest(
        @NotNull ApplicationStage stage
) {
}