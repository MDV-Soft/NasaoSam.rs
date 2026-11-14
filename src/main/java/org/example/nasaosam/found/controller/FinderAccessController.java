package org.example.nasaosam.found.controller;

import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import org.example.nasaosam.found.dto.FinderAccessRequest;
import org.example.nasaosam.found.dto.FinderAccessResponse;
import org.example.nasaosam.found.service.FinderAccessService;
import org.springframework.web.bind.annotation.*;

// otvaranje linka iz mejla
@RestController
@RequestMapping("/api/finder")
public class FinderAccessController {

    private final FinderAccessService finderAccessService;

    public FinderAccessController(FinderAccessService finderAccessService) {
        this.finderAccessService = finderAccessService;
    }

    @PostMapping("/access")
    public FinderAccessResponse openLink(@Valid @RequestBody FinderAccessRequest request) {
        return finderAccessService.openLink(request.token());
    }
}

