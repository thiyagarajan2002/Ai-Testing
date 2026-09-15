package org.ai.testing.executor;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.put.PutRequestDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;

public class PutExecutor extends AbstractHttpExecutor<PutRequestDto> {

    @Override
    protected String httpMethod() {
        return "PUT";
    }

    @Override
    public ResponseDto execute(PutRequestDto request) {
        return super.execute(request);
    }
}
