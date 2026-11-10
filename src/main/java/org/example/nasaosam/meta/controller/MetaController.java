package org.example.nasaosam.meta.controller;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.meta.dto.ItemOptionsResponse;
import org.example.nasaosam.meta.dto.OptionResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/options")
    public ItemOptionsResponse getOptions() {
        List<OptionResponse> categories;
        List<OptionResponse> documentTypes;
        List<OptionResponse> colors;
        ItemCategory[] categoryValues;
        DocumentType[] documentTypeValues;
        ItemColor[] colorValues;

        categories = new ArrayList<>();
        categoryValues = ItemCategory.values();
        for (int index = 0; index < categoryValues.length; index++) {
            categories.add(new OptionResponse(categoryValues[index].name(), categoryValues[index].getLabel()));
        }

        documentTypes = new ArrayList<>();
        documentTypeValues = DocumentType.values();
        for (int index = 0; index < documentTypeValues.length; index++) {
            documentTypes.add(new OptionResponse(documentTypeValues[index].name(), documentTypeValues[index].getLabel()));
        }

        colors = new ArrayList<>();
        colorValues = ItemColor.values();
        for (int index = 0; index < colorValues.length; index++) {
            colors.add(new OptionResponse(colorValues[index].name(), colorValues[index].getLabel()));
        }

        return new ItemOptionsResponse(categories, documentTypes, colors);
    }
}
