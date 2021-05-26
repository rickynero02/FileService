package com.fileservice.files;

import com.fileservice.utility.Message;
import com.fileservice.utility.UserRoles;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
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
    public Mono<Message> getAllFiles(WebSession session) {
        if(session.isStarted() && !session.isExpired()) {
            return service.fetchAllFiles(session.getAttribute("username"))
                    .collect(Collectors.toList())
                    .map(fileList -> new Message().withElement("result", fileList));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));
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
         @RequestPart("filename") String filename, @RequestPart("role") String role) {

        UploadRequest request = new UploadRequest(username, filename, UserRoles.valueOf(role));
        return service.uploadFile(headers, file, request)
                .map(keys -> new Message().withElement("result", keys))
                .onErrorResume(error -> Mono.just(new Message().withElement("error", error.getMessage())));
    }

    @GetMapping(path="/download/{filekey}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable("filekey") String fileKey) {
        return service.downloadFile(fileKey);
    }
}
