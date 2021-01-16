package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Getter
@Setter
public class FieldChain {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  @Enumerated(EnumType.STRING)
  private Dir direction;
  @Enumerated(EnumType.STRING)
  private Color color;
  @LazyCollection(LazyCollectionOption.FALSE)
  @ManyToMany // (cascade = CascadeType.ALL)
  private List<Move> moves;
  @ManyToOne
  private Game game;

  public FieldChain() {
    moves = new ArrayList<>();
  }

  public FieldChain(Color color, Dir direction, Game game) {
    this.direction = direction;
    moves = new ArrayList<>();
    this.color = color;
    this.game = game;
  }

  public void addMove(Move move) {
    moves.add(move);
  }
}
