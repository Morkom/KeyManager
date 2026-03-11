package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.GenerateCrlRequest;
import dev.morkom.keymanager.dto.RevokeCertificateRequest;
import dev.morkom.keymanager.service.RevocationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revocation")
public class RevocationController {

    private final RevocationService revocationService;

    public RevocationController(RevocationService revocationService) {
        this.revocationService = revocationService;
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeCertificate(@RequestBody RevokeCertificateRequest request) throws Exception {
        revocationService.revokeCertificate(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/crl")
    public ResponseEntity<Resource> downloadCrl(@RequestBody GenerateCrlRequest request) throws Exception {
        Resource resource = revocationService.generateCrl(request);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/pkix-crl"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + request.caFilename() + ".crl\"")
                .body(resource);
    }
}
