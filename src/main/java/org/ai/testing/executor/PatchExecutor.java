package org.ai.testing.executor;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.patch.PatchRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;

public class PatchExecutor extends AbstractHttpExecutor<PatchRequestDto> {

    @Override
    protected String httpMethod() {
        return "PATCH";
    }

    @Override
    public ResponseDto execute(PatchRequestDto request) {
        return super.execute(request);
    }
}
