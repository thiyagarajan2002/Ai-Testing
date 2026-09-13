package org.ai.testing.executor.common;

import org.ai.testing.dto.common.ResponseDto;

public interface ApiExecutor<T> {

    ResponseDto execute(T request);
}