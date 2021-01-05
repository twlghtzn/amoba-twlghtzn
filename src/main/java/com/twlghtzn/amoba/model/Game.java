package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.State;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Game {

  private List<Move> moves;
  private Enum<State> state;
  private ActualBoard actualBoard;
  private List<Direction> connections;

  public Game() {
    moves = new ArrayList<>();
    state = State.ONGOING;
    actualBoard = new ActualBoard();
    connections = new ArrayList<>();
    initConnections();
  }

  public void initConnections() {
    Direction diagUp = new Direction(Dir.DIAG_UP);
    connections.add(diagUp);
    Direction diagDown = new Direction(Dir.DIAG_DOWN);
    connections.add(diagDown);
    Direction horiz = new Direction(Dir.HORIZ);
    connections.add(horiz);
    Direction vert = new Direction(Dir.VERT);
    connections.add(vert);
  }

  public Direction getDirectionByName(Enum<Dir> name) {
    for (Direction dir : connections) {
      if (dir.getName().equals(name)) {
        return dir;
      }
    }
    return null;
  }
}
