package com.twlghtzn.amoba.repository;

import com.twlghtzn.amoba.model.Move;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MoveRepository extends CrudRepository<Move, String> {

  @Query(value = "SELECT * FROM move m WHERE game_id = :id", nativeQuery = true)
  List<Move> findAllByGameId(String id);

  @Query(value = "SELECT * FROM move m WHERE game_id = :id AND field = :field", nativeQuery = true)
  Move findMoveByGameIdAndField(String id, String field);
}
