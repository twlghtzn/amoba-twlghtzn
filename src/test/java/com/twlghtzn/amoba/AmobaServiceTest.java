package com.twlghtzn.amoba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.model.FieldChain;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.repository.FieldChainRepository;
import com.twlghtzn.amoba.repository.GameRepository;
import com.twlghtzn.amoba.repository.MoveRepository;
import com.twlghtzn.amoba.service.AmobaService;
import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class AmobaServiceTest {

  private final String id = "f6591202-09e6-43a7-b6a3-2792dfe5f31d";
  private Game game;


  @Autowired
  private AmobaService amobaService;

  @Autowired
  private MoveRepository moveRepository;

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private FieldChainRepository fieldChainRepository;

  @BeforeEach
  public void setup() {

    game = new Game(id);
    gameRepository.save(game);

  }

  @Test
  public void whenSaveMove_moveInDB() {

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    amobaService.saveMove(moveRequest);

    Move expectedMove = moveRepository.findMoveByGameIdAndField(id, "3.3");

    int movesCount = gameRepository.findGameById(id).getMoves().size();

    assertEquals("3.3", expectedMove.getField());
    assertEquals(id, expectedMove.getGame().getId());
    assertEquals(Color.BLUE, expectedMove.getColor());
    assertEquals(1, movesCount);
  }

  @Test
  public void whenSaveNeighboringMove_createFieldChain() {

    saveMove("2.2", Color.BLUE);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    amobaService.saveMove(moveRequest);

    List<FieldChain> gamesFieldChains = gameRepository.findGameById(id).getFieldChains();
    FieldChain expectedFieldChain = gamesFieldChains.get(0);

    int movesCount = gameRepository.findGameById(id).getMoves().size();

    assertEquals(1, gamesFieldChains.size());
    assertEquals(2, expectedFieldChain.getMoves().size());
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain,"2.2"));
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain,"3.3"));
    assertEquals(2, movesCount);
  }

  @Test
  public void whenSaveThirdInARow_mergeFieldChains() {

/*    Move move = saveMove("1.1", Color.BLUE);
    Move move1 = saveMove("2.2", Color.BLUE);

    saveFieldChain(move, move1, Dir.DIAG_DOWN); */

    MoveRequest moveRequest1 = new MoveRequest(1, 1, id, "BLUE");
    MoveRequest moveRequest2 = new MoveRequest(0, 0, id, "RED");
    MoveRequest moveRequest3 = new MoveRequest(2, 2, id, "BLUE");
    MoveRequest moveRequest4 = new MoveRequest(2, 0, id, "RED");
    MoveRequest moveRequest5 = new MoveRequest(3, 3, id, "BLUE");

    MoveRequest[] moveRequests = {moveRequest1, moveRequest2, moveRequest3, moveRequest4, moveRequest5};

    for (MoveRequest moveRequest : moveRequests) {

      amobaService.saveMove(moveRequest);

    }

    List<FieldChain> gamesFieldChains = gameRepository.findGameById(id).getFieldChains();
    FieldChain expectedFieldChain = gamesFieldChains.get(0);

    assertEquals(1, gamesFieldChains.size());
    assertEquals(3, expectedFieldChain.getMoves().size());
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain, "1.1"));
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain, "2.2"));
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain, "3.3"));
  }

  @Test
  public void whenSaveDifferentColorMove_noMerge() {

    Move move = saveMove("1.1", Color.RED);
    Move move1 = saveMove("2.2", Color.RED);

    saveFieldChain(move, move1, Dir.DIAG_DOWN);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    amobaService.saveMove(moveRequest);

    List<FieldChain> gamesFieldChains = gameRepository.findGameById(id).getFieldChains();
    FieldChain expectedFieldChain = gamesFieldChains.get(0);

    int movesCount = gameRepository.findGameById(id).getMoves().size();

    assertEquals(1, gamesFieldChains.size());
    assertEquals(2, expectedFieldChain.getMoves().size());
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain, "1.1"));
    assertTrue(checkIfFieldChainContainsMove(expectedFieldChain, "2.2"));
    assertEquals(3, movesCount);
  }

  @Test
  public void whenMoveConnectsTwoExistingFieldChains_MergeIntoOne() {

    String[] fieldsDiagDown = {"1.1", "2.2", "4.4", "5.5"};
    String[] fieldsDiagUp = {"1.5", "2.4", "4.2", "5.1"};

    saveMovesToFieldChains(fieldsDiagDown, Dir.DIAG_DOWN);
    saveMovesToFieldChains(fieldsDiagUp, Dir.DIAG_UP);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    String status = amobaService.saveMove(moveRequest).getStatus();

    List<FieldChain> gamesFieldChains = gameRepository.findGameById(id).getFieldChains();
    FieldChain expectedFieldChain1 = gamesFieldChains.get(0);
    FieldChain expectedFieldChain2 = gamesFieldChains.get(1);


    assertEquals(2, gamesFieldChains.size());
    assertEquals(5, expectedFieldChain1.getMoves().size());
    assertEquals(5, expectedFieldChain2.getMoves().size());
    assertEquals(status, GameState.BLUE_WON.name());
  }

  public Move saveMove(String field, Color color) {

    Move move = new Move(field, color, game);
    moveRepository.save(move);

    game.addMove(move);
    gameRepository.save(game);

    return move;
  }

  public void saveFieldChain(Move move, Move move1, Dir dir) {

    FieldChain newFieldChain = new FieldChain(Color.BLUE, dir, game);

    move.addFieldChain(newFieldChain);
    moveRepository.save(move);
    move1.addFieldChain(newFieldChain);
    moveRepository.save(move1);

    newFieldChain.addMove(move);
    newFieldChain.addMove(move1);
    fieldChainRepository.save(newFieldChain);

    game.addFieldChain(newFieldChain);
    gameRepository.save(game);
  }

  public void saveMovesToFieldChains(String[] fields, Dir dir) {

    List<Move> moves = new ArrayList<>();

    for (String field : fields) {
      Move move = saveMove(field, Color.BLUE);
      moves.add(move);
      if (moves.size() == 2) {
        saveFieldChain(moves.get(0), moves.get(1), dir);
        moves.clear();
      }
    }
  }

  public boolean checkIfFieldChainContainsMove(FieldChain fieldChain, String field) {

    for (Move move : fieldChain.getMoves()) {

      if (move.getField().equals(field)) {
        return true;
      }
    }
    return false;
  }
}
