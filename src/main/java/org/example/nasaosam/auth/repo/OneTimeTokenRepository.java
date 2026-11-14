package org.example.nasaosam.auth.repo;

import org.example.nasaosam.auth.entity.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    Optional<OneTimeToken> findByTokenHash(String tokenHash);
}
