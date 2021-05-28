package com.fileservice.categories;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    public Flux<Category> getAllCategories() {
        return repository.findAll();
    }
}
