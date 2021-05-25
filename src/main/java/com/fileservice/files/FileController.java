package com.fileservice.files;

import com.fileservice.utility.Message;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@RestController
@RequestMapping("api/v1/files")
@AllArgsConstructor
public class FileController {

    private final FileService service;

    @GetMapping(path = "/getAll/{username}")
    public Flux<Message> getAllFiles(@PathVariable("username") String username) {
        return service.fetchAllFiles(username)
                .map(file -> new Message().withElement("name", file.getName())
                        .withElement("uploadDate", file.getUploadDate()));
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
                      @RequestPart("filename") String filename) {
        return service.uploadFile(headers, file, username, filename)
                .map(keys -> new Message().withElement("result", keys))
                .onErrorResume(error -> Mono.just(new Message().withElement("error", error.getMessage())));
    }

    @GetMapping(path="/download/{filekey}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable("filekey") String fileKey) {
        return service.downloadFile(fileKey);
    }
}
