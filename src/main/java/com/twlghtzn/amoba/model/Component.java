package com.twlghtzn.amoba.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Component {

  private List<String> fields;

  public Component(String color) {
    fields = new ArrayList<>();
    fields.add(0, color);
  }

  public String getColor() {
    return fields.get(0);
  }
}
