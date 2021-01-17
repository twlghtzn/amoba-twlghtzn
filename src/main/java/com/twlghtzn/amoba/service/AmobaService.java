package com.twlghtzn.amoba.service;

import com.twlghtzn.amoba.dto.GameStartResponse;
import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.dto.MoveResponse;
import com.twlghtzn.amoba.dto.MovesView;
import com.twlghtzn.amoba.exceptionhandling.RequestIncorrectException;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.repository.GameRepository;
import com.twlghtzn.amoba.repository.MoveRepository;
import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmobaService {

  private final MoveRepository moveRepository;
  private final GameRepository gameRepository;

  @Autowired
  public AmobaService(MoveRepository moveRepository, GameRepository gameRepository) {
    this.moveRepository = moveRepository;
    this.gameRepository = gameRepository;
  }

  public String generateGameId() {
    return UUID.randomUUID().toString();
  }

  public GameStartResponse startNewGame() {

    List<String> gameIds = gameRepository.getALLGameIds();

    String id;
    do {
      id = generateGameId();
    } while (gameIds.contains(id));
    gameRepository.save(new Game(id));
    return new GameStartResponse(id);
  }

  public MoveResponse saveMove(MoveRequest moveRequest) {
    checkRequest(moveRequest);
    String id = moveRequest.getId();
    int valX = moveRequest.getValX();
    int valY = moveRequest.getValY();
    Color color = Color.valueOf(moveRequest.getColor());

    Game game = gameRepository.findGameById(id);
    checkGameState(game.getGameState(), color);

    String field = getNameFromPosition(valX, valY);
    Move move = new Move(field, color, game);
    moveRepository.save(move);
    game.addMove(move);
    gameRepository.save(game);

    addMoveToParentLists(move.getField(), id);
    int movesCount = game.getMoves().size();
    if (movesCount > 2) {
      registerParents(move, id);
      rewireParents(id);
    }
    GameState gameState = checkIfTheresAWinner(id);
    game.setGameState(gameState);
    gameRepository.save(game);

    return new MoveResponse(gameState.toString());
  }

  // move is registered with itself as parent (e.g. key: "1.1", value: "1.1")
  public void addMoveToParentLists(String field, String id) {
    Game game = gameRepository.findGameById(id);
    for (Dir dir : Dir.values()) {
      game.addConnection(dir, field, field);
    }
    gameRepository.save(game);
  }

  public void registerParents(Move newMove, String id) {
    Game game = gameRepository.findGameById(id);
    Map<String, Dir> neighboringSameColorMoves = getNeighboringSameColorFields(newMove, id);

    if (neighboringSameColorMoves.size() != 0) {
      int newMoveValX = getValXFromField(newMove.getField());
      int newMoveValY = getValYFromField(newMove.getField());

      for (Map.Entry<String, Dir> moveEntry : neighboringSameColorMoves.entrySet()) {
        int moveEntryValX = getValXFromField(moveEntry.getKey());
        int moveEntryValY = getValYFromField(moveEntry.getKey());
        Dir dir = moveEntry.getValue();

        // coordinates decide, which move will be the parent
        if (dir.equals(Dir.VERT)) {
          // move with smaller Y is parent
          if (moveEntryValY < newMoveValY) {
            game.addConnection(dir, newMove.getField(), moveEntry.getKey());
          } else {
            game.addConnection(dir, moveEntry.getKey(), newMove.getField());
          }
        } else {
          // move with smaller X is parent
          if (moveEntryValX < newMoveValX) {
            game.addConnection(dir, newMove.getField(), moveEntry.getKey());
          } else {
            game.addConnection(dir, moveEntry.getKey(), newMove.getField());
          }
        }
      }
    }
    gameRepository.save(game);
  }

  public void rewireParents(String id) {
    Game game = gameRepository.findGameById(id);
    List<Map<String, String>> parentsRegister = game.getParents();

    for (Map<String, String> connections : parentsRegister) {

      for (Map.Entry<String, String> connection : connections.entrySet()) {

        String field = connection.getKey();

        for (Map.Entry<String, String> connectionToCorrect : connections.entrySet()) {
          // in a chain, the move with the smallest X or Y coordinate is the parent of ALL moves in the chain
          // if a move has another move as parent, but that move has again a parent,
          // than the first move's parent has to be changed to the second move's parent
          if (!connectionToCorrect.getKey().equals(field) &&
              connectionToCorrect.getValue().equals(field)) {

            connectionToCorrect.setValue(connection.getValue());
          }
        }
      }
    }
    gameRepository.save(game);
  }

  public Map<String, Dir> getNeighboringSameColorFields(Move newMove, String id) {
    int newMoveValX = getValXFromField(newMove.getField());
    int newMoveValY = getValYFromField(newMove.getField());
    List<Move> sameColorMoves =
        moveRepository.getMovesByIdAndByColor(id, newMove.getColor().name());
    Map<String, Dir> neighboringSameColorMoves = new HashMap<>();

    for (Move move : sameColorMoves) {
      int moveValX = getValXFromField(move.getField());
      int moveValY = getValYFromField(move.getField());

      if (moveValX == newMoveValX &&
          (moveValY == newMoveValY + 1 || moveValY == newMoveValY - 1)) {
        neighboringSameColorMoves.put(move.getField(), Dir.VERT);
      } else if (moveValY == newMoveValY &&
          (moveValX == newMoveValX + 1 || moveValX == newMoveValX - 1)) {
        neighboringSameColorMoves.put(move.getField(), Dir.HORIZ);
      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY + 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY - 1)) {
        neighboringSameColorMoves.put(move.getField(), Dir.DIAG_DOWN);
      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY - 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY + 1)) {
        neighboringSameColorMoves.put(move.getField(), Dir.DIAG_UP);
      }
    }
    return neighboringSameColorMoves;
  }

  public void checkGameState(GameState gameState, Color color) {
    checkMoveOrder(gameState, color);
    checkGameState(gameState);
  }

  public void checkMoveOrder(GameState gameState, Color color) {
    if ((gameState.equals(GameState.BLUE_NEXT) && color.equals(Color.RED)) ||
        (gameState.equals(GameState.RED_NEXT) && color.equals(Color.BLUE))) {
      throw new RequestIncorrectException("It's not your turn");
    }
  }

  public GameState checkIfTheresAWinner(String id) {
    Game game = gameRepository.findGameById(id);
    List<Map<String, String>> parentsRegister = game.getParents();
    int counter = 0;
    String parentField = "none";

    for (Map<String, String> connections : parentsRegister) {

      for (Map.Entry<String, String> checkerConnection : connections.entrySet()) {
        // count how many times a field is registered as parent in a specific direction
        String field = checkerConnection.getKey();

        for (Map.Entry<String, String> checkedConnection : connections.entrySet()) {

          if (checkedConnection.getValue().equals(field)) {
            counter++;
            parentField = field;
          }
        }
        if (counter < 5) {
          counter = 0;
          parentField = "none";
        }
      }
    }
    if (!parentField.equals("none")) {
      Move move = moveRepository.findMoveByGameIdAndField(game.getId(), parentField);
      Color winnersColor = move.getColor();
      if (winnersColor.equals(Color.BLUE)) {
        return GameState.BLUE_WON;
      } else if (winnersColor.equals(Color.RED)) {
        return GameState.RED_WON;
      }
    }
    return swapPlayer(game);
  }

  public GameState swapPlayer(Game game) {
    GameState gameState = game.getGameState();
    if (gameState.equals(GameState.RED_NEXT)) {
      return GameState.BLUE_NEXT;
    } else {
      return GameState.RED_NEXT;
    }
  }

  public void checkGameState(GameState gameState) {
    if (gameState.equals(GameState.RED_WON) || gameState.equals(GameState.BLUE_WON)) {
      throw new RequestIncorrectException("This game has finished");
    }
  }

  public MovesView generateMovesView(String id) {
    Game game = gameRepository.findGameById(id);
    List<Move> moves = game.getMoves();
    Map<String, Color> mappedMovesData = new HashMap<>();

    for (Move move : moves) {
      mappedMovesData.put(move.getField(), move.getColor());
    }
    GameState status = game.getGameState();
    return new MovesView(mappedMovesData, status);
  }

  public void checkRequest(MoveRequest moveRequest) {
    if (moveRequest.getId() == null) {
      throw new RequestIncorrectException("Id missing");
    } else {
      Optional<Game> optionalGame = gameRepository.findById(moveRequest.getId());
      if (!optionalGame.isPresent()) {
        throw new RequestIncorrectException("Invalid id");
      }
    }
    if (moveRequest.getValX() == null || moveRequest.getValY() == null) {
      throw new RequestIncorrectException("Move coordinates missing");
    }
    if (moveRequest.getColor() == null) {
      throw new RequestIncorrectException("Color missing");
    } else if (!moveRequest.getColor().equals(Color.RED.name()) &&
        !moveRequest.getColor().equals(Color.BLUE.name())) {
      throw new RequestIncorrectException("Invalid color");
    }
  }

  public int[] getPositionsFromName(String field) {
    String[] strCoordinates = field.split("\\.");
    int[] coordinates = new int[2];
    coordinates[0] = Integer.parseInt(strCoordinates[0]);
    coordinates[1] = Integer.parseInt(strCoordinates[1]);
    return coordinates;
  }

  public int getValXFromField(String field) {
    return getPositionsFromName(field)[0];
  }

  public int getValYFromField(String field) {
    return getPositionsFromName(field)[1];
  }

  public String getNameFromPosition(int valX, int valY) {
    return valX + "." + valY;
  }
}
