package com.fileservice.files;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FileRepository extends ReactiveCrudRepository<File, String> {
    Flux<File> findAllByOwner(String owner);
}
