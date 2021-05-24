package com.fileservice.files;

import com.fileservice.config.S3ClientConfigProperties;
import com.fileservice.exceptions.DownloadFailedException;
import com.fileservice.exceptions.UploadFailedException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/v1/files")
@AllArgsConstructor
public class FileController {

    private final FileService service;
    private final S3AsyncClient asyncClient;
    private final S3ClientConfigProperties properties;

    @GetMapping(path = "/getAll/{username}")
    public Flux<File> getAllFiles(@PathVariable("username") String username) {
        return service.fetchAllFiles(username);
    }

    @PostMapping(path = "/upload")
    public Mono<ResponseEntity<UploadResult>> uploadFile(@RequestHeader HttpHeaders headers,
         @RequestBody Flux<ByteBuffer> body) {

        long length = headers.getContentLength();
        if (length < 0) {
            throw new UploadFailedException(HttpStatus.BAD_REQUEST.value(),
                    Optional.of("required header missing: Content-Length"));
        }

        MediaType mediaType = headers.getContentType();
        if(mediaType == null) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String fileKey = UUID.randomUUID().toString();
        Map<String, String> metadata = new HashMap<>();

        CompletableFuture<PutObjectResponse> future = asyncClient
                .putObject(PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .contentLength(length)
                    .key(fileKey)
                    .contentType(mediaType.toString())
                    .metadata(metadata)
                    .build(),
                    AsyncRequestBody.fromPublisher(body));


        return Mono.fromFuture(future)
                .map(response -> {
                    checkUploadResult(response);
                    return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(new UploadResult(HttpStatus.CREATED, new String[]{fileKey}));
                });
    }

    @GetMapping(path="/download/{filekey}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable("filekey") String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(fileKey)
                .build();

        return Mono.fromFuture(asyncClient.getObject(request, new ResponseProvider()))
                .map(response -> {
                    checkDownloadResult(response.sdkResponse);
                    String fileName = getMetadataItem(response.sdkResponse, fileKey);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, response.sdkResponse.contentType())
                            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(response.sdkResponse.contentLength()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fileName))
                            .body(response.flux);
                });
    }

    private String getMetadataItem(GetObjectResponse sdkResponse, String defaultValue) {
        for( Map.Entry<String, String> entry : sdkResponse.metadata().entrySet()) {
            if ( entry.getKey().equalsIgnoreCase("filename")) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    private static void checkUploadResult(SdkResponse result) {
        if(result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            throw new UploadFailedException(result);
        }
    }

    private static void checkDownloadResult(SdkResponse response) {
        SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if ( sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        throw new DownloadFailedException(response);
    }
}
