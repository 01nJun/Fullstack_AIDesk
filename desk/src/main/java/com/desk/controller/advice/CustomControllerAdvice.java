package com.desk.controller.advice;

import com.desk.util.CustomJWTException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * CustomControllerAdvice
 */
@RestControllerAdvice
public class CustomControllerAdvice {


  @ExceptionHandler(NoSuchElementException.class)
  protected ResponseEntity<?> notExist(NoSuchElementException e) {

      String msg = e.getMessage();

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("msg", msg));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<?> handleIllegalArgumentException(MethodArgumentNotValidException e) {

      String msg = e.getMessage();

      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("msg", msg));
  }

  @ExceptionHandler(CustomJWTException.class)
  protected ResponseEntity<?> handleJWTException(CustomJWTException e) {

      String msg = e.getMessage();

      return ResponseEntity.ok().body(Map.of("error", msg));
  }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage();

        // 권한 관련 에러 403 Forbidden
        if (msg != null && (msg.contains("Not allowed") || msg.contains("권한"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
        }

        // 기타 IllegalArgumentException은 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }
}