package org.example.nasaosam.found.service;

import org.example.nasaosam.auth.entity.OneTimeToken;
import org.example.nasaosam.auth.entity.OneTimeTokenPurpose;
import org.example.nasaosam.auth.repo.OneTimeTokenRepository;
import org.example.nasaosam.auth.service.FinderTokenService;
import org.example.nasaosam.auth.service.OneTimeTokenService;
import org.example.nasaosam.found.dto.FinderAccessResponse;
import org.example.nasaosam.found.entity.FoundReport;
import org.example.nasaosam.found.entity.FoundReportStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class FinderAccessService {
    private static final long FINDER_TOKEN_MINUTES = 120;
    private static final long ACTIVE_REPORT_DAYS = 60;
    private final OneTimeTokenRepository oneTimeTokenRepository;
    private final FinderTokenService finderTokenService;

    public FinderAccessService(OneTimeTokenRepository oneTimeTokenRepository, FinderTokenService finderTokenService) {
        this.oneTimeTokenRepository = oneTimeTokenRepository;
        this.finderTokenService = finderTokenService;
    }

    // TOK PODATAKA
    // token iz linka -> SHA hesh -> red u one_time_tokens -> nalaz
    // -> provere -> nalaz postaje ACTIVE -> JWT sa reportId

    @Transactional
    public FinderAccessResponse openLink(String rawToken){
        Instant now;
        String tokenHash;
        Optional<OneTimeToken> foundToken;
        OneTimeToken oneTimeToken;
        FoundReport foundReport;
        Instant activeUntil;
        Instant accessExpiresAt;
        String accessToken;
        now = Instant.now();

        tokenHash = OneTimeTokenService.hash(rawToken.trim());
        foundToken = oneTimeTokenRepository.findWithLockByTokenHash(tokenHash);
        if(foundToken.isEmpty()){
            throw invalidLink();
        }
        oneTimeToken = foundToken.get();

        if(oneTimeToken.getPurpose() != OneTimeTokenPurpose.FINDER_ACCESS){
            throw invalidLink();
        }

        if(!oneTimeToken.isUsable(now)){
            throw invalidLink();
        }

        foundReport = oneTimeToken.getFoundReport();
        if(foundReport.getStatus() != FoundReportStatus.PENDING_EMAIL){
            throw invalidLink();
        }

        //spajanja
        activeUntil = now.plus(ACTIVE_REPORT_DAYS, ChronoUnit.DAYS);
        foundReport.activate(now, activeUntil);
        oneTimeToken.markUsed(now);

        accessExpiresAt = now.plus(FINDER_TOKEN_MINUTES, ChronoUnit.MINUTES);
        accessToken = finderTokenService.createToken(foundReport.getId(), now, accessExpiresAt);

        return new FinderAccessResponse(accessToken, accessExpiresAt);

    }

    private ResponseStatusException invalidLink() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link nije ispravan ili je istekao");
    }
}
