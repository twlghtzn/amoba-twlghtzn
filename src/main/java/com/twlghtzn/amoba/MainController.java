package com.twlghtzn.amoba;

import com.twlghtzn.amoba.dto.GameStartResponse;
import com.twlghtzn.amoba.dto.MoveRequest;
import com.twlghtzn.amoba.dto.MoveResponse;
import com.twlghtzn.amoba.service.AmobaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

  private final AmobaService amobaService;

  @Autowired
  public MainController(AmobaService amobaService) {
    this.amobaService = amobaService;
  }

  @PostMapping("/game")
  public ResponseEntity<GameStartResponse> game() {
    GameStartResponse gameStartResponse = amobaService.startNewGame();
    return ResponseEntity.status(HttpStatus.CREATED).body(gameStartResponse);
  }

  @PutMapping("/move")
  public ResponseEntity<?> move(@RequestBody MoveRequest moveRequest) {
    MoveResponse moveResponse = amobaService.saveMove(moveRequest);
    String status = moveResponse.getStatus();
    if (status.contains("Move saved")) {
      return ResponseEntity.status(HttpStatus.OK).body(moveResponse);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/game/{id}")
  public ResponseEntity<?> board(@PathVariable(name = "id") String id) {
    return ResponseEntity.status(HttpStatus.OK).body(amobaService.generateActualBoard(id));
  }

  @GetMapping("/game/{id}/moves")
  public ResponseEntity<?> moves(@PathVariable(name = "id") String id) {
    return ResponseEntity.status(HttpStatus.OK).body(amobaService.generateMovesDTO(id));
  }
}
