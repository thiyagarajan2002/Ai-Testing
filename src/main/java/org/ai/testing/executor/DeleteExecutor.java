package org.ai.testing.executor;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.delete.DeleteRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;

public class DeleteExecutor extends AbstractHttpExecutor<DeleteRequestDto> {

    @Override
    protected String httpMethod() {
        return "DELETE";
    }

    @Override
    public ResponseDto execute(DeleteRequestDto request) {
        return super.execute(request);
    }
}
