package org.ai.testing.dto.delete;

import org.ai.testing.dto.common.BaseRequestDto;

/** Marker subtype so the Delete executor is selected in a type-safe way. */
public class DeleteRequestDto extends BaseRequestDto {

    public static DeleteRequestDto from(BaseRequestDto source) {
        DeleteRequestDto request = new DeleteRequestDto();
        if (source != null) {
            source.copyInto(request);
        }
        return request;
    }
}
