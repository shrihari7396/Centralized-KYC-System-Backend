package org.kyc.management.bankservice.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalCustomers;
    private long approvedCount;
    private long pendingCount;
    private long rejectedCount;
    private long overrideCount;
    private Map<String, Long> riskDistribution;
}
