package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.CertificateDetailsDto;
import dev.morkom.keymanager.dto.KeystoreEntry;
import dev.morkom.keymanager.dto.KeystoreModificationRequest;
import dev.morkom.keymanager.service.KeystoreService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/keystore")
public class KeystoreController {

    private final KeystoreService keystoreService;

    public KeystoreController(KeystoreService keystoreService) {
        this.keystoreService = keystoreService;
    }

    @PostMapping("/view")
    public ResponseEntity<List<KeystoreEntry>> viewKeystore(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws Exception {
        List<KeystoreEntry> entries = keystoreService.viewKeystore(file, password);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/view-entry")
    public ResponseEntity<CertificateDetailsDto> viewEntry(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password,
            @RequestParam("alias") String alias) throws Exception {
        CertificateDetailsDto details = keystoreService.getCertificateDetails(file, password, alias);
        return ResponseEntity.ok(details);
    }

    @PostMapping(value = "/save", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Resource> saveKeystore(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") KeystoreModificationRequest request) throws Exception {
        Resource modifiedKeystore = keystoreService.applyModifications(file, request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modified-" + Objects.requireNonNull(file.getOriginalFilename()) + "\"")
                .body(modifiedKeystore);
    }
}
