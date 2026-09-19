package org.ai.testing.executor;

import org.ai.testing.dto.put.PutRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;
import org.ai.testing.executor.common.ExecutionOptions;

/** Sends PUT requests. */
public class PutExecutor extends AbstractHttpExecutor<PutRequestDto> {

    public PutExecutor() {
        super();
    }

    public PutExecutor(ExecutionOptions options) {
        super(options);
    }

    @Override
    protected String httpMethod() {
        return "PUT";
    }
}
