package org.example.nasaosam.meta.controller;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.meta.dto.ItemOptionsResponse;
import org.example.nasaosam.meta.dto.OptionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kontroler nema zavisnosti, pa se pravi običnim new i poziva direktno.
 * Ono što test štiti: ako neko doda konstantu u enum, ona mora da se pojavi u listi,
 * a ako je obriše, frontend ne sme da dobije staru vrednost.
 */
class MetaControllerTest {

    private final MetaController controller = new MetaController();

    private boolean containsValue(List<OptionResponse> options, String value) {
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).value().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void listsHaveSameSizeAsEnums() {
        ItemOptionsResponse response;

        response = controller.getOptions();

        assertEquals(ItemCategory.values().length, response.categories().size());
        assertEquals(DocumentType.values().length, response.documentTypes().size());
        assertEquals(ItemColor.values().length, response.colors().size());
    }

    @Test
    void optionsCarrySerbianLabels() {
        ItemOptionsResponse response;
        OptionResponse firstCategory;

        response = controller.getOptions();
        firstCategory = response.categories().get(0);

        // Prva konstanta u enumu je WALLET, prikazuje se kao "Novčanik"
        assertEquals(ItemCategory.values()[0].name(), firstCategory.value());
        assertEquals(ItemCategory.values()[0].getLabel(), firstCategory.label());
    }

    @Test
    void otherCategoryAndDocumentTypesArePresent() {
        ItemOptionsResponse response;

        response = controller.getOptions();

        // Bez "Ostalo" korisnik ne bi mogao da prijavi predmet van liste
        assertTrue(containsValue(response.categories(), "OTHER"));
        assertTrue(containsValue(response.categories(), "DOCUMENT"));
        assertTrue(containsValue(response.documentTypes(), "ID_CARD"));
    }
}