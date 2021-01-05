package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Info;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ActualBoard {

  private Map<String, Info> fields;

  public ActualBoard(int boardSize) {
    fields = new HashMap<>();
    for (int i = 0; i < boardSize; i++) {
      for (int j = 0; j < boardSize; j++) {
        fields.put(i + "." + j, Info.EMPTY);
      }
    }
  }
}
