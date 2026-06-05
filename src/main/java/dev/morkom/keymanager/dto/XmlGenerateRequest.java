package dev.morkom.keymanager.dto;

public record XmlGenerateRequest(
    String xsdFilename,
    String rootElement
) {}
