package org.example.nasaosam.found.service;

import org.example.nasaosam.auth.entity.OneTimeTokenPurpose;
import org.example.nasaosam.auth.service.OneTimeTokenService;
import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.security.HmacService;
import org.example.nasaosam.common.security.PhoneNumberNormalizer;
import org.example.nasaosam.found.dto.CreateFoundReportRequest;
import org.example.nasaosam.found.dto.FoundReportItemRequest;
import org.example.nasaosam.found.dto.FoundReportSubmittedResponse;
import org.example.nasaosam.found.entity.FoundReport;
import org.example.nasaosam.found.entity.FoundReportItem;
import org.example.nasaosam.found.entity.FoundReportSource;
import org.example.nasaosam.found.entity.ItemLocation;
import org.example.nasaosam.found.repo.FoundReportRepository;
import org.example.nasaosam.notification.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static org.apache.logging.log4j.util.Strings.trimToNull;
import static org.springframework.web.servlet.function.ServerResponse.badRequest;

@Service
public class FoundReportService {
    private static final long PENDING_HOURS = 24;

    // Predmet nađen pre više od mesec dana nema smisla prijavljivati kao nov nalaz
    private static final long MAX_PAST_DAYS = 30;

    // Tolerancija za razliku između sata na telefonu i na serveru
    private static final long FUTURE_TOLERANCE_MINUTES = 5;

    private final FoundReportRepository foundReportRepository;
    private final HmacService hmacService;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final OneTimeTokenService oneTimeTokenService;
    private final EmailService emailService;
    private final String finderLinkBaseUrl;

    public FoundReportService(FoundReportRepository foundReportRepository,
                              HmacService hmacService,
                              PhoneNumberNormalizer phoneNumberNormalizer,
                              OneTimeTokenService oneTimeTokenService,
                              EmailService emailService,
                              @Value("${app.finder-link-base-url}") String finderLinkBaseUrl) {
        this.foundReportRepository = foundReportRepository;
        this.hmacService = hmacService;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.oneTimeTokenService = oneTimeTokenService;
        this.emailService = emailService;
        this.finderLinkBaseUrl = finderLinkBaseUrl;
    }
    // ----------------- TODO --------------
    // PRijava nalaza : provere - pravljenje nalaza i predmeta - cuvanje - token - slanje linka
    @Transactional
    public FoundReportSubmittedResponse create(CreateFoundReportRequest request) {
        Instant now;
        Instant linkExpiresAt;
        String email;
        String phone;
        FoundReport report;
        FoundReport savedReport;
        String rawToken;
        String link;
        List<FoundReportItemRequest> itemRequests;

        now = Instant.now();
        itemRequests = request.items();

        // 1. PROVERE
        validateReport(request, now);
        for (int index = 0; index < itemRequests.size(); index++) {
            validateItem(itemRequests.get(index), index + 1);
        }

        // 2. KONTAKT: sve u jedan oblik (mala slova, +381...)
        email = normalizeEmail(request.finderEmail());
        phone = null;
        if (trimToNull(request.finderPhone()) != null) {
            phone = phoneNumberNormalizer.normalize(request.finderPhone());
        }

        // 3. NALAZ
        linkExpiresAt = now.plus(PENDING_HOURS, ChronoUnit.HOURS);
        report = new FoundReport(
                FoundReportSource.CITIZEN,
                email,
                request.foundAt(),
                request.city().trim(),
                request.latitude(),
                request.longitude(),
                request.itemLocation(),
                request.controlQuestion().trim(),
                request.controlAnswer().trim(),
                linkExpiresAt
        );
        report.setPlaceDescription(trimToNull(request.placeDescription()));
        report.setHandoverInfo(trimToNull(request.handoverInfo()));

        report.setShareEmail(true);
        if (phone != null) {
            report.setFinderPhone(phone);
            report.setSharePhone(true);
        }

        for (int index = 0; index < itemRequests.size(); index++) {
            report.addItem(buildItem(itemRequests.get(index)));
        }

        // 4. CUAVNJEn
        savedReport = foundReportRepository.save(report);
        rawToken = oneTimeTokenService.issue(OneTimeTokenPurpose.FINDER_ACCESS, savedReport, linkExpiresAt);

        // 5. LINK.
        link = finderLinkBaseUrl + "?token=" + rawToken;
        emailService.sendFinderAccessLink(email, link);

        return new FoundReportSubmittedResponse(
                "Nalaz je primljen. Na mejl smo poslali link; potvrdi ga da bi nalaz postao aktivan.",
                linkExpiresAt
        );
    }

