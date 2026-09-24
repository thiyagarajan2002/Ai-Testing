package org.ai.testing.executor;

import org.ai.testing.dto.delete.DeleteRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;
import org.ai.testing.executor.common.ExecutionOptions;

/** Sends DELETE requests. */
public class DeleteExecutor extends AbstractHttpExecutor<DeleteRequestDto> {

    public DeleteExecutor() {
        super();
    }

    public DeleteExecutor(ExecutionOptions options) {
        super(options);
    }

    @Override
    protected String httpMethod() {
        return "DELETE";
    }
}
