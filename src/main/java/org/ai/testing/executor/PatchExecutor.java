package org.ai.testing.executor;

import org.ai.testing.dto.patch.PatchRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;
import org.ai.testing.executor.common.ExecutionOptions;

/** Sends PATCH requests. */
public class PatchExecutor extends AbstractHttpExecutor<PatchRequestDto> {

    public PatchExecutor() {
        super();
    }

    public PatchExecutor(ExecutionOptions options) {
        super(options);
    }

    @Override
    protected String httpMethod() {
        return "PATCH";
    }
}
