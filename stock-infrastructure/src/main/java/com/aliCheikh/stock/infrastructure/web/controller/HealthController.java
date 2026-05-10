package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.infrastructure.web.dto.HealthStatusResponse;
import com.aliCheikh.stock.infrastructure.web.dto.HealthStatusValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthStatusResponse status() {
        return new HealthStatusResponse(HealthStatusValue.UP, "0.0.1", Instant.now());
    }
}
