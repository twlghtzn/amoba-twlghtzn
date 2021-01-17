package com.twlghtzn.amoba.controller;

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
  public ResponseEntity<GameStartResponse> start() {
    GameStartResponse gameStartResponse = amobaService.startNewGame();
    return ResponseEntity.status(HttpStatus.CREATED).body(gameStartResponse);
  }

  @PutMapping("/game/{id}")
  public ResponseEntity<?> move(@PathVariable(name = "id") String id,
                                @RequestBody MoveRequest moveRequest) {
    MoveResponse moveResponse = amobaService.saveMove(moveRequest, id);
    return ResponseEntity.status(HttpStatus.OK).body(moveResponse);
  }

  @GetMapping("/game/{id}")
  public ResponseEntity<?> moves(@PathVariable(name = "id") String id) {
    return ResponseEntity.status(HttpStatus.OK).body(amobaService.generateMovesView(id));
  }
}
