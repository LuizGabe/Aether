package com.aether.api.controller;

import com.aether.api.service.PluggyException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, java.time.DateTimeException.class})
    public ResponseEntity<Map<String,String>> invalid(RuntimeException error) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message",error.getMessage()==null?"Dados inválidos.":error.getMessage()));
    }

    @ExceptionHandler(PluggyException.class)
    public ResponseEntity<Map<String, String>> pluggy(PluggyException error) {
        HttpStatus status = "PLUGGY_CREDENTIALS_MISSING".equals(error.getCode()) ? HttpStatus.PRECONDITION_REQUIRED : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("code", error.getCode(), "message", error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_FAILED", "message", "Dados obrigatorios ausentes ou invalidos."));
    }
}
