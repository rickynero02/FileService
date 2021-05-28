package com.fileservice.files;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface FileRepository extends ReactiveCrudRepository<File, String> {
    Flux<File> findAllByOwner(String owner);
    Mono<File> findByNameAndOwner(String name, String owner);
    Flux<File> findFileByCategories(List<String> categories);
    Flux<File> findFileByTagsContaining(List<String> tags);
    Flux<File> findAllByName(String name);
}

