package com.fileservice.files;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
public class FileService {

    private final FileRepository repository;

    public Flux<File> fetchAllFiles(String owner) {
        return repository.findAllByOwner(owner);
    }

}
