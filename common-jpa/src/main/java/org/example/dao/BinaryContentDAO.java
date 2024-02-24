package org.example.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.BinaryContent;

public interface BinaryContentDAO extends JpaRepository<BinaryContent, Long> {
}
