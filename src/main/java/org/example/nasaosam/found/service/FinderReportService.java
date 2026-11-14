package org.example.nasaosam.found.service;

// STA NALAZAAC VIDI SA SVOJIM NALAZOM : pregled, promena gde je predmet, povlacenje

import org.example.nasaosam.found.dto.FinderReportItemResponse;
import org.example.nasaosam.found.dto.FinderReportResponse;
import org.example.nasaosam.found.dto.UpdateItemLocationRequest;
import org.example.nasaosam.found.entity.FoundReport;
import org.example.nasaosam.found.entity.FoundReportItem;
import org.example.nasaosam.found.entity.FoundReportStatus;
import org.example.nasaosam.found.entity.ItemLocation;
import org.example.nasaosam.found.repo.FoundReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinderReportService {
    private final FoundReportRepository foundReportRepository;

    public FinderReportService(FoundReportRepository foundReportRepository) {
        this.foundReportRepository = foundReportRepository;
    }

    //pregled
    @Transactional
    public FinderReportResponse getReport(Long reportId){
        FoundReport report;
        report = loadReport(reportId);
        return toResponse(report);
    }

    //update item loc
    @Transactional
    public FinderReportResponse updateItemLocation (Long reportId, UpdateItemLocationRequest req){
        FoundReport report;
        String handoverInfo;

        report = loadReport(reportId);
        requireActive(report);

        handoverInfo = trimToNull(req.handoverInfo());
        if (req.itemLocation() != ItemLocation.WITH_FINDER && handoverInfo == null) {
            throw badRequest("Navedi gde vlasnik može da preuzme predmet (adresa, šalter, radno vreme)");
        }
        if (req.itemLocation() == ItemLocation.WITH_FINDER && handoverInfo != null) {
            throw badRequest("Podaci o predaji se unose samo kad predmet nije kod tebe");
        }

        report.setItemLocation(req.itemLocation());
        report.setHandoverInfo(handoverInfo);
        return toResponse(report);
    }


    // "Predmet vise nije kod mene"
    @Transactional
    public FinderReportResponse withdraw(Long reportId) {
        FoundReport report;

        report = loadReport(reportId);
        requireActive(report);

        report.withdraw(Instant.now());
        return toResponse(report);
    }

    // load report
    private FoundReport loadReport(Long reportId) {
        return foundReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nalaz nije pronađen"));
    }

    private void requireActive(FoundReport report) {
        if (report.getStatus() != FoundReportStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nalaz više nije aktivan");
        }
    }

    //nroamalan odgovor
    private FinderReportResponse toResponse(FoundReport report) {
        List<FinderReportItemResponse> items;
        FoundReportItem item;

        items = new ArrayList<>();
        for (int index = 0; index < report.getItems().size(); index++) {
            item = report.getItems().get(index);
            items.add(new FinderReportItemResponse(
                    item.getCategory(),
                    item.getCustomCategory(),
                    item.getDocumentType(),
                    item.getColor(),
                    item.getBrand(),
                    item.getMaterial(),
                    item.getDescription(),
                    item.getDistinctiveMarks()
            ));
        }

        return new FinderReportResponse(
                report.getStatus(),
                report.getFoundAt(),
                report.getCity(),
                report.getPlaceDescription(),
                report.getLatitude(),
                report.getLongitude(),
                report.getItemLocation(),
                report.getHandoverInfo(),
                report.getControlQuestion(),
                report.getActivatedAt(),
                report.getExpiresAt(),
                items
        );
    }

    private String trimToNull(String value) {
        String trimmed;

        if (value == null) {
            return null;
        }
        trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
