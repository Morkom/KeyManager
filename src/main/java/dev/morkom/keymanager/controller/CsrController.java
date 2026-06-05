package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.CreateCsrRequest;
import dev.morkom.keymanager.dto.CreateCsrResponse;
import dev.morkom.keymanager.service.CsrService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/csr")
public class CsrController {

    private final CsrService csrService;

    public CsrController(CsrService csrService) {
        this.csrService = csrService;
    }

    @PostMapping
    public ResponseEntity<CreateCsrResponse> createCsr(@RequestBody CreateCsrRequest request) throws Exception {
        CreateCsrResponse response = csrService.createCsr(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadPrivateKey(@PathVariable String filename) throws IOException {
        byte[] data = csrService.getPrivateKeyAsBytes(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                .body(data);
    }

    @PostMapping("/download-pem")
    public ResponseEntity<byte[]> downloadEncryptedPemKey(@RequestBody CreateCsrRequest request) throws Exception {
        byte[] data = csrService.generateEncryptedPemKey(request);
        String filename = "private-key-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".pem";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                .body(data);
    }

    @PostMapping("/download-public-key")
    public ResponseEntity<byte[]> downloadPublicKey(@RequestBody CreateCsrRequest request) throws Exception {
        byte[] data = csrService.generatePublicKey(request);
        String filename = "public-key-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".pem";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                .body(data);
    }
}
