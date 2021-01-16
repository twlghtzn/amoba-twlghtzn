package com.twlghtzn.amoba.model;

import com.twlghtzn.amoba.util.Color;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
  @ManyToMany // (mappedBy = "moves")
  private List<FieldChain> fieldChains;
  @ManyToOne
  private Game game;

  public Move(int valX, int valY, Color color, Game game) {
    field = getNameFromPosition(valX, valY);
    this.color = color;
    fieldChains = new ArrayList<>();
    this.game = game;
  }

  public Move(String field, Color color, Game game) {
    this.field = field;
    this.color = color;
    this.game = game;
    fieldChains = new ArrayList<>();
  }

  public void addFieldChain(FieldChain fieldChain) {
    fieldChains.add(fieldChain);
  }

  public String getNameFromPosition(int valX, int valY) {
    return valX + "." + valY;
  }

  public int[] getPositionsFromName(String name) {
    String[] strCoordinates = name.split("\\.");
    int[] coordinates = new int[2];
    coordinates[0] = Integer.parseInt(strCoordinates[0]);
    coordinates[1] = Integer.parseInt(strCoordinates[1]);
    return coordinates;
  }

  public int getValXFromName() {
    return getPositionsFromName(field)[0];
  }

  public int getValYFromName() {
    return getPositionsFromName(field)[1];
  }

  public void removeFieldChain(FieldChain fieldChain) {
    fieldChains.remove(fieldChain);
  }
}
