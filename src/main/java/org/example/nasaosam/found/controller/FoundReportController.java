package org.example.nasaosam.found.controller;

import jakarta.validation.Valid;
import org.example.nasaosam.found.dto.CreateFoundReportRequest;
import org.example.nasaosam.found.dto.FoundReportSubmittedResponse;
import org.example.nasaosam.found.service.FoundReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/found-reports")
public class FoundReportController {

    private final FoundReportService foundReportService;

    public FoundReportController(FoundReportService foundReportService) {
        this.foundReportService = foundReportService;
    }

    @PostMapping
    public ResponseEntity<FoundReportSubmittedResponse> create(@Valid @RequestBody CreateFoundReportRequest request) {
        FoundReportSubmittedResponse response;

        response = foundReportService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}