package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.XmlGenerateRequest;
import dev.morkom.keymanager.dto.XmlGenerateResponse;
import dev.morkom.keymanager.dto.XmlTransformResponse;
import dev.morkom.keymanager.dto.XmlValidationResponse;
import dev.morkom.keymanager.service.XmlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/xml")
public class XmlController {

    private final XmlService xmlService;

    public XmlController(XmlService xmlService) {
        this.xmlService = xmlService;
    }

    @PostMapping("/validate")
    public ResponseEntity<XmlValidationResponse> validateXml(
            @RequestParam("xmlFile") MultipartFile xmlFile,
            @RequestParam(value = "xsdFile", required = false) MultipartFile xsdFile,
            @RequestParam(value = "prepackagedXsd", required = false) String prepackagedXsd) {
        
        if (xsdFile != null) {
            return ResponseEntity.ok(xmlService.validate(xmlFile, xsdFile));
        } else if (prepackagedXsd != null && !prepackagedXsd.isEmpty()) {
            return ResponseEntity.ok(xmlService.validateWithPrepackagedXsd(xmlFile, prepackagedXsd));
        } else {
            return ResponseEntity.badRequest().body(new XmlValidationResponse(false, "No XSD source provided."));
        }
    }

    @PostMapping("/transform")
    public ResponseEntity<XmlTransformResponse> transformXml(
            @RequestParam("xmlFile") MultipartFile xmlFile,
            @RequestParam("xsltFile") MultipartFile xsltFile) {
        return ResponseEntity.ok(xmlService.transform(xmlFile, xsltFile));
    }

    @GetMapping("/prepackaged-xsds")
    public ResponseEntity<List<String>> getPrepackagedXsds() throws IOException {
        return ResponseEntity.ok(xmlService.listPrepackagedXsds());
    }

    @PostMapping("/generate")
    public ResponseEntity<XmlGenerateResponse> generateXml(@RequestBody XmlGenerateRequest request) {
        return ResponseEntity.ok(xmlService.generateXmlFromXsd(request));
    }
}
