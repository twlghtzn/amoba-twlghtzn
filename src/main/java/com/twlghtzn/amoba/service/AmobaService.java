package com.twlghtzn.amoba.service;

import com.twlghtzn.amoba.dto.GameStartResponse;
import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.dto.MoveResponse;
import com.twlghtzn.amoba.dto.MovesDTO;
import com.twlghtzn.amoba.exceptionhandling.RequestIncorrectException;
import com.twlghtzn.amoba.model.ActualBoard;
import com.twlghtzn.amoba.model.Component;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.Info;
import com.twlghtzn.amoba.util.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmobaService {

  private final Map<String, Game> boards;
  private final int boardSize;

  @Autowired
  public AmobaService() {
    boards = new HashMap<>();
    boardSize = 20;
  }

// region    -------------- setup & moves ----------------

  public String generateGameId() {
    return UUID.randomUUID().toString();
  }

  public GameStartResponse startNewGame() {
    String id;
    do {
      id = generateGameId();
    } while (boards.containsKey(id));

    Game game = new Game();

    boards.put(id, game);

    return new GameStartResponse(id);
  }

  public ActualBoard generateActualBoard(String id) {

    List<Move> moves = boards.get(id).getMoves();
    ActualBoard actualBoard = new ActualBoard(boardSize);
    Map<String, Info> fields = actualBoard.getFields();

    for (Move move : moves) {
      fields.put(move.getValX() + "." + move.getValY(), move.getInfo());
    }
    actualBoard.setFields(fields);
    return actualBoard;
  }

  public MovesDTO generateMovesDTO(String id) {
    ActualBoard actualBoard = generateActualBoard(id);
    MovesDTO movesDTO = new MovesDTO();
    Map<String, Info> actualMoves = new HashMap<>();
    for (Map.Entry<String, Info> field : actualBoard.getFields().entrySet()) {
      if (!field.getValue().equals(Info.EMPTY)) {
        actualMoves.put(field.getKey(), field.getValue());
      }
    }
    movesDTO.setMoves(actualMoves);
    return movesDTO;
  }

  public MoveResponse saveMove(MoveRequest moveRequest) {
    checkRequest(moveRequest);
    String id = moveRequest.getId();
    int valX = moveRequest.getValX();
    int valY = moveRequest.getValY();
    Info info = getPlayerFromString(moveRequest.getPlayer());

    if (isMoveAllowed(id, valX, valY, info)) {
      Move move = new Move(valX, valY, info);
      Game game = boards.get(id);

      addNewConnections(valX, valY, info, id);

      List<Move> moves = game.getMoves();
      moves.add(move);
      game.setMoves(moves);

      boards.put(id, game);

      State state = checkIfTheresAWinner(game);

      if (state.equals(State.BLUE_WON)) {
        return new MoveResponse("-", "Move saved, Blue won");
      } else if (state.equals(State.RED_WON)) {
        return new MoveResponse("-", "Move saved, Red won");
      }

      String nextUp = nextUp(info);
      return new MoveResponse(nextUp, "Move saved");
    }
    return new MoveResponse("", "Something went wrong");
  }

  public String nextUp(Enum<Info> info) {
    if (info.equals(Info.BLUE)) {
      return "Red";
    } else {
      return "Blue";
    }
  }

  public Info getPlayerFromString(String player) {
    if (player.equals("BLUE")) {
      return Info.BLUE;
    } else if (player.equals("RED")) {
      return Info.RED;
    } else {
      throw new IllegalArgumentException("Invalid player info");
    }
  }
// endregion

//region    -------------- checks ----------------

  public void checkRequest(MoveRequest moveRequest) {
    if (moveRequest.getId() == null) {
      throw new RequestIncorrectException("Id missing");
    }
    if (moveRequest.getValX() == null || moveRequest.getValY() == null) {
      throw new RequestIncorrectException("Move coordinates missing");
    }
    if (!areMoveCoordinatesInsideBoard(moveRequest.getValX(), moveRequest.getValY())) {
      throw new RequestIncorrectException("Move coordinates outside board");
    }
    if (moveRequest.getPlayer() == null) {
      throw new RequestIncorrectException("Player info missing");
    }
  }

  public boolean areMoveCoordinatesInsideBoard(int valX, int valY) {
    return valX <= boardSize - 1 &&
        valX >= 0 &&
        valY <= boardSize - 1 &&
        valY >= 0;
  }

  public boolean isMoveAllowed(String id, int valX, int valY, Info info) {
    ActualBoard actualBoard = generateActualBoard(id);
    Map<String, Info> fields = actualBoard.getFields();
    if (!isPlayerAllowed(id, info)) {
      throw new RequestIncorrectException("It's not your turn");
    }
    if (!fields.get(valX + "." + valY).equals(Info.EMPTY)) {
      throw new RequestIncorrectException("This field is occupied");
    }
    return true;
  }

  public boolean isPlayerAllowed(String id, Info info) {
    List<Move> moves = boards.get(id).getMoves();
    if (moves.size() != 0) {
      Move lastMove = moves.get(moves.size() - 1);
      return lastMove.getInfo() != info;
    }
    return true;
  }
  // endregion

  public void addNewConnections(int valX, int valY, Info color, String id) {
    Game game = boards.get(id);
    Map<String, Dir> neighbors = generateNeighbouringFieldsNames(valX, valY);
    List<Move> moves = game.getMoves();
    List<Component> components = game.getComponents();

    for (Map.Entry<String, Dir> neighbor : neighbors.entrySet()) {
      for (Move move : moves) {
        if (neighbor.getKey().equals(move.getValX() + "." + move.getValY())) {
          if (move.getInfo().equals(color)) {
            if (components.size() != 0) {
              for (int i = 0; i < components.size(); i++) {
                if (components.get(i).getFields().contains(neighbor.getKey())) {
                  Component component = components.get(i);
                  List<String> componentFields = component.getFields();
                  componentFields.add(valX + "." + valY);
                  component.setFields(componentFields);
                } else {
                  Component newComponent =
                      createNewComponent(color, neighbor.getKey(), valX, valY, neighbor.getValue());
                  components.add(newComponent);
                }
              }
            } else {
              Component newComponent =
                  createNewComponent(color, neighbor.getKey(), valX, valY, neighbor.getValue());
              components.add(newComponent);
            }
          }
        }
      }
    }
    game.setComponents(components);
    boards.put(id, game);
  }

  public Component createNewComponent(Info color, String field, int valX, int valY, Dir direction) {
    Component newComponent = new Component(color);
    List<String> fields = new ArrayList<>();
    fields.add(field);
    fields.add(valX + "." + valY);
    newComponent.setFields(fields);
    newComponent.setDirection(direction);
    return newComponent;
  }

  public Map<String, Dir> generateNeighbouringFieldsNames(int valX, int valY) {
    Map<String, Dir> neighbors = new HashMap<>();
    int leftColumnX = valX - 1;
    int rightColumnX = valX + 1;
    int topRowY = valY - 1;
    int bottomRowY = valY + 1;

    neighbors.put(leftColumnX + "." + topRowY, Dir.DIAG_DOWN);
    neighbors.put(valX + "." + topRowY, Dir.VERT);
    neighbors.put(rightColumnX + "." + topRowY, Dir.DIAG_UP);
    neighbors.put(leftColumnX + "." + valY, Dir.HORIZ);
    neighbors.put(rightColumnX + "." + valY, Dir.HORIZ);
    neighbors.put(leftColumnX + "." + bottomRowY, Dir.DIAG_UP);
    neighbors.put(valX + "." + bottomRowY, Dir.VERT);
    neighbors.put(rightColumnX + "." + bottomRowY, Dir.DIAG_DOWN);
    return neighbors;
  }

  public State checkIfTheresAWinner(Game game) {
    List<Component> components = game.getComponents();
    for (Component component : components) {
      if (component.getFields().size() > 4) {
        Info color = component.getColor();
        if (color.equals(Info.BLUE)) {
          return State.BLUE_WON;
        } else if (color.equals(Info.RED)) {
          return State.RED_WON;
        }
      }
    }
    return State.ONGOING;
  }
}
