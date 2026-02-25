package com.infotienda.media.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.infotienda.media.exception.S3Exception;
import java.io.File;

import com.infotienda.media.service.S3Service;
import com.infotienda.media.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 s3client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile multipartFile) {
        File file = S3Util.convertMultiPartToFile(multipartFile);
        String fileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();

        try {
            s3client.putObject(new PutObjectRequest(bucketName, fileName, file));
            return s3client.getUrl(bucketName, fileName).toString();
        } catch (AmazonServiceException e) {
            log.error("Error uploading file to S3", e);
            throw new S3Exception("Error uploading file to S3: " + e.getErrorMessage(), e);
        } finally {
            if (!file.delete()) {
                log.warn("Could not delete temporary file: {}", file.getAbsolutePath());
                file.deleteOnExit();
            }
        }
    }



    public void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            s3client.deleteObject(bucketName, fileName);
        } catch (AmazonServiceException e) {
            log.error("Error deleting file from S3", e);
            throw new S3Exception("Error deleting file from S3: " + e.getErrorMessage(), e);
        }
    }


}
