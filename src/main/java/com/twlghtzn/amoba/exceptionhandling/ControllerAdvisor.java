package com.twlghtzn.amoba.exceptionhandling;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

  @ExceptionHandler(RequestIncorrectException.class)
  public ResponseEntity<Object> handleRequestIncorrectException(
      RequestIncorrectException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    String message = ex.getError();
    String error = "error";
    body.put("status", error);
    body.put("error", message);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
}
