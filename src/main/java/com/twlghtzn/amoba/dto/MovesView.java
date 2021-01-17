package com.twlghtzn.amoba.dto;

import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.GameState;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MovesView {

  private Map<String, Color> moves;
  private GameState status;

  public MovesView(Map<String, Color> moves, GameState status) {
    this.moves = moves;
    this.status = status;
  }
}
