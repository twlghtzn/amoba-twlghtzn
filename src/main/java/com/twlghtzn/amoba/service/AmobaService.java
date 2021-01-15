package com.twlghtzn.amoba.service;

import com.twlghtzn.amoba.dto.GameStartResponse;
import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.dto.MoveResponse;
import com.twlghtzn.amoba.dto.MovesView;
import com.twlghtzn.amoba.exceptionhandling.RequestIncorrectException;
import com.twlghtzn.amoba.model.FieldChain;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.repository.FieldChainRepository;
import com.twlghtzn.amoba.repository.GameRepository;
import com.twlghtzn.amoba.repository.MoveRepository;
import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmobaService {

  private final MoveRepository moveRepository;
  private final FieldChainRepository fieldChainRepository;
  private final GameRepository gameRepository;

  @Autowired
  public AmobaService(MoveRepository moveRepository, FieldChainRepository fieldChainRepository,
                      GameRepository gameRepository) {
    this.moveRepository = moveRepository;
    this.fieldChainRepository = fieldChainRepository;
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

    Move move = new Move(valX, valY, color, game);
    game.addMove(move);

    moveRepository.save(move);

    addNewConnections(move.getField(), id);
    mergeFieldChains(id);
    GameState gameState = checkIfTheresAWinner(game);
    game.setGameState(gameState);
    gameRepository.save(game);

    return new MoveResponse(gameState.toString());
  }

  public void addNewConnections(String field, String id) {

    Move move = moveRepository.findMoveByGameIdAndField(id, field);

    int valX = move.getValXFromName();
    int valY = move.getValYFromName();
    Color color = move.getColor();
    Game game = move.getGame();

    Map<String, Dir> neighbors = generateNeighbouringFieldsNames(valX, valY);
    List<Move> moves = game.getMoves();

    Map<String, Dir> sameColorNeighbors = neighbors.entrySet().stream()
        .filter(neighbor -> moves.stream()
            .anyMatch(move1 -> neighbor.getKey().equals(move1.getField()) &&
                move1.getColor().equals(move.getColor())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<Move, Dir> sameColorNeighboringMoves = new HashMap<>();

    for (Map.Entry<String, Dir> neighbor : sameColorNeighbors.entrySet()) {
      Dir dir = neighbor.getValue();
      Move move1 = getMoveFromNeighbor(neighbor.getKey(), id, color);
      if (move1.getColor().equals(move.getColor())) {
        sameColorNeighboringMoves.put(move1, dir);
      }
    }

    for (Map.Entry<Move, Dir> sameColorNeighboringMove : sameColorNeighboringMoves.entrySet()) {
      saveTwoMovesToFieldChain(move, sameColorNeighboringMove.getKey(),
          sameColorNeighboringMove.getValue(), id);
      mergeFieldChains(id);
    }
  }

  public void mergeFieldChains(String id) {

    Game game = gameRepository.findGameById(id);

    List<FieldChain> fieldChains = game.getFieldChains();

    if (fieldChains.size() > 1) {

      List<Long> fieldChainToDeleteIds = new ArrayList<>();

      List<Move> moves = game.getMoves();

      for (Dir dir : Dir.values()) {

        List<FieldChain> sameDirectionFieldChains = fieldChains.stream()
            .filter(fieldChain -> fieldChain.getDirection().equals(dir))
            .collect(Collectors.toList());

        if (sameDirectionFieldChains.size() > 1) {

          List<FieldChain> fieldChainsToConnect;

          for (Move move : moves) {

            fieldChainsToConnect = sameDirectionFieldChains.stream()
                .filter(fieldChain -> fieldChain.getMoves().containsKey(move.getField()))
                .collect(Collectors.toList());

            if (fieldChainsToConnect.size() > 1) {

              for (Map.Entry<String, Move> moveToCopy : fieldChainsToConnect.get(1).getMoves()
                  .entrySet()) {
                fieldChainsToConnect.get(0).addMove(moveToCopy.getValue());

                moveToCopy.getValue().removeFieldChain(fieldChainsToConnect.get(1));
              }
              fieldChainsToConnect.get(1).setMoves(null);
              fieldChainsToConnect.get(1).setGame(null);
              fieldChains.remove(fieldChainsToConnect.get(1));
              fieldChainToDeleteIds.add(fieldChainsToConnect.get(1).getId());
              sameDirectionFieldChains.remove(fieldChainsToConnect.get(1));
              break;
            } else {
              fieldChainsToConnect.clear();
            }
          }
        }
      }
      game.setFieldChains(fieldChains);
      gameRepository.save(game);
      if (fieldChainToDeleteIds.size() != 0) {
        for (long idToDelete : fieldChainToDeleteIds) {
          fieldChainRepository.deleteById(idToDelete);
        }
      }
    }
  }

  public void saveTwoMovesToFieldChain(Move move1, Move move2, Dir direction, String id) {

    Game game = gameRepository.findGameById(id);

    FieldChain newFieldChain = new FieldChain(move1.getColor(), direction, game);
    game.addFieldChain(newFieldChain);
    newFieldChain.addMove(move1);
    newFieldChain.addMove(move2);
    fieldChainRepository.save(newFieldChain);
    gameRepository.save(game);
  }

  public Move getMoveFromNeighbor(String field, String id, Color color) {

    Game game = gameRepository.findGameById(id);

    Optional<Move> optionalMove = moveRepository.findMoveByGameAndField(game, field);

    if (optionalMove.isPresent()) {
      return optionalMove.get();
    } else {
      Move move = new Move(field, color, game);
      game.addMove(move);
      moveRepository.save(move);
      return move;
    }
  }

  public List<Map<Dir, Move>> getNeighboringSameColorMoves(Move newMove, String id) {

    int newMoveValX = newMove.getValXFromName();
    int newMoveValY = newMove.getValYFromName();
    List<Move> sameColorMoves = moveRepository.getMovesByIdAndByColor(id, newMove.getColor());

    List<Map<Dir, Move>> neighboringSameColorMoves = new ArrayList<>();

    for (Move move : sameColorMoves) {

      int moveValX = move.getValXFromName();
      int moveValY = move.getValYFromName();

      if (moveValX == newMoveValX &&
          (moveValY == newMoveValY + 1 || moveValY == newMoveValY - 1)) {

        neighboringSameColorMoves.add(addMoveToMap(Dir.VERT, move));

      } else if (moveValY == newMoveValY &&
          (moveValX == newMoveValX + 1 || moveValX == newMoveValX - 1)) {

        neighboringSameColorMoves.add(addMoveToMap(Dir.HORIZ, move));

      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY + 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY - 1)) {

        neighboringSameColorMoves.add(addMoveToMap(Dir.DIAG_DOWN, move));

      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY - 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY + 1)) {

        neighboringSameColorMoves.add(addMoveToMap(Dir.DIAG_UP, move));

      }
    }

    return neighboringSameColorMoves;
  }

/*  public Map<String, Dir> generateNeighbouringFieldsNames(int valX, int valY) {
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
  } */

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

  public GameState checkIfTheresAWinner(Game game) {

    List<FieldChain> fieldChains = fieldChainRepository.findAllByGameId(game.getId());

    Optional<FieldChain> optionalFieldChain = fieldChains.stream()
        .filter(fieldChain1 -> fieldChain1.getMoves().size() > 4)
        .findFirst();

    if (optionalFieldChain.isPresent()) {
      FieldChain fieldChain = optionalFieldChain.get();
      Color winnersColor = fieldChain.getColor();
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

    List<Move> moves = moveRepository.findAllByGameId(id);
    Map<String, Color> mappedMovesData = new HashMap<>();

    for (Move move : moves) {
      mappedMovesData.put(move.getField(), move.getColor());
    }
    return new MovesView(mappedMovesData);
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
}
