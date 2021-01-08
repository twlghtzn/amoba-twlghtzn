package com.twlghtzn.amoba.dto;

import com.twlghtzn.amoba.util.Color;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MovesView {

  private Map<String, Color> moves;

  public MovesView(Map<String, Color> moves) {
    this.moves = moves;
  }
}
