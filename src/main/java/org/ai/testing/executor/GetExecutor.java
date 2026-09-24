package org.ai.testing.executor;

import org.ai.testing.dto.get.GetRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;
import org.ai.testing.executor.common.ExecutionOptions;

/** Sends GET requests. */
public class GetExecutor extends AbstractHttpExecutor<GetRequestDto> {

    public GetExecutor() {
        super();
    }

    public GetExecutor(ExecutionOptions options) {
        super(options);
    }

    @Override
    protected String httpMethod() {
        return "GET";
    }
}
