package org.ai.testing.dto.post;

import org.ai.testing.dto.common.BaseRequestDto;

/** Marker subtype so the Post executor is selected in a type-safe way. */
public class PostRequestDto extends BaseRequestDto {

    public static PostRequestDto from(BaseRequestDto source) {
        PostRequestDto request = new PostRequestDto();
        if (source != null) {
            source.copyInto(request);
        }
        return request;
    }
}
