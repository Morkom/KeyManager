package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.*;
import dev.morkom.keymanager.service.CryptoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @PostMapping("/encrypt")
    public ResponseEntity<CryptoResponse> encrypt(@RequestBody CryptoRequest request) throws Exception {
        CryptoResponse response = cryptoService.encrypt(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/decrypt")
    public ResponseEntity<CryptoResponse> decrypt(@RequestBody CryptoRequest request) throws Exception {
        CryptoResponse response = cryptoService.decrypt(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bcrypt/hash")
    public ResponseEntity<BcryptHashResponse> bcryptHash(@RequestBody BcryptHashRequest request) {
        return ResponseEntity.ok(cryptoService.bcryptHash(request));
    }

    @PostMapping("/bcrypt/verify")
    public ResponseEntity<BcryptVerifyResponse> bcryptVerify(@RequestBody BcryptVerifyRequest request) {
        return ResponseEntity.ok(cryptoService.bcryptVerify(request));
    }
}
