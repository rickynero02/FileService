package com.fileservice.files;

import com.fileservice.categories.CategoryRepository;
import com.fileservice.config.S3ClientConfigProperties;
import com.fileservice.exceptions.DeleteFailedException;
import com.fileservice.exceptions.DownloadFailedException;
import com.fileservice.exceptions.UploadFailedException;
import com.fileservice.utility.UserRoles;
import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FileService {

    private final FileRepository repository;
    private final S3AsyncClient asyncClient;
    private final S3ClientConfigProperties properties;

    private static final int MAX_FILE = 10;

    public Flux<File> fetchAllFiles(String owner) {
        return repository.findAllByOwner(owner);
    }

    public Mono<File> fetchByNameAndOwner(String username, String name, String passwd) {
        return repository.findByNameAndOwner(name, username)
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")))
                .filter(file -> {
                    if(file.getPassword() == null)
                        return true;
                    return file.getPassword().equals(passwd);
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Incorrect password")));
    }

    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(File f) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(f.getId())
                .build();

        return repository.findById(f.getId())
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")))
                .filter(file -> isAccessible(file, f.getName(), f.getPassword()))
                .switchIfEmpty(Mono.error(new IllegalStateException("This file is protected")))
                .flatMap(file -> Mono.fromFuture(asyncClient
                        .getObject(request, new ResponseProvider())).map(response -> {
                    checkResult(response.sdkResponse,
                            new DownloadFailedException(response.sdkResponse));
                    String fileName = getMetadataItem(response.sdkResponse, file.getId());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE,
                                    response.sdkResponse.contentType())
                            .header(HttpHeaders.CONTENT_LENGTH,
                                    Long.toString(response.sdkResponse.contentLength()))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    String.format("attachment; filename=\"%s\"", fileName))
                            .body(response.flux);
                }));
    }

    public Mono<List> uploadFile(HttpHeaders headers,
                 Flux<FilePart> fileParts, UploadRequest request) {

        File file = new File(UUID.randomUUID().toString(), request.getUsername(),
                request.getFilename(), null,  headers.getContentLength(),
                LocalDateTime.now(), true, request.getDescription(),
                new LinkedList<>(), new LinkedList<>());

        return repository.findAllByOwner(request.getUsername()).collect(Collectors.toList())
                .map(List::size)
                .filter(c -> c >= MAX_FILE && request.getRole().equals(UserRoles.STANDARD))
                .flatMap(f -> Mono.error(new IllegalStateException("Max file number reached")))
                .switchIfEmpty(Mono.defer(() ->
                        repository.findByNameAndOwner(request.getFilename(), request.getUsername())
                        .flatMap(f -> Mono.error(new IllegalStateException("File already exist")))
                        .switchIfEmpty(Mono.defer(() -> fileParts.flatMap(part ->
                                saveFile(headers, part, file)).collect(Collectors.toList())
                        .flatMap(list -> {
                            var monoList = Mono.just(list);
                            var newFile = repository.save(file);
                            return Mono.when(monoList, newFile).then(monoList);
                        }))))).cast(List.class);

    }

    public Mono<File> deleteFile(File f) {

        DeleteObjectRequest delete = DeleteObjectRequest
                .builder()
                .bucket(properties.getBucket())
                .key(f.getId())
                .build();

        return repository.findById(f.getId())
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")))
                .filter(file -> file.getOwner().equals(f.getOwner()))
                .switchIfEmpty(Mono.error(new IllegalStateException("The file is protected")))
                .flatMap(file -> Mono.fromFuture(asyncClient.deleteObject(delete))
                        .flatMap(response -> {
                            checkResult(response, new DeleteFailedException(response));
                            var deleted = repository.delete(file);
                            var monoFile = Mono.just(file);
                            return Mono.when(deleted, monoFile).then(monoFile);
                }));
    }

    public Mono<File> updateFileInfo(File file) {
        return repository.findById(file.getId())
                .switchIfEmpty(Mono.error(new IllegalStateException("File does not exists")))
                .filter(f -> f.getOwner().equals(file.getOwner()))
                .switchIfEmpty(Mono.error(new IllegalStateException("File protected")))
                .flatMap(__ -> repository.save(file));
    }

    public Flux<File> searchByCategory(List<String> categories) {
        return repository.findFileByCategories(categories)
                .filter(f -> !f.isPrivate())
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")));
    }

    public Flux<File> getByTags(List<String> tags) {
        return repository.findFileByTagsContaining(tags)
                .filter(f -> !f.isPrivate())
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")));

    }

    public Flux<File> findByName(String name) {
        return repository.findAllByName(name)
                .filter(f -> !f.isPrivate())
                .switchIfEmpty(Mono.error(new IllegalStateException("File not found")));
    }

   private boolean isAccessible(File repo, String username, String password) {
       if(repo.isPrivate() && !repo.getOwner().equals(username))
           return false;
       else if(!repo.isPrivate() && Objects.nonNull(repo.getPassword()))
           return repo.getPassword().equals(password);
       return true;
   }

   private Mono<String> saveFile(HttpHeaders headers, FilePart part, File file) {
        String fileKey = file.getId();

        Map<String, String> metadata = new HashMap<>();
        String fileName = file.getName();
        String bucketName = properties.getBucket();

        metadata.put("filename", fileName);
        MediaType mediaType = headers.getContentType();
        if(mediaType == null) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        CompletableFuture<CreateMultipartUploadResponse> future = asyncClient
                .createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .contentType(mediaType.toString())
                        .key(fileKey)
                        .metadata(metadata)
                        .build());

        final UploadState uploadState = new UploadState(bucketName, fileKey);

        return Mono
                .fromFuture(future)
                .flatMapMany(response -> {
                    checkResult(response, new UploadFailedException(response));
                    uploadState.setUploadId(response.uploadId());
                    return part.content();
                })
                .bufferUntil(buffer -> {
                    uploadState.setBuffered(uploadState.getBuffered()
                            + buffer.readableByteCount());
                    if(uploadState.getBuffered() >= properties.getMultiPartMinSize()) {
                        uploadState.setBuffered(0);
                        return true;
                    }
                    return false;
                })
                .map(this::concatBuffers)
                .flatMap(buffer -> uploadPart(uploadState, buffer))
                .onBackpressureBuffer()
                .reduce(uploadState, (state, completedPart) -> {
                    state.getCompleteParts().put(completedPart.partNumber(), completedPart);
                    return state;
                })
                .flatMap(this::completeUpload)
                .map(response -> {
                    checkResult(response, new UploadFailedException(response));
                    return uploadState.getFileKey();
                });
    }

    private Mono<CompleteMultipartUploadResponse> completeUpload(UploadState state) {
        CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
                .parts(state.getCompleteParts().values())
                .build();

        return Mono.fromFuture(asyncClient.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(state.getBucket())
                        .uploadId(state.getUploadId())
                        .multipartUpload(multipartUpload)
                        .key(state.getFileKey())
                        .build()));
    }

    private ByteBuffer concatBuffers(List<DataBuffer> buffers) {
        int partSize = 0;
        for (DataBuffer b : buffers) {
            partSize += b.readableByteCount();
        }

        ByteBuffer partData = ByteBuffer.allocate(partSize);
        buffers.forEach(buffer -> partData.put(buffer.asByteBuffer()));

        partData.rewind();
        return partData;
    }

    private Mono<CompletedPart> uploadPart(UploadState uploadState, ByteBuffer byteBuffer) {
        uploadState.setPartCounter(uploadState.getPartCounter()+1);
        final int partNumber = uploadState.getPartCounter();

        CompletableFuture<UploadPartResponse> request = asyncClient.uploadPart(
                UploadPartRequest.builder()
                        .bucket(uploadState.getBucket())
                        .key(uploadState.getFileKey())
                        .partNumber(partNumber)
                        .uploadId(uploadState.getUploadId())
                        .contentLength((long) byteBuffer.capacity())
                        .build(), AsyncRequestBody.fromPublisher(Mono.just(byteBuffer))
        );

        return Mono.fromFuture(request)
                .map(uploadPartResult -> {
                    checkResult(uploadPartResult,
                            new UploadFailedException(uploadPartResult));
                    return CompletedPart.builder()
                            .eTag(uploadPartResult.eTag())
                            .partNumber(partNumber)
                            .build();
                });
    }

    private static void checkResult(SdkResponse result, RuntimeException ex) {
        if(result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            throw ex;
        }
    }

    private String getMetadataItem(GetObjectResponse sdkResponse, String defaultValue) {
        for( Map.Entry<String, String> entry : sdkResponse.metadata().entrySet()) {
            if ( entry.getKey().equalsIgnoreCase("filename")) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }
}
