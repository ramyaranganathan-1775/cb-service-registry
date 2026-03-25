package com.igot.service_locator.health.controller;


import com.igot.service_locator.health.service.HealthService;
import com.igot.service_locator.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
public class HealthController {

    @Autowired
    private HealthService healthService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck() throws Exception {
        String requestId = UUID.randomUUID().toString();
        ApiResponse response = healthService.checkHealthStatus(requestId);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
