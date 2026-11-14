package org.example.nasaosam.auth.entity;

import jakarta.persistence.*;
import org.example.nasaosam.found.entity.FoundReport;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "one_time_tokens")
public class OneTimeToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "purpose", nullable = false, length = 30)
    private OneTimeTokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "found_report_id", nullable = false)
    private FoundReport foundReport;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OneTimeToken() {
    }

    public OneTimeToken(OneTimeTokenPurpose purpose, String tokenHash, FoundReport foundReport, Instant expiresAt) {
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.foundReport = foundReport;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Token se može iskoristiti samo jednom i samo dok ne istekne
    public boolean isUsable(Instant now) {
        if (usedAt != null) {
            return false;
        }
        return expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        if (!isUsable(now)) {
            throw new IllegalStateException("Token je već iskorišćen ili je istekao");
        }
        this.usedAt = now;
    }

    public Long getId() {
        return id;
    }

    public OneTimeTokenPurpose getPurpose() {
        return purpose;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public FoundReport getFoundReport() {
        return foundReport;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

