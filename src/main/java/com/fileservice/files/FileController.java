package com.fileservice.files;

import com.fileservice.utility.Message;
import com.fileservice.utility.UserRoles;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/files")
@AllArgsConstructor
@CrossOrigin
public class FileController {

    private final FileService service;

    @GetMapping(path = "/getAll")
    public Mono<Message> getAllFiles(WebSession session) {
        if(session.isStarted() && !session.isExpired()) {
            return service.fetchAllFiles(session.getAttribute("username"))
                    .collect(Collectors.toList())
                    .map(fileList -> new Message().withElement("result", fileList))
                    .onErrorResume(ex -> Mono.just(new Message().withElement("error", ex.getMessage())));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));
    }

    @PostMapping(path = "/info")
    public Mono<Message> getFileInfo(@RequestBody File file, WebSession session) {
        if(session.isStarted() && !session.isExpired()){
            return service.fetchByNameAndOwner(session.getAttribute("username"), file)
                    .map(f -> new Message().withElement("file", f))
                    .onErrorResume(error ->
                            Mono.just(new Message().withElement("error", error.getMessage())));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));
    }


    @PostMapping(path = "/upload")
    public Mono<Message> uploadMultiPart(@RequestHeader HttpHeaders headers,
                                         @RequestPart("file") Flux<FilePart> file,
                                         WebSession session) {
        if(!session.isExpired() && session.isStarted()) {
            UploadRequest request = new UploadRequest(session.getAttribute("username"),
                    UserRoles.valueOf(session.getAttribute("role")));
            return service.uploadFile(headers, file, request)
                    .map(keys -> new Message().withElement("result", keys))
                    .onErrorResume(error -> Mono.just(new Message().withElement("error", error.getMessage())));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));

    }

    @PostMapping(path = "/download")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@RequestBody File f, WebSession session) {
        if(session.isStarted() && !session.isExpired()) {
            return service.downloadFile(f, session.getAttribute("username"));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @DeleteMapping(path = "/delete/{id}")
    public Mono<Message> deleteFile(@PathVariable("id") String id, WebSession session) {
        if(!session.isExpired() && session.isStarted()) {
            return service.deleteFile(id, session.getAttribute("username"))
                    .map(file -> new Message().withElement("result", file))
                    .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));
    }

    @PutMapping(path = "/updateInfo")
    public Mono<Message> updateFileInfo(@RequestBody File file, WebSession session) {
        if(session.isStarted() && !session.isExpired()) {
            return service.updateFileInfo(file, session.getAttribute("username"))
                    .map(f -> new Message().withElement("result", f))
                    .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
        }
        return Mono.just(new Message().withElement("error", "Session expired"));
    }

    @PostMapping("/categories")
    public Mono<Message> searchByCategory(@RequestBody List<String> categories){
        return service.searchByCategory(categories)
                .collect(Collectors.toList())
                .map(files -> new Message().withElement("result", files))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
    }

    @PostMapping("/getByTags")
    public Mono<Message> searchByTags(@RequestBody List<String> tags) {
        return service.getByTags(tags)
                .collect(Collectors.toList())
                .map(f -> new Message().withElement("result", f))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
    }

    @GetMapping("/getByName/{name}")
    public Mono<Message> searchByName(@PathVariable("name") String name) {
        return service.findByName(name)
                .collect(Collectors.toList())
                .map(f -> new Message().withElement("result", f))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err)));
    }
}
