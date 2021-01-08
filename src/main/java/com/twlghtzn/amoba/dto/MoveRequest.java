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
  private String id;
  private String color;

  public MoveRequest(Integer valX, Integer valY, String id, String color) {
    this.valX = valX;
    this.valY = valY;
    this.id = id;
    this.color = color;
  }
}
