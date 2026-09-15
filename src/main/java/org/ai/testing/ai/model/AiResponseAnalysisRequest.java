package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.dto.common.ResponseDto;

@Data
public class AiResponseAnalysisRequest {

    private ResponseDto response;

    private Integer expectedStatusCode;

    private long slowResponseThresholdMs = 2000;
}
