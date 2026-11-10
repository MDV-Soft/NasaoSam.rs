package org.example.nasaosam.lost.controller;

import jakarta.validation.Valid;
import org.example.nasaosam.lost.dto.CreateLostReportRequest;
import org.example.nasaosam.lost.dto.LostReportResponse;
import org.example.nasaosam.lost.service.LostReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
/*
*
* API endpoint za izgubljene oglase
*
* */

@RestController
@RequestMapping("/api/lost-reports")
public class LostReportController {
    private final LostReportService lostReportService;

    public LostReportController(LostReportService lostReportService){
        this.lostReportService = lostReportService;
    }

    //NOV OGLASS
    @PostMapping
    public ResponseEntity<LostReportResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateLostReportRequest request){
        LostReportResponse response;
        URI location;
        response = lostReportService.create(currentUserId(jwt), request);
        location = URI.create("api/lost-reports/" + response.id());
    return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<LostReportResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return lostReportService.listMine(currentUserId(jwt));
    }

    // GET MOJI OGLASI
    @GetMapping("/{id}")
    public LostReportResponse getMine(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long reportId){
        return lostReportService.getMine(currentUserId(jwt), reportId);
    }

    /**
     * Izmena oglasa
     */
    @PutMapping("/{id}")
    public LostReportResponse update(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable("id") Long reportId,
                                     @Valid @RequestBody CreateLostReportRequest request) {
        return lostReportService.update(currentUserId(jwt), reportId, request);
    }

    /**
     * Zatvaranje oglasa
     */
    @PostMapping("/{id}/close")
    public LostReportResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long reportId) {
        return lostReportService.close(currentUserId(jwt), reportId);
    }

    private Long currentUserId(Jwt jwt){
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
    }
}
