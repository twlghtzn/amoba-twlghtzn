package com.twlghtzn.amoba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import com.twlghtzn.amoba.repository.GameRepository;
import com.twlghtzn.amoba.repository.MoveRepository;
import com.twlghtzn.amoba.service.AmobaService;
import com.twlghtzn.amoba.util.Color;
import com.twlghtzn.amoba.util.Dir;
import com.twlghtzn.amoba.util.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    Game game = gameRepository.findGameById(id);
    List<Map<String, String>> parents = game.getParents();

    assertEquals("3.3", expectedMove.getField());
    assertEquals(id, expectedMove.getGame().getId());
    assertEquals(Color.BLUE, expectedMove.getColor());
    for (Map<String, String> connections : parents) {
      assertTrue(connections.containsKey("3.3"));
    }
    assertEquals(GameState.RED_NEXT, game.getGameState());
  }

  @Test
  public void whenSaveNeighboringMove_registerParent() {
    saveMove("1.1", Color.BLUE);
    saveMove("2.2", Color.BLUE);
    game.addConnection(Dir.DIAG_DOWN, "2.2", "1.1");
    gameRepository.save(game);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    amobaService.saveMove(moveRequest);

    Map<String, String> diagDownParents = gameRepository.findGameById(id).getDiagDownParents();

    assertEquals(3, diagDownParents.size());
    assertEquals("1.1", diagDownParents.get("2.2"));
    assertEquals("1.1", diagDownParents.get("3.3"));
  }

  @Test
  public void whenSaveDifferentColorMove_noMerge() {
    saveMove("1.1", Color.BLUE);
    saveMove("2.2", Color.BLUE);
    game.addConnection(Dir.DIAG_DOWN, "2.2", "1.1");
    game.setGameState(GameState.RED_NEXT);
    gameRepository.save(game);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "RED");

    amobaService.saveMove(moveRequest);

    List<Move> moves = gameRepository.findGameById(id).getMoves();
    Map<String, String> diagDownParents = gameRepository.findGameById(id).getDiagDownParents();

    assertEquals(3, moves.size());
    assertTrue(diagDownParents.containsKey("2.2"));
    assertTrue(diagDownParents.containsKey("2.2"));
    assertTrue(diagDownParents.containsKey("3.3"));
    assertNotEquals("1.1", diagDownParents.get("3.3"));
  }

  @Test
  public void whenMoveConnectsTwoExistingFieldChains_MergeIntoOne() {

    String[] fieldsDiagDown = {"1.1", "2.2", "4.4", "5.5"};
    for (String field : fieldsDiagDown) {
      saveMove(field, Color.BLUE);
    }
    game.addConnection(Dir.DIAG_DOWN, "2.2", "1.1");
    game.addConnection(Dir.DIAG_DOWN, "5.5", "4.4");
    gameRepository.save(game);

    MoveRequest moveRequest = new MoveRequest(3, 3, id, "BLUE");

    amobaService.saveMove(moveRequest);

    Game game = gameRepository.findGameById(id);
    Map<String, String> diagDownParents = game.getDiagDownParents();
    GameState status = game.getGameState();

    assertEquals(5, diagDownParents.size());
    for (String field : fieldsDiagDown) {
      assertTrue(diagDownParents.containsKey(field));
    }
    assertTrue(diagDownParents.containsKey("3.3"));
    for (Map.Entry<String, String> connections : diagDownParents.entrySet()) {
      assertEquals("1.1", connections.getValue());
    }
    assertEquals(GameState.BLUE_WON, status);
  }

  public Move saveMove(String field, Color color) {

    Move move = new Move(field, color, game);
    moveRepository.save(move);
    game.addMove(move);
    for (Dir dir : Dir.values()) {
      game.addConnection(dir, field, field);
    }
    gameRepository.save(game);
    return move;
  }
}
