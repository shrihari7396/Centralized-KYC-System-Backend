package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.service.DigilockerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/digilocker/files")
@RequiredArgsConstructor
public class FileController {

    private final DigilockerService digilockerService;

    @GetMapping("/issued")
    public ResponseEntity<Object> getIssuedDocuments(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(
                digilockerService.getIssuedDocuments(token)
        );
    }

    @GetMapping(value = "/{uri}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String uri
    ) {
        String token = extractToken(authHeader);
        byte[] file = digilockerService.getFileFromUri(token, uri);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + uri + ".pdf\"")
                .body(file);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
