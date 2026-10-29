package org.example.nasaosam.common.item;

/**
 * Fiksna lista boja i vlasnik i AI biraju iz istog skupa boja
 */
public enum ItemColor {

    BLACK("Crna"),
    WHITE("Bela"),
    GRAY("Siva"),
    BROWN("Braon"),
    BEIGE("Bež"),
    RED("Crvena"),
    PINK("Roze"),
    ORANGE("Narandžasta"),
    YELLOW("Žuta"),
    GREEN("Zelena"),
    BLUE("Plava"),
    PURPLE("Ljubičasta"),
    SILVER("Srebrna"),
    GOLD("Zlatna"),
    MULTICOLOR("Višebojna");

    private final String label;

    ItemColor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}