package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.CreateCaRequest;
import dev.morkom.keymanager.dto.CreateCaResponse;
import dev.morkom.keymanager.dto.SignCertificateRequest;
import dev.morkom.keymanager.dto.SignCertificateResponse;
import dev.morkom.keymanager.service.CaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ca")
public class CaController {

    private final CaService caService;

    public CaController(CaService caService) {
        this.caService = caService;
    }

    @PostMapping
    public ResponseEntity<CreateCaResponse> createRootCa(@RequestBody CreateCaRequest request) throws Exception {
        CreateCaResponse response = caService.createRootCa(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadKeystore(@RequestParam("file") MultipartFile file) throws IOException {
        caService.storeCaKeystore(file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign")
    public ResponseEntity<SignCertificateResponse> signCertificate(@RequestBody SignCertificateRequest request) throws Exception {
        SignCertificateResponse response = caService.signCertificate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listCas() throws IOException {
        return ResponseEntity.ok(caService.listCaKeystores());
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Void> deleteCa(@PathVariable String filename) throws IOException {
        caService.deleteCaKeystore(filename);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadKeystore(@PathVariable String filename) throws IOException {
        Resource resource = caService.loadKeystoreAsResource(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
