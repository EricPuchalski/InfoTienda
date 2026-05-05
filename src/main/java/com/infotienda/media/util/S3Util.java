package com.infotienda.media.util;

import com.infotienda.media.exception.S3Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Slf4j
public class S3Util {

    public static File convertMultiPartToFile(MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("upload-", resolveFileExtension(file));
            file.transferTo(tempFile);
            return tempFile.toFile();
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File. originalFilename={}", file.getOriginalFilename(), e);
            throw new S3Exception("Could not convert multipart file to file", e);
        }
    }

    private static String resolveFileExtension(MultipartFile file) {
        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.tmp");
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".tmp";
        }

        String extension = fileName.substring(dotIndex).replaceAll("[^A-Za-z0-9.]", "");
        if (extension.isBlank() || extension.length() > 10 || !extension.startsWith(".")) {
            return ".tmp";
        }
        return extension;
    }
}
