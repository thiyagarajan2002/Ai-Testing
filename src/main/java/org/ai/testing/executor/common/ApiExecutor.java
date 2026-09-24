package org.ai.testing.executor.common;

import org.ai.testing.dto.common.ResponseDto;

/** Sends one request and returns what came back. */
public interface ApiExecutor<T> {

    ResponseDto execute(T request);

    /** The HTTP verb this executor is responsible for. */
    String method();
}
