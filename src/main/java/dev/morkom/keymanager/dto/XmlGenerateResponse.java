package dev.morkom.keymanager.dto;

import java.util.List;

public record XmlGenerateResponse(
    String xml,
    String error,
    List<String> rootElements
) {}
