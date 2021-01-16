package com.twlghtzn.amoba.repository;

import com.twlghtzn.amoba.model.Game;
import com.twlghtzn.amoba.model.Move;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MoveRepository extends CrudRepository<Move, String> {

  @Query(value = "SELECT * FROM move m WHERE game_id = :id", nativeQuery = true)
  List<Move> findAllByGameId(String id);

  @Query(value = "SELECT * FROM move m WHERE game_id = :id AND field = :field", nativeQuery = true)
  Move findMoveByGameIdAndField(String id, String field);

  @Query(value = "SELECT * FROM move m WHERE game_id = :id AND color = :color", nativeQuery = true)
  List<Move> getMovesByIdAndByColor(String id, String color);

  Optional<Move> findMoveByGameAndField(Game game, String field);
}
