package com.twlghtzn.amoba.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MoveResponse {

  private String nextUp;
  private String status;

  public MoveResponse(String nextUp, String status) {
    this.nextUp = nextUp;
    this.status = status;
  }
}
