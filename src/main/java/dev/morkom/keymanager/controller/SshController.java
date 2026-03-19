package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.GenerateSshKeyRequest;
import dev.morkom.keymanager.dto.GenerateSshKeyResponse;
import dev.morkom.keymanager.service.SshKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ssh")
public class SshController {

    private final SshKeyService sshKeyService;

    public SshController(SshKeyService sshKeyService) {
        this.sshKeyService = sshKeyService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateSshKeyResponse> generateSshKey(@RequestBody GenerateSshKeyRequest request) throws Exception {
        GenerateSshKeyResponse response = sshKeyService.generateSshKey(request);
        return ResponseEntity.ok(response);
    }
}
