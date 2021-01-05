package com.twlghtzn.amoba.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class GameStartResponse {

  private String id;

  public GameStartResponse(String id) {
    this.id = id;
  }
}
