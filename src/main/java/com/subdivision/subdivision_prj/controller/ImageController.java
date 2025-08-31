package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 이미지 업로드를 처리하는 컨트롤러입니다.
 * @author subdivision
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
// application.properties의 cloud.aws.s3.enabled 값이 true일 때만 이 컨트롤러 Bean을 생성합니다.
@ConditionalOnProperty(name = "cloud.aws.s3.enabled", havingValue = "true")
public class ImageController {

    private final S3Uploader s3Uploader;

    /**
     * 클라이언트로부터 받은 이미지 파일을 S3에 업로드하고, 저장된 파일의 경로(Key)를 반환합니다.
     * @param image 'image'라는 이름으로 전송된 MultipartFile
     * @return 성공 시 HTTP 200 OK 상태와 함께 S3 파일 경로 문자열을, 실패 시 HTTP 500 Internal Server Error를 반환합니다.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            // S3Uploader 서비스를 호출하여 파일을 "images" 디렉토리에 업로드합니다.
            String imageUrl = s3Uploader.upload(image, "images");
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            // 파일 처리 중 입출력 예외가 발생하면 서버 에러로 응답합니다.
            // 예: 파일 변환 실패, 파일 읽기 실패 등
            return ResponseEntity.status(500).body("이미지 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            // S3 업로드 중 발생할 수 있는 모든 예외(자격증명, 권한 등)를 처리합니다.
            return ResponseEntity.status(500).body("이미지 업로드 중 서버에서 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

