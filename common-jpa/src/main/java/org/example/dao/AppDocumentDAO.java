package org.example.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.AppDocument;

public interface AppDocumentDAO extends JpaRepository<AppDocument, Long> {
}
