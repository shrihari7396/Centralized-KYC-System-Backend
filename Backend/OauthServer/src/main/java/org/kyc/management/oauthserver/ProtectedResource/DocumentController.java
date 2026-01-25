package org.kyc.management.oauthserver.ProtectedResource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DocumentController {
    @GetMapping("/docs")
    public String docs(){
        return null;
    }

}
