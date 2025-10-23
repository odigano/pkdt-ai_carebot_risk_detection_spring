package com.project.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.auth0.jwt.exceptions.JWTVerificationException;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex) {
    	log.warn("요청 리소스를 찾을 수 없음: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "요청하신 페이지 또는 리소스를 찾을 수 없습니다."); 
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException ex) {
    	log.warn("지원하지 않는 HTTP 메서드 요청: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "지원하지 않는 요청 방식입니다. 허용되는 방식은 " + ex.getSupportedHttpMethods() + " 입니다."); 
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }
	
    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<Map<String, String>> handleJWTVerificationException(JWTVerificationException ex) {
    	log.warn("JWT 검증 실패: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "인증에 실패했습니다. 유효하지 않거나 만료된 토큰입니다."); 
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
	
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    	Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("유효성 검사 실패: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
    	log.warn("요청 엔티티를 찾지 못함: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage()); 
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    	log.warn("잘못된 요청 본문: {}", ex.getMessage());
    	Map<String, String> error = new HashMap<>();
        error.put("error", "요청 본문(Request Body)의 형식이 잘못되었거나 비어있습니다.");
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFileException(InvalidFileException ex) {
    	log.warn("잘못된 파일 요청: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
    	log.warn("부적절한 인자 값으로 인한 충돌: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
    	log.warn("부적절한 상태로 인한 충돌: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn("지원하지 않는 미디어 타입 요청: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        String errorMessage;

        MediaType contentType = ex.getContentType();
        List<MediaType> supportedMediaTypes = ex.getSupportedMediaTypes();

        if (contentType != null) {
            if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) && supportedMediaTypes.contains(MediaType.MULTIPART_FORM_DATA)) {
                errorMessage = "잘못된 요청 형식입니다. 이 API는 'multipart/form-data' 형식의 요청만 지원합니다. JSON 데이터와 파일을 함께 보내주세요.";
            } else if (contentType.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {
                errorMessage = "지원하지 않는 Content-Type 입니다. form-data로 JSON 객체를 보낼 경우, 해당 파트의 Content-Type을 'application/json'으로 명시적으로 지정해야 합니다.";
            } else {
                errorMessage = String.format("지원하지 않는 Content-Type('%s') 입니다. 지원되는 형식: %s", contentType, supportedMediaTypes);
            }
        } else if (ex.getMessage().contains("Invalid mime type")) {
            errorMessage = "요청의 Content-Type 형식이 올바르지 않습니다. 'type/subtype' 형식(예: 'application/json')을 사용해야 합니다.";
        } else {
            errorMessage = "Content-Type 헤더가 누락되었거나 형식이 잘못되었습니다.";
        }
        
        error.put("error", errorMessage);
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }
    
    @ExceptionHandler(PythonApiException.class)
    public ResponseEntity<Map<String, String>> handlePythonApiException(PythonApiException ex) {
    	log.error("Python 분석 서버 API 오류 응답: status={}, message={}", ex.getStatus(), ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(error);
    }
    
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleResourceAccessException(ResourceAccessException ex) {
        log.error("외부 서비스 연결 실패: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "분석 서버와 통신하는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllUncaughtException(Exception ex) {
        log.error("처리못한 예외 발생: ", ex); 
        Map<String, String> error = new HashMap<>();
        error.put("error", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");
        return ResponseEntity.internalServerError().body(error);
    }
}