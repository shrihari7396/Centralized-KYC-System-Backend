package org.rbi.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DiscoveryServiceApplication.class)
@ActiveProfiles("test")
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Eureka Discovery Server context bootstrap
    }
}
