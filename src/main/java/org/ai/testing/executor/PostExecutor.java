package org.ai.testing.executor;

import org.ai.testing.dto.post.PostRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;
import org.ai.testing.executor.common.ExecutionOptions;

/** Sends POST requests. */
public class PostExecutor extends AbstractHttpExecutor<PostRequestDto> {

    public PostExecutor() {
        super();
    }

    public PostExecutor(ExecutionOptions options) {
        super(options);
    }

    @Override
    protected String httpMethod() {
        return "POST";
    }
}
