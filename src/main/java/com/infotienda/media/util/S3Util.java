package com.infotienda.media.util;

import com.infotienda.media.exception.S3Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

@Component
@Slf4j
public class S3Util {

    public static File convertMultiPartToFile(MultipartFile file) {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
            return convFile;
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File. originalFilename={}", file.getOriginalFilename(), e);
            throw new S3Exception("Could not convert multipart file to file", e);
        }
    }
}
