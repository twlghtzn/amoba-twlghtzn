package com.twlghtzn.amoba.exceptionhandling;

import lombok.Getter;

@Getter
public class RequestIncorrectException extends RuntimeException {

  private final String error;

  public RequestIncorrectException(String error) {
    super();
    this.error = error;
  }
}
