package org.example.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.AppPhoto;

public interface AppPhotoDAO extends JpaRepository<AppPhoto, Long> {
}
