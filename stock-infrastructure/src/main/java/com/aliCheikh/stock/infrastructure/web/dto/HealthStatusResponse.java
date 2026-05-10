package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.Instant;

public record HealthStatusResponse(HealthStatusValue status, String version, Instant timestamp) {

}
