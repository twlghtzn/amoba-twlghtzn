package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Game {

  @Id
  private String id;
  private GameState gameState;
  @LazyCollection(LazyCollectionOption.FALSE)
  @OneToMany // (mappedBy = "game", cascade = CascadeType.ALL)
  private List<FieldChain> fieldChains;
  @LazyCollection(LazyCollectionOption.FALSE)
  @OneToMany // (mappedBy = "game", cascade = CascadeType.ALL)
  private List<Move> moves;

  public Game(String id) {
    this.id = id;
    gameState = GameState.BLUE_NEXT;
    fieldChains = new ArrayList<>();
    moves = new ArrayList<>();
  }

  public void addMove(Move move) {
    moves.add(move);
  }

  public void addFieldChain(FieldChain fieldChain) {
    fieldChains.add(fieldChain);
  }

  public void removeFieldChain(FieldChain fieldChain) {
    fieldChains.remove(fieldChain);
  }
}
