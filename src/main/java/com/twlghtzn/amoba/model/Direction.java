package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Dir;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Direction {

  private Enum<Dir> name;
  private List<Component> components;

  public Direction(Enum<Dir> name) {
    this.name = name;
    components = new ArrayList<>();
  }
}
