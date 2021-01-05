package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Info;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Move {

  private int valX;
  private int valY;
  private Enum<Info> info;

  public Move(int valX, int valY, Enum<Info> info) {
    this.valX = valX;
    this.valY = valY;
    this.info = info;
  }
}
