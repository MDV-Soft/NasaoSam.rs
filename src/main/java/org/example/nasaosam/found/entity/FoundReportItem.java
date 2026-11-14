package org.example.nasaosam.found.entity;

import jakarta.persistence.*;
import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "found_report_items")
public class FoundReportItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "found_report_id", nullable = false)
    private FoundReport foundReport;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 30)
    private ItemCategory category;

    @Column(name = "custom_category", length = 50)
    private String customCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "document_type", length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "color", length = 20)
    private ItemColor color;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "material", length = 50)
    private String material;

    // Neobavezan
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "distinctive_marks", length = 500)
    private String distinctiveMarks;

    // HMAC
    @Column(name = "name_hash", columnDefinition = "CHAR(64)")
    private String nameHash;

    @Column(name = "name_dob_hash", columnDefinition = "CHAR(64)")
    private String nameDobHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected FoundReportItem() {
    }

    public FoundReportItem(ItemCategory category) {
        this.category = category;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public FoundReport getFoundReport() {
        return foundReport;
    }

    void setFoundReport(FoundReport foundReport) {
        this.foundReport = foundReport;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public ItemColor getColor() {
        return color;
    }

    public void setColor(ItemColor color) {
        this.color = color;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDistinctiveMarks() {
        return distinctiveMarks;
    }

    public void setDistinctiveMarks(String distinctiveMarks) {
        this.distinctiveMarks = distinctiveMarks;
    }

    public String getNameHash() {
        return nameHash;
    }

    public void setNameHash(String nameHash) {
        this.nameHash = nameHash;
    }

    public String getNameDobHash() {
        return nameDobHash;
    }

    public void setNameDobHash(String nameDobHash) {
        this.nameDobHash = nameDobHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
