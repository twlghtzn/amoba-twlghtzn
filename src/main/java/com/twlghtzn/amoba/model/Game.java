package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Game {

  @Id
  private String id;
  private GameState gameState;
  @LazyCollection(LazyCollectionOption.FALSE)
  @OneToMany
  private List<Move> moves;

  // key: field (e.g. "1.1"), value: parent (e.g. "2.2")
  @ElementCollection
//  @MapKeyColumn
//  @Column
//  @CollectionTable
  private Map<String, String> diagDownParents;
  @ElementCollection
  private Map<String, String> diagUpParents;
  @ElementCollection
  private Map<String, String> horizParents;
  @ElementCollection
  private Map<String, String> vertParents;

  public Game(String id) {
    this.id = id;
    gameState = GameState.BLUE_NEXT;
    moves = new ArrayList<>();
    diagDownParents = new HashMap<>();
    diagUpParents = new HashMap<>();
    horizParents = new HashMap<>();
    vertParents = new HashMap<>();
  }

  public List<Map<String, String>> getParents() {
    return Arrays.asList(diagDownParents, diagUpParents, horizParents, vertParents);
  }

  public void addMove(Move move) {
    moves.add(move);
  }

  public void addConnection(Dir dir, String key, String value) {
    switch (dir) {
      case DIAG_DOWN:
        diagDownParents.put(key, value);
        break;
      case DIAG_UP:
        diagUpParents.put(key, value);
        break;
      case HORIZ:
        horizParents.put(key, value);
        break;
      case VERT:
        vertParents.put(key, value);
    }
  }
}
