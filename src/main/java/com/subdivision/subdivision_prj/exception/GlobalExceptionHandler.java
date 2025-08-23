package com.subdivision.subdivision_prj.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException을 처리하는 핸들러
     * 서비스 계층에서 발생하는 대부분의 비즈니스 로직 예외를 처리합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        // 예외 메시지를 JSON 형식으로 만들어서 400 Bad Request 상태와 함께 반환
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}