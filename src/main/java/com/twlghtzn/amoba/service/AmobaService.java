package com.twlghtzn.amoba.service;

import com.twlghtzn.amoba.dto.GameStartResponse;
import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.dto.MoveResponse;
import com.twlghtzn.amoba.model.ActualBoard;
import com.twlghtzn.amoba.model.Component;
import com.twlghtzn.amoba.model.Direction;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.Info;
import com.twlghtzn.amoba.util.State;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmobaService {

  private Map<String, Game> boards;
  private int boardSize;

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

    ActualBoard actualBoard = generateActualBoard(id);
    game.setActualBoard(actualBoard);

    boards.put(id, game);

    return new GameStartResponse(id);
  }

  public ActualBoard generateActualBoard(String id) {

    List<Move> moves = boards.get(id).getMoves();
    ActualBoard actualBoard = new ActualBoard(boardSize);
    Map<String, Enum<Info>> fields = actualBoard.getFields();

    for (Move move : moves) {
      fields.put(move.getValX() + "." + move.getValY(), move.getInfo());
    }
    actualBoard.setFields(fields);
    return actualBoard;
  }

  public ActualBoard updateActualBoard(ActualBoard actualBoard, int valX, int valY,
                                       Enum<Info> info) {
    Map<String, Enum<Info>> fields = actualBoard.getFields();
    fields.put(valX + "." + valY, info);
    actualBoard.setFields(fields);
    return actualBoard;
  }

  public Game getGameById(String id) {
    if (boards.containsKey(id)) {
      return boards.get(id);
    } else {
      throw new IllegalArgumentException("Invalid game id");
    }
  }

  public MoveResponse saveMove(MoveRequest moveRequest) {
    checkRequest(moveRequest);
    String id = moveRequest.getId();
    int valX = moveRequest.getValX();
    int valY = moveRequest.getValY();
    Enum<Info> info = evaluatePlayerInfo(moveRequest.getPlayer());
    if (isMoveAllowed(id, valX, valY, info)) {
      Move move = new Move(valX, valY, info);
      Game game = boards.get(id);

      List<Move> moves = game.getMoves();
      moves.add(move);
      game.setMoves(moves);

      ActualBoard oldBoard = game.getActualBoard();
      ActualBoard actualBoard = updateActualBoard(oldBoard, valX, valY, info);
      game.setActualBoard(actualBoard);

      boards.put(id, game);

      addNewConnections(valX, valY, info, actualBoard, id);

      Enum<State> state = checkIfTheresAWinner(game);

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

  public Enum<Info> evaluatePlayerInfo(String player) {
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
      throw new IllegalArgumentException("Id missing");
    }
    if (moveRequest.getValX() == null || moveRequest.getValY() == null) {
      throw new IllegalArgumentException("Move coordinates missing");
    }
    if (!areMoveCoordinatesInsideBoard(moveRequest.getValX(), moveRequest.getValY())) {
      throw new IllegalArgumentException("Move coordinates outside board");
    }
    if (moveRequest.getPlayer() == null) {
      throw new IllegalArgumentException("Player info missing");
    }
  }

  public boolean areMoveCoordinatesInsideBoard(int valX, int valY) {
    return valX <= boardSize - 1 &&
        valX >= 0 &&
        valY <= boardSize - 1 &&
        valY >= 0;
  }

  public boolean isMoveAllowed(String id, int valX, int valY, Enum<Info> info) {
    Game game = getGameById(id);
    ActualBoard actualBoard = game.getActualBoard();
    Map<String, Enum<Info>> fields = actualBoard.getFields();
    if (!isPlayerAllowed(id, info)) {
      throw new IllegalArgumentException("It's not your turn");
    }
    if (!fields.get(valX + "." + valY).equals(Info.EMPTY)) {
      throw new IllegalArgumentException("This field is occupied");
    }
    return true;
  }

  public boolean isPlayerAllowed(String id, Enum<Info> info) {
    List<Move> moves = boards.get(id).getMoves();
    if (moves.size() != 0) {
      Move lastMove = moves.get(moves.size() - 1);
      return lastMove.getInfo() != info;
    }
    return true;
  }
  // endregion

  public void addNewConnections(int valX, int valY, Enum<Info> color,
                                        ActualBoard actualBoard, String id) {
    Game game = boards.get(id);
    Map<String, Enum<Dir>> neighbors = generateNeighbouringFieldsNames(valX, valY);
    Map<String, Enum<Info>> fields = actualBoard.getFields();

    for (Map.Entry<String, Enum<Dir>> neighbor : neighbors.entrySet()) {
      if (fields.get(neighbor.getKey()).equals(color)) {
        Enum<Dir> dir = neighbor.getValue();
        Direction direction = game.getDirectionByName(dir);
        List<Component> components = direction.getComponents();
        for (Component component : components) {
          if (component.getColor().equals(color.toString()) &&
              component.getFields().contains(neighbor.getKey())) {
            List<String> componentFields = component.getFields();
            componentFields.add(valX + "." + valY);
            component.setFields(componentFields);
          } else {
            Component newComponent = new Component(color.toString());
            List<String> newFields = newComponent.getFields();
            newFields.add(neighbor.getKey());
            newFields.add(valX + "." + valY);
            newComponent.setFields(newFields);
          }
        }
        direction.setComponents(components);
        updateDirection(game, direction);
        boards.put(id, game);
      }
    }
  }

  public void updateDirection(Game game, Direction direction) {
    Enum<Dir> name = direction.getName();
    List<Direction> connections = game.getConnections();
    for (Direction dir : connections) {
      if (dir.getName().equals(name)) {
        connections.remove(dir);
        connections.add(direction);
      }
    }
  }

  public Map<String, Enum<Dir>> generateNeighbouringFieldsNames(int valX, int valY) {
    Map<String, Enum<Dir>> neighbors = new HashMap<>();
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

  public Enum<State> checkIfTheresAWinner(Game game) {
    String winnerColor = "";
    List<Direction> connections = game.getConnections();
    for (Direction direction : connections) {
      List<Component> components = direction.getComponents();
      for (Component component : components) {
        List<String> fields = component.getFields();
        if (fields.size() > 5) {
          winnerColor = component.getColor();
        }
      }
    }
    if (winnerColor.equals("BLUE")) {
      return State.BLUE_WON;
    } else if (winnerColor.equals("RED")) {
      return State.RED_WON;
    }
    return State.ONGOING;
  }
}
