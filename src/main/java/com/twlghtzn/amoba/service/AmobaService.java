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

    List<String> gameIds = gameRepository.getAllGameIds();

    String id;
    do {
      id = generateGameId();
    } while (gameIds.contains(id));
    gameRepository.save(new Game(id));
    return new GameStartResponse(id);
  }

  public MoveResponse saveMove(MoveRequest moveRequest, String id) {
    checkRequest(moveRequest, id);
    int valX = moveRequest.getValX();
    int valY = moveRequest.getValY();

    Game game = gameRepository.findGameById(id);
    GameState gameState = game.getGameState();
    Color color = getMoveColor(gameState);

    String field = getNameFromPosition(valX, valY);
    Move move = new Move(field, color, game);
    moveRepository.save(move);
    game.addMove(move);
    gameRepository.save(game);

    addMoveToConnectionsLists(field, id);
    if (game.getMoves().size() > 2) {
      registerConnectionsWithNeighboringMoves(move, id);
      rewireParents(id);
    }
    gameState = checkIfTheresAWinner(id, color);
    game.setGameState(gameState);
    gameRepository.save(game);
    return new MoveResponse(gameState.toString());
  }

  public Color getMoveColor(GameState gameState) {
    if (gameState.equals(GameState.RED_NEXT)) {
      return Color.RED;
    } else if (gameState.equals(GameState.BLUE_NEXT)) {
      return Color.BLUE;
    } else {
      throw new RequestIncorrectException("This game has finished");
    }
  }

  // move is registered with itself as parent (e.g. key(field): "1.1", value(parent): "1.1") in all directions
  public void addMoveToConnectionsLists(String field, String id) {
    Game game = gameRepository.findGameById(id);
    for (Dir dir : Dir.values()) {
      game.addConnection(dir, field, field);
    }
    gameRepository.save(game);
  }

  public void registerConnectionsWithNeighboringMoves(Move newMove, String id) {
    Game game = gameRepository.findGameById(id);
    Map<String, Dir> neighboringSameColorMoves = getNeighboringSameColorFields(newMove, id);

    if (neighboringSameColorMoves.size() != 0) {
      int newMoveValX = getValXFromField(newMove.getField());
      int newMoveValY = getValYFromField(newMove.getField());

      for (Map.Entry<String, Dir> moveEntry : neighboringSameColorMoves.entrySet()) {
        int moveEntryValX = getValXFromField(moveEntry.getKey());
        int moveEntryValY = getValYFromField(moveEntry.getKey());
        Dir dir = moveEntry.getValue();

        // between two neighboring moves coordinates decide, which move will be the parent
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

  // in a chain, the move with the smallest X or Y coordinate is the parent of ALL moves in the chain
  // if a move has another move as parent, but that move again has an other move as parent,
  // than the first move's parent has to be changed to the second move's parent
  public void rewireParents(String id) {
    Game game = gameRepository.findGameById(id);
    List<Map<String, String>> parentsRegister = game.getConnections();

    for (Map<String, String> connections : parentsRegister) {

      for (Map.Entry<String, String> connection : connections.entrySet()) {
        String field = connection.getKey();
        String parent = connection.getValue();

        if (!parent.equals(field)) {
          connection.setValue(connections.get(parent));
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

  public GameState checkIfTheresAWinner(String id, Color color) {
    Game game = gameRepository.findGameById(id);
    List<Map<String, String>> parentsRegister = game.getConnections();

    List<Move> sameColorMoves = moveRepository.getMovesByIdAndByColor(id, color.name());

    for (Map<String, String> connections : parentsRegister) {

      for (Move move : sameColorMoves) {
        // count how many times a field is registered as parent in a specific direction
        String field = move.getField();
        long count = connections.entrySet().stream()
            .filter(c -> c.getValue().equals(field))
            .count();

        if (count >= 5) {
          if (color.equals(Color.BLUE)) {
            return GameState.BLUE_WON;
          } else {
            return GameState.RED_WON;
          }
        }
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

  public void checkRequest(MoveRequest moveRequest, String id) {
    Optional<Game> optionalGame = gameRepository.findById(id);
    if (!optionalGame.isPresent()) {
      throw new RequestIncorrectException("Invalid id");
    }
    String field = getNameFromPosition(moveRequest.getValX(), moveRequest.getValY());
    if (optionalGame.get().isFieldOccupied(field)) {
      throw new RequestIncorrectException("There's already a move on this field");
    }
    if (moveRequest.getValX() == null || moveRequest.getValY() == null) {
      throw new RequestIncorrectException("Move coordinates missing");
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
