package org.ai.testing.dto.put;

import org.ai.testing.dto.common.BaseRequestDto;

/** Marker subtype so the Put executor is selected in a type-safe way. */
public class PutRequestDto extends BaseRequestDto {

    public static PutRequestDto from(BaseRequestDto source) {
        PutRequestDto request = new PutRequestDto();
        if (source != null) {
            source.copyInto(request);
        }
        return request;
    }
}
