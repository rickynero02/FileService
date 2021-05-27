package com.fileservice.exceptions;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.Optional;

@AllArgsConstructor
@SuppressWarnings("unused")
public class DeleteFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final Optional<String> statusText;

    public DeleteFailedException(SdkResponse response) {
        SdkHttpResponse httpResponse = response.sdkHttpResponse();
        if (httpResponse != null) {
            this.statusCode = httpResponse.statusCode();
            this.statusText = httpResponse.statusText();
        } else {
            this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            this.statusText = Optional.of("UNKNOWN");
        }
    }
}
