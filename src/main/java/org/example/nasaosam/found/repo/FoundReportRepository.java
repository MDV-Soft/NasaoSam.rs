package org.example.nasaosam.found.repo;

import org.example.nasaosam.found.entity.FoundReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoundReportRepository extends JpaRepository<FoundReport, Long> {
}
