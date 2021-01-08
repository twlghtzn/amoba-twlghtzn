package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class FieldChain {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  private Dir direction;
  private Color color;
  @ManyToMany
  private Map<String, Move> moves;
  @OneToOne
  private Game game;

  public FieldChain(Color color, Dir direction, Game game) {
    this.direction = direction;
    moves = new HashMap<>();
    this.color = color;
    this.game = game;
  }

  public void addMove(Move move) {
    moves.put(move.getField(), move);
  }
}
