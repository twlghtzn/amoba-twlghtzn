package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Game {

  @Id
  private String id;
  private GameState gameState;
  @OneToMany
  private List<Move> moves;

  // key: field (e.g. "1.1"), value: parent (e.g. "2.2")
  @ElementCollection
  private Map<String, String> diagDownConnections;
  @ElementCollection
  private Map<String, String> diagUpConnections;
  @ElementCollection
  private Map<String, String> horizConnections;
  @ElementCollection
  private Map<String, String> vertConnections;

  public Game(String id) {
    this.id = id;
    gameState = GameState.BLUE_NEXT;
    moves = new ArrayList<>();
    diagDownConnections = new HashMap<>();
    diagUpConnections = new HashMap<>();
    horizConnections = new HashMap<>();
    vertConnections = new HashMap<>();
  }

  public List<Map<String, String>> getConnections() {
    return Arrays.asList(diagDownConnections, diagUpConnections, horizConnections, vertConnections);
  }

  public Map<String, String> getConnectionsByDirection(Dir dir) {
    switch (dir) {
      case DIAG_DOWN:
        return diagDownConnections;
      case DIAG_UP:
        return diagUpConnections;
      case HORIZ:
        return horizConnections;
      default:
        return vertConnections;
    }
  }

  public boolean isFieldOccupied(String field) {
    return diagDownConnections.containsKey(field);
  }

  public void addMove(Move move) {
    moves.add(move);
  }

  public void addConnection(Dir dir, String field, String parent) {
    switch (dir) {
      case DIAG_DOWN:
        diagDownConnections.put(field, parent);
        break;
      case DIAG_UP:
        diagUpConnections.put(field, parent);
        break;
      case HORIZ:
        horizConnections.put(field, parent);
        break;
      case VERT:
        vertConnections.put(field, parent);
    }
  }
}
