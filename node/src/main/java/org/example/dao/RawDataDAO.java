package org.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.RawData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RawDataDAO extends JpaRepository<RawData, Long> {
    RawData findTopByOrderByIdDesc();

    @Query(value = "SELECT event -> 'message' ->> 'text' FROM raw_data WHERE CAST(event -> 'message' -> 'chat' ->> 'id' AS BIGINT) = :chatId ORDER BY id DESC LIMIT 1", nativeQuery = true)
    String findLastEventTextByChatId(@Param("chatId") Long chatId);
}

