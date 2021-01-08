package com.twlghtzn.amoba.repository;

import com.twlghtzn.amoba.model.FieldChain;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FieldChainRepository extends CrudRepository<FieldChain, Long> {

  @Query(value = "SELECT * FROM field_chain f WHERE game_id = :id", nativeQuery = true)
  List<FieldChain> findAllByGameId(String id);
}
