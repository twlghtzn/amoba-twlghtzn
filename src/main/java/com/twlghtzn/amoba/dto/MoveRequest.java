package com.twlghtzn.amoba.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MoveRequest {

  private Integer valX;
  private Integer valY;

  public MoveRequest(Integer valX, Integer valY) {
    this.valX = valX;
    this.valY = valY;
  }
}
