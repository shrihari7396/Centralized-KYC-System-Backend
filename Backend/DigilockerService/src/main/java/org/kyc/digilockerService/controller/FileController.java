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
    public ResponseEntity<Object> getIssuedDocuments(@RequestHeader("Authorization") String authHeader) {
        String accessToken = extractToken(authHeader);
        return ResponseEntity.ok(digilockerService.getIssuedDocuments(accessToken));
    }

    @GetMapping(value = "/{uri}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getFile(@RequestHeader("Authorization") String authHeader, @PathVariable String uri) {
        String accessToken = extractToken(authHeader);
        byte[] fileData = digilockerService.getFileFromUri(accessToken, uri);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uri + ".pdf\"")
                .body(fileData);
    }

    @PostMapping("/pull")
    public ResponseEntity<Object> pullDocument(@RequestHeader("Authorization") String authHeader,
            @RequestBody Object pullRequest) {
        String accessToken = extractToken(authHeader);
        return ResponseEntity.ok(digilockerService.pullDocument(accessToken, pullRequest));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
