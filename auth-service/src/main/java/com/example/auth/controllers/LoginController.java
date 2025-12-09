package com.example.auth.controllers;

import com.example.auth.dtos.credentials.CredentialsDetailsDTO;
import com.example.auth.entities.Credentials;
import com.example.auth.services.CredentialsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Validated
public class LoginController {

    private final CredentialsService credentialsService;

    public LoginController(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody CredentialsDetailsDTO credentialsDetailsDTO) {
        // Folosim metoda login care returnează userul complet (cu rol cu tot)
        Optional<Credentials> userOpt = credentialsService.login(credentialsDetailsDTO.getEmail(), credentialsDetailsDTO.getPassword());

        if (userOpt.isPresent()) {
            Credentials user = userOpt.get();

            Map<String, Object> response = new HashMap<>();
            // --- MODIFICARE: Punem rolul REAL din baza de date ---
            response.put("role", user.getRole());
            response.put("userId", user.getId());
            // Token JWT valid hardcodat (pentru moment)
            response.put("token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTUxNjIzOTAyMn0.OCPCKq8j8y5Q5j0j9o9y9q9y9q9y9q9y9q9y9q9y9q9");

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Credențiale invalide");
        }
    }
}