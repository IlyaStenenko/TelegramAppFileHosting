package org.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.AppUser;
import java.util.Optional;

public interface AppUserDAO extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTelegramUserId(Long id);
    Optional<AppUser> findById(Long id);
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findFirstByTelegramUserId(Long id);

    Optional<AppUser> findFirstById(Long userId);

    Optional<AppUser> findAppUserByUsername(String username);
}
