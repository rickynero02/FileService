package com.fileservice.files;

import com.fileservice.utility.UserRoles;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadRequest {
    private String username;
    private String filename;
    private UserRoles role;
    private String description;
}
