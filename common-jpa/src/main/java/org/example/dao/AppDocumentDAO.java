package org.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.AppDocument;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AppDocumentDAO extends JpaRepository<AppDocument, Long> {
    List<AppDocument> findAppDocumentByUserId(Long id);
    List<AppDocument> findTop5ByPublishedTrueOrderByDownloadsDesc();
    AppDocument findAppDocumentByDocNameAndUserId(String name, Long telegramUserId);
    AppDocument findAppDocumentByBinaryContentId(Long id);
    @Query(value = "SELECT * FROM app_document p WHERE p.doc_name LIKE %:keyword% AND p.published=true", nativeQuery = true)
    List<AppDocument> findByDocNameContainingKeyword(String keyword);
}
