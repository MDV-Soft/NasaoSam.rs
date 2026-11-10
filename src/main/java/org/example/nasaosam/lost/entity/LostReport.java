package org.example.nasaosam.lost.entity;

import jakarta.persistence.*;
import org.example.nasaosam.common.security.EncryptedStringConverter;
import org.example.nasaosam.lost.repo.LostReportRepository;
import org.example.nasaosam.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lost_reports")
public class LostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // korisnik se ucitava iz baze tek kad zatreba
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="status", nullable = false, length = 20)
    private LostReportStatus status;

    @Column(name = "lost_from", nullable = false)
    private Instant lostFrom;

    @Column(name = "lost_to", nullable = false)
    private Instant lostTo;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "place_description", length = 255)
    private String placeDescription;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "location_radius_meters")
    private Integer locationRadiusMeters;

    //privateni podaci tjst detaljniji opsi (AES sifrovano) - sifruje se PRE uipsa u bazu
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "private_details", columnDefinition = "TEXT")
    private String privateDetails;

    // SCORE ZA MATCHOVANJE
    @Column(name = "completeness_score", nullable = false)
    private int completenessScore;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // kaskadno se cuvaju predemeti kada se sacuva i oglas
    @OneToMany(mappedBy = "lostReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LostReportItem> items = new ArrayList<>();

    public LostReport() {
    }

    public LostReport(User owner, Instant lostFrom, Instant lostTo, String city, Instant expiresAt) {
        this.owner = owner;
        this.lostFrom = lostFrom;
        this.lostTo = lostTo;
        this.city = city;
        this.expiresAt = expiresAt;
        this.status = LostReportStatus.ACTIVE;
        this.completenessScore = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(LostReportItem item){
        items.add(item);
        item.setLostReport(this);
    }

    public void removeItem(LostReportItem item)
    {
        items.remove(item);
        item.setLostReport(null);
    }

    public void close(){
        if (status != LostReportStatus.ACTIVE) {
            throw new IllegalStateException("Može da se zatvori samo aktivan oglas");
        }
        this.status = LostReportStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void markReturned() {
        if (status != LostReportStatus.ACTIVE) {
            throw new IllegalStateException("Kao vraćen može da se označi samo aktivan oglas");
        }
        this.status = LostReportStatus.RETURNED;
        this.closedAt = Instant.now();
    }

    public void markExpired() {
        if (status != LostReportStatus.ACTIVE) {
            throw new IllegalStateException("Može da istekne samo aktivan oglas");
        }
        this.status = LostReportStatus.EXPIRED;
        this.closedAt = Instant.now();
    }

    public boolean isActive() {
        return status == LostReportStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LostReportStatus getStatus() {
        return status;
    }

    public void setStatus(LostReportStatus status) {
        this.status = status;
    }

    public Instant getLostFrom() {
        return lostFrom;
    }

    public void setLostFrom(Instant lostFrom) {
        this.lostFrom = lostFrom;
    }

    public Instant getLostTo() {
        return lostTo;
    }

    public void setLostTo(Instant lostTo) {
        this.lostTo = lostTo;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPlaceDescription() {
        return placeDescription;
    }

    public void setPlaceDescription(String placeDescription) {
        this.placeDescription = placeDescription;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getLocationRadiusMeters() {
        return locationRadiusMeters;
    }

    public void setLocationRadiusMeters(Integer locationRadiusMeters) {
        this.locationRadiusMeters = locationRadiusMeters;
    }

    public String getPrivateDetails() {
        return privateDetails;
    }

    public void setPrivateDetails(String privateDetails) {
        this.privateDetails = privateDetails;
    }

    public int getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(int completenessScore) {
        this.completenessScore = completenessScore;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<LostReportItem> getItems() {
        return items;
    }

    public void setItems(List<LostReportItem> items) {
        this.items = items;
    }
}
