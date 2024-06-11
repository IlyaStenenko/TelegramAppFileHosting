package org.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.AppPhoto;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AppPhotoDAO extends JpaRepository<AppPhoto, Long> {
    List<AppPhoto> findAppPhotoByUserId(Long id);

    List<AppPhoto> findTop5ByPublishedTrueOrderByDownloadsDesc();
    AppPhoto findAppPhotoByPhotoNameAndUserId(String name, Long telegramUserId);
    AppPhoto findAppPhotoByBinaryContentId(Long id);
    @Query(value = "SELECT * FROM app_photo p WHERE p.photo_name LIKE %:keyword% AND p.published=true", nativeQuery = true)
    List<AppPhoto> findByPhotoNameContainingKeyword(String keyword);
}
