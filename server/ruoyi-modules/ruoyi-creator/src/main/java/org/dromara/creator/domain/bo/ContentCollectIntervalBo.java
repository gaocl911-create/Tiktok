package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for updating a monitor target content collection interval.
 */
@Data
public class ContentCollectIntervalBo {

    @NotNull(message = "contentCollectIntervalMin is required")
    @Min(value = 15, message = "contentCollectIntervalMin must be at least 15 minutes")
    @Max(value = 1440, message = "contentCollectIntervalMin must be at most 1440 minutes")
    private Integer contentCollectIntervalMin;
}
