package com.fileservice.files;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Getter
@Setter
public class UploadState {
    private final String bucket;
    private final String fileKey;

    private String uploadId;
    private int partCounter;
    private final Map<Integer, CompletedPart> completeParts = new HashMap<>();
    private int buffered = 0;

    public UploadState(String bucket, String fileKey) {
        this.bucket = bucket;
        this.fileKey = fileKey;
    }
}
