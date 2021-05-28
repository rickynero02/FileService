package com.fileservice.files;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Document
public class File {
    @Id
    private String id;
    private String owner;
    private String name;
    private String password = null;
    private long length = 0;
    private LocalDateTime uploadDate;
    private boolean isPrivate = true;
    private String description;
    private List<String> tags;
    private List<String> categories;
}
