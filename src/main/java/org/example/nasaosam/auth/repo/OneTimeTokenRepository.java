package org.example.nasaosam.auth.repo;

import jakarta.persistence.LockModeType;
import org.example.nasaosam.auth.entity.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OneTimeToken> findWithLockByTokenHash(String tokenHash);
}
