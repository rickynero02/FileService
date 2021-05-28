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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/files")
@AllArgsConstructor
@CrossOrigin
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
                                         @RequestPart("file") Flux<FilePart> file,
                                         @RequestPart("username") String username,
                                         @RequestPart("role") String role,
                                         @RequestPart("description") String description) {

        UploadRequest request = new UploadRequest(username, UserRoles.valueOf(role), description);
        return service.uploadFile(headers, file, request)
                .map(keys -> new Message().withElement("result", keys))
                .onErrorResume(error -> Mono.just(new Message().withElement("error", error.getMessage())));
    }

    @PostMapping(path = "/download")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@RequestBody File f) {
        return service.downloadFile(f);
    }

    @DeleteMapping(path = "/delete")
    public Mono<Message> deleteFile(@RequestBody File f) {
        return service.deleteFile(f)
                .map(file -> new Message().withElement("result", file))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));
    }

    @PutMapping(path = "/updateInfo")
    public Mono<Message> updateFileInfo(@RequestBody File file) {
        return service.updateFileInfo(file)
                .map(f -> new Message().withElement("result", f))
                .onErrorResume(err -> Mono.just(new Message().withElement("error", err.getMessage())));

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
