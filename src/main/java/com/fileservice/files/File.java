package com.fileservice.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("unused")
public class File {
    @Id
    private String id;
    private String owner;
    private String name;
    private String password = null;
    private LocalDateTime uploadDate;
    private boolean isPrivate = true;

    public File(String owner,
                String name,
                String password,
                LocalDateTime uploadDate,
                boolean isPrivate) {
        this.owner = owner;
        this.name = name;
        this.password = password;
        this.uploadDate = uploadDate;
        this.isPrivate = isPrivate;
    }
}
