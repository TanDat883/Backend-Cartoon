package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.AssistantPricingResponse;
import flim.backendcartoon.services.AssistantPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for AI Assistant pricing queries
 * Provides endpoint for active subscription packages with pricing
 *
 * @author CartoonToo Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/pricing/assistant")
@RequiredArgsConstructor
public class AssistantPricingController {

    private final AssistantPricingService assistantPricingService;

    /**
     * Get active subscription packages for AI Assistant
     * Returns packages with active price lists and calculated monthly prices
     *
     * @param date Target date (optional, defaults to today)
     * @return Active pricing response
     */
    @GetMapping("/active")
    public ResponseEntity<AssistantPricingResponse> getActivePricing(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        log.info("📞 GET /api/pricing/assistant/active called with date: {}", date);

        try {
            AssistantPricingResponse response = assistantPricingService.getActivePricing(date);

            log.info("✅ Returning {} active packages", response.getPackages().size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting active pricing: {}", e.getMessage(), e);

            // Return empty response on error
            AssistantPricingResponse errorResponse = AssistantPricingResponse.builder()
                    .date(date != null ? date.toString() : LocalDate.now().toString())
                    .currency("VND")
                    .packages(java.util.List.of())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }
}

