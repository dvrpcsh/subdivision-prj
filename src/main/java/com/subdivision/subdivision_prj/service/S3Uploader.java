package com.subdivision.subdivision_prj.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * AWS S3에 파일을 업로드하고, 업로드된 파일의 고유 경로(Key)를 반환하는 서비스 클래스입니다.
 * 이 서비스는 'prod' 프로파일과 같이 실제 S3 기능이 필요한 환경에서만 활성화됩니다.
 * @author subdivision
 */
@Slf4j
@RequiredArgsConstructor
@Service
// application.properties의 cloud.aws.s3.enabled 값이 true일 때만 이 Bean을 생성합니다.
// 'local' 환경에서 S3 관련 설정이 없어도 서버가 실행될 수 있도록 해주는 중요한 어노테이션입니다.
@ConditionalOnProperty(name = "cloud.aws.s3.enabled", havingValue = "true")
public class S3Uploader {

    // S3Config에서 Bean으로 등록한 AmazonS3Client가 주입됩니다.
    private final AmazonS3 amazonS3Client;

    // application.properties에 설정한 S3 버킷 이름을 주입받습니다.
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * Controller로부터 받은 MultipartFile 객체를 S3에 업로드하는 메인 메서드입니다.
     * @param multipartFile 업로드 할 파일
     * @param dirName S3 버킷 내에 파일을 저장할 디렉토리 이름 (예: "images")
     * @return S3에 저장된 파일의 고유 경로 (예: "images/xxxxxxxx-xxxx-xxxx.png")
     * @throws IOException 파일 변환 과정에서 발생할 수 있는 예외
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        // 1. MultipartFile을 로컬의 임시 File 객체로 변환합니다. (S3 SDK는 File 객체를 사용하기 때문)
        File uploadFile = convert(multipartFile)
                .orElseThrow(() -> new IllegalArgumentException("MultipartFile -> File 전환에 실패했습니다."));

        // 2. 변환된 File 객체를 S3에 업로드하고, DB에 저장될 파일 경로(Key)를 반환받습니다.
        return upload(uploadFile, dirName);
    }

    /**
     * File 객체를 S3에 업로드하고, 파일 경로를 반환한 뒤, 로컬 임시 파일을 삭제합니다.
     * @param uploadFile S3에 업로드 할 File 객체
     * @param dirName S3 버킷 내의 디렉토리 이름
     * @return 업로드 된 파일의 고유 경로(Key)
     */
    private String upload(File uploadFile, String dirName) {
        // S3에 저장될 파일의 전체 경로(Key)를 생성합니다. (예: "images/고유ID_원본파일이름.png")
        String fileName = dirName + "/" + uploadFile.getName();

        // S3에 파일을 업로드합니다.
        putS3(uploadFile, fileName);

        // 로컬에 생성된 임시 파일을 삭제하여 서버의 디스크 공간을 낭비하지 않도록 합니다.
        removeNewFile(uploadFile);

        // [핵심] 완전한 URL이 아닌, 데이터베이스에 저장될 '파일 경로(Key)'만 반환합니다.
        // 이 경로를 기반으로 PotService에서 Presigned URL을 생성하게 됩니다.
        return fileName;
    }

    /**
     * S3에 실제로 파일을 업로드하는 private 메서드입니다.
     * @param uploadFile 업로드 할 파일
     * @param fileName S3에 저장될 파일의 전체 경로(Key)
     */
    private void putS3(File uploadFile, String fileName) {
        // 파일을 S3 버킷에 저장합니다.
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile));
        log.info("S3에 파일 업로드 완료: {}", fileName);
    }

    /**
     * 로컬에 생성된 임시 파일을 삭제합니다.
     */
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("로컬 임시 파일 삭제 성공: {}", targetFile.getName());
        } else {
            log.warn("로컬 임시 파일 삭제 실패: {}", targetFile.getName());
        }
    }

    /**
     * MultipartFile을 로컬의 임시 File 객체로 변환합니다.
     * 이 때, 파일 이름이 중복되지 않도록 UUID를 사용하여 고유한 파일 이름을 생성합니다.
     * @param file 변환할 MultipartFile
     * @return 변환된 File 객체 (Optional로 감싸서 null 안전성 확보)
     * @throws IOException 파일 쓰기 작업 중 발생할 수 있는 예외
     */
    private Optional<File> convert(MultipartFile file) throws IOException {
        if (file.getOriginalFilename() == null) {
            throw new IOException("원본 파일 이름이 없습니다.");
        }
        // 고유한 파일 이름을 생성합니다. (예: "xxxxxxxx-xxxx_원본파일이름.png")
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // 시스템의 기본 임시 디렉토리에 파일을 생성합니다.
        File convertFile = new File(System.getProperty("java.io.tmpdir") + "/" + uniqueFileName);
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes()); // MultipartFile의 내용을 File에 씁니다.
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }
}

