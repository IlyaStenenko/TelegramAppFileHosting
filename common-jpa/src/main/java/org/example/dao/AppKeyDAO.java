package org.example.dao;

import org.example.entity.AppKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppKeyDAO extends JpaRepository<AppKey, Long> { }
