package com.fileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Data
@ConfigurationProperties(prefix = "aws.s3")
public class S3ClientConfigProperties {
    private Region region = Region.EU_CENTRAL_1;
    private URI endpoint = null;
    private String accessKeyId = "AKIAWGBTQG4CKMPXS467";
    private String secretAccessKey = "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";
    private String bucket = "filesharing-project-123";
    private int multiPartMinSize = 0;
}
