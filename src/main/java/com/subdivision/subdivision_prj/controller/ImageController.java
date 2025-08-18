package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Uploader s3Uploader;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) {
        try{
            String imageUrl = s3Uploader.upload(image, "images"); //"images"라는 디렉토리에 저장

            return ResponseEntity.ok(imageUrl);
        } catch(IOException e) {
            e.printStackTrace();

            return ResponseEntity.status(500).body("Failed to upload image.");
        }
    }
}
