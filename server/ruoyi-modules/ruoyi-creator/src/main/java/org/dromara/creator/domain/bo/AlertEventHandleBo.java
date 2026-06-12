package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Alert event handling request.
 */
@Data
public class AlertEventHandleBo {

    @NotBlank(message = "status is required")
    private String status;

    private String handleNote;
}
