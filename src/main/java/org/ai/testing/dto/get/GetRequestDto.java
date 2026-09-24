package org.ai.testing.dto.get;

import org.ai.testing.dto.common.BaseRequestDto;

/** Marker subtype so the Get executor is selected in a type-safe way. */
public class GetRequestDto extends BaseRequestDto {

    public static GetRequestDto from(BaseRequestDto source) {
        GetRequestDto request = new GetRequestDto();
        if (source != null) {
            source.copyInto(request);
        }
        return request;
    }
}
