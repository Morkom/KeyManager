package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.AlgorithmDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    @GetMapping
    public List<AlgorithmDto> getAvailableAlgorithms() {
        return List.of(
                new AlgorithmDto("RSA-2048", "RSA 2048-bit", "Standard RSA key with 2048 bits."),
                new AlgorithmDto("RSA-3072", "RSA 3072-bit", "Standard RSA key with 3072 bits."),
                new AlgorithmDto("RSA-4096", "RSA 4096-bit", "High-security RSA key with 4096 bits."),
                new AlgorithmDto("EC-P256", "ECDSA P-256 (secp256r1)", "Elliptic Curve standard security."),
                new AlgorithmDto("EC-P384", "ECDSA P-384 (secp384r1)", "Elliptic Curve high security."),
                new AlgorithmDto("EC-P521", "ECDSA P-521 (secp521r1)", "Elliptic Curve highest security."),
                new AlgorithmDto("BP-256R1", "Brainpool P-256r1", "Brainpool curve standard security."),
                new AlgorithmDto("BP-384R1", "Brainpool P-384r1", "Brainpool curve high security."),
                new AlgorithmDto("BP-512R1", "Brainpool P-512r1", "Brainpool curve highest security.")
        );
    }
}