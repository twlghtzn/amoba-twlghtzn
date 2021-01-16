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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
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
    moveRepository.save(move);
    game.addMove(move);
    gameRepository.save(game);

    saveFieldChainsWithNeighbors(move.getField(), id);

    mergeFieldChains(id, move.getField());

    GameState gameState = checkIfTheresAWinner(game);
    game.setGameState(gameState);
    gameRepository.save(game);

    return new MoveResponse(gameState.toString());
  }

  public void saveFieldChainsWithNeighbors(String field, String id) {

    Move move = moveRepository.findMoveByGameIdAndField(id, field);

    Game game = gameRepository.findGameById(id);

    List<Map<Dir, Move>> neighboringMoves = getNeighboringSameColorMoves(move, game.getId());

    if (neighboringMoves.size() != 0) {

      FieldChain diagDown = new FieldChain(move.getColor(), Dir.DIAG_DOWN, game);
      FieldChain diagUp = new FieldChain(move.getColor(), Dir.DIAG_UP, game);
      FieldChain horiz = new FieldChain(move.getColor(), Dir.HORIZ, game);
      FieldChain vert = new FieldChain(move.getColor(), Dir.VERT, game);


      for (Map<Dir, Move> moveMap : neighboringMoves) {

        if (moveMap.containsKey(Dir.DIAG_DOWN)) {

//            moveMap.get(Dir.DIAG_DOWN).addFieldChain(diagDown);
//            moveRepository.save(moveMap.get(Dir.DIAG_DOWN));
            diagDown.addMove(moveMap.get(Dir.DIAG_DOWN));

        } else if (moveMap.containsKey(Dir.DIAG_UP)) {

//            moveMap.get(Dir.DIAG_UP).addFieldChain(diagUp);
//          moveRepository.save(moveMap.get(Dir.DIAG_UP));
            diagUp.addMove(moveMap.get(Dir.DIAG_UP));

        } else if (moveMap.containsKey(Dir.HORIZ)) {

//            moveMap.get(Dir.HORIZ).addFieldChain(horiz);
//          moveRepository.save(moveMap.get(Dir.HORIZ));
            horiz.addMove(moveMap.get(Dir.HORIZ));

        } else {

//            moveMap.get(Dir.VERT).addFieldChain(vert);
//          moveRepository.save(moveMap.get(Dir.VERT));
            vert.addMove(moveMap.get(Dir.VERT));

        }
      }

      List<FieldChain> fieldChainsToSave = Arrays.asList(diagDown, diagUp, horiz, vert);

      for (FieldChain fieldChain : fieldChainsToSave) {

        if (fieldChain.getMoves().size() != 0) {

          fieldChain.addMove(move);
          fieldChainRepository.save(fieldChain);

          for (Move moveToSave : fieldChain.getMoves()) {

            moveToSave.addFieldChain(fieldChain);
            moveRepository.save(moveToSave);
          }


          move.addFieldChain(fieldChain);
          moveRepository.save(move);
          game.addFieldChain(fieldChain);
          gameRepository.save(game);

        }
      }
    }
  }

  public void mergeFieldChains(String id, String field) {

    Move newMove = moveRepository.findMoveByGameIdAndField(id, field);

    Game game = gameRepository.findGameById(id);

    List<Move> moves = getNeighboringMovesWithoutDirection(id, newMove);

    List<FieldChain> fieldChains = game.getFieldChains();

    List<FieldChain> fieldChainsToDelete = new ArrayList<>();

    for (Move move : moves) {

      List<FieldChain> fieldChainsOfMove = move.getFieldChains();

      if (fieldChainsOfMove.size() > 1) {

        for (Dir dir : Dir.values()) {

          List<FieldChain> fieldChainsToMerge = new ArrayList<>();

          for (FieldChain fieldChain : fieldChainsOfMove) {

            if (fieldChain.getDirection().equals(dir)) {
              fieldChainsToMerge.add(fieldChain);
            }
          }

          if (fieldChainsToMerge.size() > 1) {

            FieldChain fieldChainToSave = copyMovesToNewFieldChain(fieldChainsToMerge, dir, move.getColor(), game);
            fieldChainRepository.save(fieldChainToSave);
            game.addFieldChain(fieldChainToSave);
            gameRepository.save(game);

            for (FieldChain fieldChainToMerge : fieldChainsToMerge) {

//              move.removeFieldChain(fieldChainToMerge);
//              moveRepository.save(move);
              fieldChainToMerge.setMoves(null);
              fieldChainToMerge.setGame(null);
              fieldChains.remove(fieldChainToMerge);
              fieldChainsToDelete.add(fieldChainToMerge);
            }
          } else {
            fieldChainsToMerge.clear();
          }
        }
      }
    }
    game.setFieldChains(fieldChains);
    gameRepository.save(game);

    if (fieldChainsToDelete.size() != 0) {

      for (FieldChain fieldChainToDelete : fieldChainsToDelete) {
        fieldChainRepository.delete(fieldChainToDelete);
      }
    }


/*    if (fieldChainsToDelete.size() != 0) {

      List<FieldChain> fieldChains = gameRepository.findGameById(id).getFieldChains();

      for (FieldChain fieldChainToDelete : fieldChainsToDelete) {

        fieldChains.remove(fieldChainToDelete);
      }

      game.setFieldChains(fieldChains);
      gameRepository.save(game);

      for (FieldChain fieldChainToDelete : fieldChainsToDelete) {

        for (Move move : fieldChainToDelete.getMoves()) {

          move.removeFieldChain(fieldChainToDelete);
          moveRepository.save(move);
        }

        fieldChainToDelete.setMoves(null);
        fieldChainToDelete.setGame(null);
        fieldChainRepository.delete(fieldChainToDelete);
      }
    } */
  }

  public FieldChain copyMovesToNewFieldChain(List<FieldChain> fieldChainsToMerge, Dir dir, Color color, Game game) {

    FieldChain newFieldChain = new FieldChain(color, dir, game);
    fieldChainRepository.save(newFieldChain);
    List<Move> moves = new ArrayList<>();

    for (FieldChain fieldChain : fieldChainsToMerge) {

      List<Move> movesToCopy = fieldChain.getMoves();

      for (Move move : movesToCopy) {

        if (!moves.contains(move)) {

          move.removeFieldChain(fieldChain);
          move.addFieldChain(newFieldChain);
          moveRepository.save(move);
          moves.add(move);
        }
      }
    }
    newFieldChain.setMoves(moves);
    fieldChainRepository.save(newFieldChain);

    return newFieldChain;
  }

  public List<Move> getNeighboringMovesWithoutDirection(String id, Move newMove) {

    List<Move> moves = new ArrayList<>();

    List<Move> sameColorMoves = moveRepository.getMovesByIdAndByColor(id, newMove.getColor().name());

    int newMoveValX = newMove.getValXFromName();
    int newMoveValY = newMove.getValYFromName();

    for (Move move : sameColorMoves) {

      int moveValX = move.getValXFromName();
      int moveValY = move.getValYFromName();

      if (moveValX == newMoveValX &&
          (moveValY == newMoveValY + 1 || moveValY == newMoveValY - 1)) {

        moves.add(move);

      } else if (moveValY == newMoveValY &&
          (moveValX == newMoveValX + 1 || moveValX == newMoveValX - 1)) {

        moves.add(move);

      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY + 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY - 1)) {

        moves.add(move);

      } else if ((moveValX == newMoveValX + 1 && moveValY == newMoveValY - 1) ||
          (moveValX == newMoveValX - 1 && moveValY == newMoveValY + 1)) {

        moves.add(move);

      }
    }
    return moves;
  }

  public List<Map<Dir, Move>> getNeighboringSameColorMoves(Move newMove, String id) {

    int newMoveValX = newMove.getValXFromName();
    int newMoveValY = newMove.getValYFromName();
    List<Move> sameColorMoves = moveRepository.getMovesByIdAndByColor(id, newMove.getColor().name());

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

  public Map<Dir, Move> addMoveToMap(Dir dir, Move move) {

    Map<Dir, Move> moves = new HashMap<>();
    moves.put(dir, move);
    return moves;
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
