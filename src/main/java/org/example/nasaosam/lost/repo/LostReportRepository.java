package org.example.nasaosam.lost.repo;

import org.example.nasaosam.lost.entity.LostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LostReportRepository extends JpaRepository<LostReport, Long> {

    // "Moji oglasi", najnoviji prvi
    List<LostReport> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    // Oglas se traži zajedno sa vlasnikom: tuđi oglas izgleda kao da ne postoji (404, ne 403)
    Optional<LostReport> findByIdAndOwnerId(Long id, Long ownerId);
}

