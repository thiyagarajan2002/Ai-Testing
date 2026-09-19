package org.ai.testing.dto.patch;

import org.ai.testing.dto.common.BaseRequestDto;

/** Marker subtype so the Patch executor is selected in a type-safe way. */
public class PatchRequestDto extends BaseRequestDto {

    public static PatchRequestDto from(BaseRequestDto source) {
        PatchRequestDto request = new PatchRequestDto();
        if (source != null) {
            source.copyInto(request);
        }
        return request;
    }
}
