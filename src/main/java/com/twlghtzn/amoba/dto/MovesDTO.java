package com.twlghtzn.amoba.dto;

import com.twlghtzn.amoba.util.Info;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovesDTO {

  private Map<String, Info> moves;

  public MovesDTO() {
    moves = new HashMap<>();
  }
}
