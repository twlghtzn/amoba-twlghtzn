package com.twlghtzn.amoba.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MoveResponse {

  private String status;

  public MoveResponse(String status) {
    this.status = status;
  }
}
