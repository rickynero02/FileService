package com.fileservice.files;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class ResponseProvider implements AsyncResponseTransformer<GetObjectResponse,
        ResponseProvider.FluxResponse> {

    private FluxResponse response;

    @Override
    public CompletableFuture<FluxResponse> prepare() {
        response = new FluxResponse();
        return response.cf;
    }

    @Override
    public void onResponse(GetObjectResponse getObjectResponse) {
        this.response.sdkResponse = getObjectResponse;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> sdkPublisher) {
        response.flux = Flux.from(sdkPublisher);
        response.cf.complete(response);
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        response.cf.completeExceptionally(throwable);
    }

     static class FluxResponse {
        final CompletableFuture<FluxResponse> cf = new CompletableFuture<>();
        GetObjectResponse sdkResponse;
        Flux<ByteBuffer> flux;
    }
}