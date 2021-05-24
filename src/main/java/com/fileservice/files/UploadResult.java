package com.fileservice.files;

import lombok.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class UploadResult {
    private HttpStatus status;
    private String[] keys;

    public UploadResult(HttpStatus status, List<String> keys) {
        this.status = status;
        this.keys = keys == null ? new String[] {}: keys.toArray(new String[] {});
    }
}
