package com.subdivision.subdivision_prj.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * AWS S3에 파일을 업로드하는 역할을 담당하는 서비스 클래스입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class S3Uploader {

    //S3Config에서 Bean으로 등록한 AmazonS3Client가 주입됩니다.
    private final AmazonS3 amazonS3Client;

    //application.properties에 설정한 S3 버킷 이름을 가져옵니다.
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * Controller로 부터 받은 MultipartFile 객체를 S3에 업로드하는 메인 메서드입니다.
     * @param multipartFile 업로드 할 파일
     * @param dirName S3 버킷 내에 파일을 저장할 디렉토리 이름
     * @return S3에 업로드 된 파일의 RUL
     * @throws IOException 파일 변환 과정에서 발생할 수 있는 예외
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        //MultipartFile을 일반 File 객체로 변환합니다.
        File uploadFile = convert(multipartFile)
                .orElseThrow(() -> new IllegalArgumentException("MultipartFile -> File 전환 실패"));

        //변환된  File 객체를 S3에 업로드합니다.
        return upload(uploadFile, dirName);
    }

    /**
     * 변환된 File 객체를 S3에 업로드하고,로컬에 생성된 임시 파일을 삭제하는 private 메서드입니다.
     * @param uploadFile S3에 업로드 할 File 객체
     * @param dirName S3 버킷 내의 디렉토리 이름
     * @return 업로드 된 파일의 S3 URL
     */
    private String upload(File uploadFile, String dirName) {
        //S3에 저장될 파일의 전체 경로를 생성합니다.
        String fileName = dirName = "/" + uploadFile.getName();

        //S3에 파일을 업로드하고, 업로드 된 파일의 URL을 받아옵니다.
        putS3(uploadFile, fileName);

        removeNewFile(uploadFile); //로컬에 생성된 File 삭제(MultipartFile -> File  전환 시 로컬에 파일 생성됨)

        return fileName; //업로드 된 파일의 S3 URL 주소 반환
    }

    /**
     * S3에 실제로 파일을 업로드하는 핵심 로직입니다.
     * @param uploadFile 업로드 할 파일
     * @param fileName S3에 저장될 파일 이름
     */
    private void putS3(File uploadFile, String fileName) {
        //파일을 업로드합니다.
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile));
    }

    //로컬에 생성된 임시 파일 삭제
    private void removeNewFile(File targetFile) {
        if(targetFile.delete()) {
            log.info("파일이 삭제되었습니다.");
        } else {
            log.info("파일 삭제에 실패했습니다.");
        }
    }

    /**
     * Spring의 MultipartFile을 java.io.File로 변환합니다.
     * @param file 변환할 MultipartFile
     * @return 변환된 File 객체
     * @throws IOException 파일 쓰기 작업 중 발생할 수 있는 예외
     */
    private Optional<File> convert(MultipartFile file) throws IOException {
        //원본 파일 이름에서 확장자를 추출합니다.
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null) {
            throw new IOException("파일 이름이 없습니다.");
        }

        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");

        if(extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex);
        }

        //고유한 파일 이름을 생성합니다. (UUID 사용)
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        //고유한 이름을 가진 File 객체를 생성합니다.
        File converFile = new File(System.getProperty("user.dir") + "/" + uniqueFileName);

        if(converFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(converFile)) {
                fos.write(file.getBytes());
            }

            return Optional.of(converFile);
        }

        return Optional.empty();
    }
}
