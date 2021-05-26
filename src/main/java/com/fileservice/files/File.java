package com.fileservice.files;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("unused")
@ToString
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
}
