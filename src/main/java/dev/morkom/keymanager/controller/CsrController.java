package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.CreateCsrRequest;
import dev.morkom.keymanager.dto.CreateCsrResponse;
import dev.morkom.keymanager.service.CsrService;
import org.springframework.core.io.Resource;
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
    public ResponseEntity<Resource> downloadPrivateKey(@PathVariable String filename) throws IOException {
        Resource resource = csrService.loadPrivateKeyAsResource(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
