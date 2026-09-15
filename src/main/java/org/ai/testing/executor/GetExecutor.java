package org.ai.testing.executor;

import org.ai.testing.dto.get.GetRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.executor.common.AbstractHttpExecutor;

public class GetExecutor extends AbstractHttpExecutor<GetRequestDto> {

    @Override
    protected String httpMethod() {
        return "GET";
    }

    @Override
    public ResponseDto execute(GetRequestDto request) {
        return super.execute(request);
    }
}
