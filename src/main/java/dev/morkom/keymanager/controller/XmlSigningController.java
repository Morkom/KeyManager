package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.service.XmlSigningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/xml")
public class XmlSigningController {

    private final XmlSigningService xmlSigningService;

    public XmlSigningController(XmlSigningService xmlSigningService) {
        this.xmlSigningService = xmlSigningService;
    }

    @PostMapping("/sign")
    public ResponseEntity<String> signXml(
            @RequestParam("xmlFile") MultipartFile xmlFile,
            @RequestParam("keystoreFile") MultipartFile keystoreFile,
            @RequestParam("storeType") String storeType,
            @RequestParam("alias") String alias,
            @RequestParam("storePassword") String storePassword,
            @RequestParam("keyPassword") String keyPassword) {
        try {
            String signedXml = xmlSigningService.signXml(
                    xmlFile.getInputStream(),
                    keystoreFile.getInputStream(),
                    storeType,
                    alias,
                    storePassword,
                    keyPassword);
            return ResponseEntity.ok(signedXml);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error signing XML: " + e.getMessage());
        }
    }

    @PostMapping("/keystore-aliases")
    public ResponseEntity<?> getAliases(
            @RequestParam("keystoreFile") MultipartFile keystoreFile,
            @RequestParam("storeType") String storeType,
            @RequestParam("storePassword") String storePassword) {
        try {
            List<String> aliases = xmlSigningService.getAliases(keystoreFile.getInputStream(), storeType, storePassword);
            return ResponseEntity.ok(aliases);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading keystore file: " + e.getMessage());
        }
    }
}
