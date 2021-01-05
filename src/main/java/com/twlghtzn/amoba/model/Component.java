package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.Info;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Component {

  private Dir direction;
  private Info color;
  private List<String> fields;

  public Component(Info color) {
    fields = new ArrayList<>();
    this.color = color;
  }
}
