package com.fileservice.files;

import com.fileservice.utility.Message;
import com.fileservice.utility.UserRoles;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/files")
@AllArgsConstructor
public class FileController {

    private final FileService service;

    @GetMapping(path = "/getAll")
    public Mono<Message> getAllFiles(@RequestParam("username") String username) {
            return service.fetchAllFiles(username)
                    .collect(Collectors.toList())
                    .map(fileList -> new Message().withElement("result", fileList))
                    .onErrorResume(ex -> Mono.just(new Message().withElement("error", ex.getMessage())));
    }

    @PostMapping(path = "/info/{username}/{filename}")
    public Mono<Message> getFileInfo(@PathVariable("username") String username,
            @PathVariable("filename") String filename, @RequestBody String passwd) {
        return service.fetchByNameAndOwner(username, filename, passwd)
                .map(file -> new Message().withElement("file", file))
                .onErrorResume(error ->
                        Mono.just(new Message().withElement("error", error.getMessage())));
    }


    @PostMapping(path = "/upload")
    public Mono<Message> uploadMultiPart(@RequestHeader HttpHeaders headers,
         @RequestPart("file") Flux<FilePart> file, @RequestPart("username") String username,
         @RequestPart("filename") String filename, @RequestPart("role") String role,
         @RequestPart("description") String description) {

        UploadRequest request = new UploadRequest(username, filename,
                UserRoles.valueOf(role), description);
        return service.uploadFile(headers, file, request)
                .map(keys -> new Message().withElement("result", keys))
                .onErrorResume(error -> Mono.just(new Message().withElement("error", error.getMessage())));
    }

    @PostMapping(path="/download")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@RequestBody File f) {
        return service.downloadFile(f);
    }

    @DeleteMapping(path = "/delete")
    public Mono<Message> deleteFile(@RequestBody File f) {
        return service.deleteFile(f)
                .map(file -> new Message().withElement("result", file))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
    }
}
