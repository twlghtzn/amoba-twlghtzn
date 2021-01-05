package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.State;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Game {

  private List<Move> moves;
  private State state;
  private List<Component> components;

  public Game() {
    moves = new ArrayList<>();
    state = State.ONGOING;
    components = new ArrayList<>();
  }
}
