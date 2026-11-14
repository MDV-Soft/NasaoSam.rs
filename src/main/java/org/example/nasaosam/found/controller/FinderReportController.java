package org.example.nasaosam.found.controller;

import jakarta.validation.Valid;
import org.example.nasaosam.auth.service.FinderTokenService;
import org.example.nasaosam.found.dto.FinderReportResponse;
import org.example.nasaosam.found.dto.UpdateItemLocationRequest;
import org.example.nasaosam.found.service.FinderReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/finder/report")
public class FinderReportController {
    private final FinderReportService finderReportService;

    public FinderReportController(FinderReportService finderReportService) {
        this.finderReportService = finderReportService;
    }

    @GetMapping
    public FinderReportResponse getReport(@AuthenticationPrincipal Jwt jwt) {
        return finderReportService.getReport(currentReportId(jwt));
    }

    // PUT jer se
    @PutMapping("/item-location")
    public FinderReportResponse updateItemLocation(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody UpdateItemLocationRequest request) {
        return finderReportService.updateItemLocation(currentReportId(jwt), request);
    }

    // POST na /withdraw,
    @PostMapping("/withdraw")
    public FinderReportResponse withdraw(@AuthenticationPrincipal Jwt jwt) {
        return finderReportService.withdraw(currentReportId(jwt));
    }

    // ID nalaza iz tokena
    private Long currentReportId(Jwt jwt) {
        Number reportId;

        reportId = jwt.getClaim(FinderTokenService.REPORT_ID_CLAIM);
        if (reportId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token nije za nalazača");
        }
        return reportId.longValue();
    }
}
