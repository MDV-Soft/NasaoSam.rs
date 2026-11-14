package org.example.nasaosam.found.entity;

import jakarta.persistence.*;
import org.example.nasaosam.common.security.EncryptedStringConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="found_reports")
public class FoundReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="source", nullable = false, length = 20)
    private FoundReportSource source;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private FoundReportStatus status;

    // U bazi šifrovano (AES), u Javi običan tekst
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "finder_email", nullable = false, columnDefinition = "TEXT")
    private String finderEmail;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "finder_phone", columnDefinition = "TEXT")
    private String finderPhone;

    @Column(name = "share_email", nullable = false)
    private boolean shareEmail = true;

    @Column(name = "share_phone", nullable = false)
    private boolean sharePhone = false;

    @Column(name = "found_at", nullable = false)
    private Instant foundAt;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "place_description", length = 255)
    private String placeDescription;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "item_location", nullable = false, length = 20)
    private ItemLocation itemLocation;

    // Adresa, šalter i radno vreme; obavezno kad predmet nije kod nalazača
    @Column(name = "handover_info", length = 255)
    private String handoverInfo;

    // Pitanje vidi vlasnik, a odgovor je tajan
    @Column(name = "control_question", nullable = false, length = 255)
    private String controlQuestion;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "control_answer", nullable = false, columnDefinition = "TEXT")
    private String controlAnswer;

    @Column(name = "blurred_photo_key", length = 255)
    private String blurredPhotoKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "foundReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoundReportItem> items = new ArrayList<>();

    public FoundReport() {
    }

    public FoundReport(FoundReportSource source, String finderEmail, Instant foundAt, String city,
                       BigDecimal latitude, BigDecimal longitude, ItemLocation itemLocation,
                       String controlQuestion, String controlAnswer, Instant expiresAt) {
        this.source = source;
        this.finderEmail = finderEmail;
        this.foundAt = foundAt;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.itemLocation = itemLocation;
        this.controlQuestion = controlQuestion;
        this.controlAnswer = controlAnswer;
        this.expiresAt = expiresAt;
        this.status = FoundReportStatus.PENDING_EMAIL;
    }

    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }

    public void addItem(FoundReportItem item) {
        items.add(item);
        item.setFoundReport(this);
    }

    public void removeItem(FoundReportItem item) {
        items.remove(item);
        item.setFoundReport(null);
    }

    // PENDING_EMAIL -> ACTIVE: nalazac je otvorio link
    public void activate(Instant now, Instant newExpiresAt) {
        if (status != FoundReportStatus.PENDING_EMAIL) {
            throw new IllegalStateException("Može da se aktivira samo nalaz koji čeka potvrdu mejla");
        }
        this.status = FoundReportStatus.ACTIVE;
        this.activatedAt = now;
        this.expiresAt = newExpiresAt;
    }

    // Nalazac povlaci naalaz
    public void withdraw(Instant now) {
        if (status != FoundReportStatus.PENDING_EMAIL && status != FoundReportStatus.ACTIVE) {
            throw new IllegalStateException("Može da se povuče samo nalaz koji još nije završen");
        }
        this.status = FoundReportStatus.WITHDRAWN;
        this.closedAt = now;
    }

    public void markReturned(Instant now){
        if(status != FoundReportStatus.ACTIVE){
            throw new IllegalStateException("Kao vracen moze da se oznaci samo aktivan oglas");
        }
        this.status = FoundReportStatus.RETURNED;
        this.closedAt = now;
    }

    public void markExpired(Instant now) {
        if (status != FoundReportStatus.PENDING_EMAIL && status != FoundReportStatus.ACTIVE) {
            throw new IllegalStateException("Može da istekne samo nalaz koji još nije završen");
        }
        this.status = FoundReportStatus.EXPIRED;
        this.closedAt = now;
    }

    public boolean isActive(){
        return status == FoundReportStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FoundReportSource getSource() {
        return source;
    }

    public void setSource(FoundReportSource source) {
        this.source = source;
    }

    public FoundReportStatus getStatus() {
        return status;
    }

    public void setStatus(FoundReportStatus status) {
        this.status = status;
    }

    public String getFinderEmail() {
        return finderEmail;
    }

    public void setFinderEmail(String finderEmail) {
        this.finderEmail = finderEmail;
    }

    public String getFinderPhone() {
        return finderPhone;
    }

    public void setFinderPhone(String finderPhone) {
        this.finderPhone = finderPhone;
    }

    public boolean isShareEmail() {
        return shareEmail;
    }

    public void setShareEmail(boolean shareEmail) {
        this.shareEmail = shareEmail;
    }

    public boolean isSharePhone() {
        return sharePhone;
    }

    public void setSharePhone(boolean sharePhone) {
        this.sharePhone = sharePhone;
    }

    public Instant getFoundAt() {
        return foundAt;
    }

    public void setFoundAt(Instant foundAt) {
        this.foundAt = foundAt;
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

    public ItemLocation getItemLocation() {
        return itemLocation;
    }

    public void setItemLocation(ItemLocation itemLocation) {
        this.itemLocation = itemLocation;
    }

    public String getHandoverInfo() {
        return handoverInfo;
    }

    public void setHandoverInfo(String handoverInfo) {
        this.handoverInfo = handoverInfo;
    }

    public String getControlQuestion() {
        return controlQuestion;
    }

    public void setControlQuestion(String controlQuestion) {
        this.controlQuestion = controlQuestion;
    }

    public String getControlAnswer() {
        return controlAnswer;
    }

    public void setControlAnswer(String controlAnswer) {
        this.controlAnswer = controlAnswer;
    }

    public String getBlurredPhotoKey() {
        return blurredPhotoKey;
    }

    public void setBlurredPhotoKey(String blurredPhotoKey) {
        this.blurredPhotoKey = blurredPhotoKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
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

    public List<FoundReportItem> getItems() {
        return items;
    }

    public void setItems(List<FoundReportItem> items) {
        this.items = items;
    }
}
