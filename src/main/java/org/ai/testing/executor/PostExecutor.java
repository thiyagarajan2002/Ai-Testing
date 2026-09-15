package org.ai.testing.executor;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.post.PostRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;

public class PostExecutor extends AbstractHttpExecutor<PostRequestDto> {

    @Override
    protected String httpMethod() {
        return "POST";
    }

    @Override
    public ResponseDto execute(PostRequestDto request) {
        return super.execute(request);
    }
}
