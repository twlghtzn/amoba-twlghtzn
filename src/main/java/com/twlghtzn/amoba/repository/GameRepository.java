package com.twlghtzn.amoba.repository;

import com.twlghtzn.amoba.model.Game;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GameRepository extends CrudRepository<Game, String> {

  Game findGameById(String id);

  @Query(value = "SELECT id FROM game g", nativeQuery = true)
  List<String> getAllGameIds();
}