    // provera
    private void validateReport(CreateFoundReportRequest request, Instant now) {
        String handoverInfo;

        if (request.items() == null || request.items().isEmpty()) {
            throw badRequest("Nalaz mora imati bar jedan predmet");
        }

        // Nalaz nije u budućnosti i nije star
        if (request.foundAt().isAfter(now.plus(FUTURE_TOLERANCE_MINUTES, ChronoUnit.MINUTES))) {
            throw badRequest("Vreme nalaska ne može biti u budućnosti");
        }
        if (request.foundAt().isBefore(now.minus(MAX_PAST_DAYS, ChronoUnit.DAYS))) {
            throw badRequest("Predmet ne može biti pronađen pre više od " + MAX_PAST_DAYS + " dana");
        }

        // Tačka na mapi je obavezna (entitet i baza je traže, pa bi inače stigla greška 500)
        if (request.latitude() == null || request.longitude() == null) {
            throw badRequest("Tačka na mapi je obavezna");
        }

        handoverInfo = trimToNull(request.handoverInfo());
        if (request.itemLocation() != ItemLocation.WITH_FINDER && handoverInfo == null) {
            throw badRequest("Navedi gde vlasnik može da preuzme predmet (adresa, šalter, radno vreme)");
        }
        if (request.itemLocation() == ItemLocation.WITH_FINDER && handoverInfo != null) {
            throw badRequest("Podaci o predaji se unose samo kad predmet nije kod tebe");
        }
    }


    private void validateItem(FoundReportItemRequest item, int itemNumber) {
        String prefix;
        boolean isDocument;
        boolean isOther;
        String customCategory;

        prefix = "Predmet " + itemNumber + ": ";
        isDocument = item.category() == ItemCategory.DOCUMENT;
        isOther = item.category() == ItemCategory.OTHER;
        customCategory = trimToNull(item.customCategory());

        if (isOther && customCategory == null) {
            throw badRequest(prefix + "za vrstu \"Ostalo\" upiši šta je predmet");
        }
        if (!isOther && customCategory != null) {
            throw badRequest(prefix + "naziv predmeta se upisuje samo za vrstu \"Ostalo\"");
        }

        if (isDocument && item.documentType() == null) {
            throw badRequest(prefix + "izaberi vrstu dokumenta");
        }
        if (!isDocument && item.documentType() != null) {
            throw badRequest(prefix + "vrsta dokumenta se bira samo za dokumenta");
        }

        // Ime se unosi samo za dokumenta; za ostale predmete nema više šta da se proverava
        if (!isDocument) {
            if (item.nameHash() != null || item.nameDobHash() != null) {
                throw badRequest(prefix + "ime se unosi samo za dokumenta");
            }
            return;
        }

        if (item.nameDobHash() != null && item.nameHash() == null) {
            throw badRequest(prefix + "nedostaje heš imena");
        }
        if (item.documentType() == DocumentType.BANK_CARD && item.nameDobHash() != null) {
            throw badRequest(prefix + "platna kartica nema datum rođenja");
        }
    }

    // -------------- HELPERI -----------
    private FoundReportItem buildItem(FoundReportItemRequest request) {
        FoundReportItem item;

        item = new FoundReportItem(request.category());
        item.setCustomCategory(trimToNull(request.customCategory()));
        item.setDocumentType(request.documentType());
        item.setColor(request.color());
        item.setBrand(trimToNull(request.brand()));
        item.setMaterial(trimToNull(request.material()));
        item.setDescription(trimToNull(request.description()));
        item.setDistinctiveMarks(trimToNull(request.distinctiveMarks()));

        // SHA-256 sa telefona -> HMAC za bazu; HmacService odbija sve što nije ispravan heš
        if (request.nameHash() != null) {
            item.setNameHash(hmacService.hashClientHash(request.nameHash()));
        }
        if (request.nameDobHash() != null) {
            item.setNameDobHash(hmacService.hashClientHash(request.nameDobHash()));
        }
        return item;
    }

    // "  Nalazac@Test.RS " i "nalazac@test.rs" su ista adresa
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // "" i "   " postaju null, ostalo se čisti od razmaka na krajevima
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
