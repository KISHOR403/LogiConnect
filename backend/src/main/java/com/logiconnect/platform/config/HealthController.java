package com.logiconnect.platform.config;

import com.logiconnect.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public Health & Liveness Controller for LogiConnect Platform API.
 */
@RestController
@Tag(name = "Health", description = "Platform health check and liveness verification endpoints")
public class HealthController {

    /**
     * Minimal application health check endpoint.
     * Accessible via GET /api/v1/health (or GET /health with servlet context-path /api/v1).
     */
    @GetMapping("/health")
    @Operation(
            summary = "API Health Check",
            description = "Returns current operational status of the LogiConnect Platform API without requiring authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "API is operational and healthy",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> getHealth() {
        Map<String, String> healthStatus = Map.of("status", "UP");
        return ResponseEntity.ok(ApiResponse.success(healthStatus, "LogiConnect API is healthy"));
    }
}
