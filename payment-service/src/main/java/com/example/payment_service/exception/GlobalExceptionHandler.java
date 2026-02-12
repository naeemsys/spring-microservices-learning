package com.example.payment_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex){
    var errors = ex.getBindingResult().getFieldErrors()
      .stream().map(e -> e.getField() + ":" + e.getDefaultMessage()).collect(Collectors.toList());
    return ResponseEntity.badRequest().body(errors);
  }

}
