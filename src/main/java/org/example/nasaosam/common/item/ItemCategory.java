package org.example.nasaosam.common.item;

/**
 * Vrste predmeta
 * Ime konstante za u bazu, label  za prikaz i AI.
 */
public enum ItemCategory {

    WALLET("Novčanik"),
    DOCUMENT("Dokument"),
    KEYS("Ključevi"),
    PHONE("Telefon"),
    BAG("Torba ili ranac"),
    OTHER("Ostalo");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}