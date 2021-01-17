package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Color;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Move {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  private String field;
  @Enumerated(EnumType.STRING)
  private Color color;
  @ManyToOne
  private Game game;

  public Move(String field, Color color, Game game) {
    this.field = field;
    this.color = color;
    this.game = game;
  }
}
