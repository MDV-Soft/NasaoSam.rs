package org.example.nasaosam.common.item;

/**
 * Podvrsta za predmete vrste DOCUMENT
 * Broj kartice, JMBG i broj dokumenta se nikad ne cuvaju.
 */
public enum DocumentType {

    ID_CARD("Lična karta"),
    DRIVER_LICENSE("Vozačka dozvola"),
    VEHICLE_REGISTRATION("Saobraćajna dozvola"),
    PASSPORT("Pasoš"),
    BANK_CARD("Platna kartica"),
    HEALTH_CARD("Zdravstvena knjižica"),
    OTHER("Ostalo");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}