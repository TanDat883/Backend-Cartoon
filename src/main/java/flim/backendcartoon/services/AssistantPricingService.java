package flim.backendcartoon.services;

import flim.backendcartoon.entities.DTO.response.AssistantPackageDTO;
import flim.backendcartoon.entities.DTO.response.AssistantPricingResponse;
import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.PriceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for AI Assistant pricing queries
 * Provides active packages with calculated monthly prices
 *
 * @author CartoonToo Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantPricingService {

    private final PricingService pricingService;
    private final PriceItemRepository priceItemRepository;

    /**
     * Get active packages for a specific date
     * Returns only packages with active price lists and their prices
     *
     * @param date Target date (null = today)
     * @return AssistantPricingResponse with active packages
     */
    public AssistantPricingResponse getActivePricing(LocalDate date) {
        if (date == null) {
            date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }

        log.info("💰 Getting active pricing for date: {}", date);

        try {
            // 1. Get active price lists
            List<PriceList> activePriceLists = pricingService.getAllActivePriceList(date);

            if (activePriceLists.isEmpty()) {
                log.warn("⚠️ No active price lists found for date: {}", date);
                return buildEmptyResponse(date);
            }

            Set<String> activePriceListIds = activePriceLists.stream()
                    .map(PriceList::getPriceListId)
                    .collect(Collectors.toSet());

            log.info("📋 Found {} active price lists", activePriceListIds.size());

            // 2. Get all subscription packages
            List<SubscriptionPackage> allPackages = pricingService.getAllSubscriptionPackages();

            // 3. Filter packages by active price list
            List<SubscriptionPackage> activePackages = allPackages.stream()
                    .filter(pkg -> pkg.getCurrentPriceListId() != null
                                && activePriceListIds.contains(pkg.getCurrentPriceListId()))
                    .collect(Collectors.toList());

            log.info("📦 Found {} active packages", activePackages.size());

            // 4. Get price items for these packages
            Map<String, PriceItem> priceItemMap = getPriceItemMap(activePriceListIds, activePackages);

            // 5. Map to DTO with pricing
            List<AssistantPackageDTO> packageDTOs = activePackages.stream()
                    .map(pkg -> mapToAssistantPackageDTO(pkg, priceItemMap))
                    .filter(dto -> dto.getPrice() != null && dto.getPrice() > 0)
                    .collect(Collectors.toList());

            log.info("✅ Returning {} packages with pricing", packageDTOs.size());

            return AssistantPricingResponse.builder()
                    .date(date.toString())
                    .currency("VND")
                    .packages(packageDTOs)
                    .updatedAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting active pricing: {}", e.getMessage(), e);
            return buildEmptyResponse(date);
        }
    }

    /**
     * Get price items as a map for quick lookup
     * Key: priceListId + packageId
     */
    private Map<String, PriceItem> getPriceItemMap(Set<String> priceListIds, List<SubscriptionPackage> packages) {
        return priceItemRepository.findAll().stream()
                .filter(item -> priceListIds.contains(item.getPriceListId()))
                .collect(Collectors.toMap(
                    item -> item.getPriceListId() + ":" + item.getPackageId(),
                    item -> item,
                    (a, b) -> a // Keep first if duplicate
                ));
    }

    /**
     * Map SubscriptionPackage + PriceItem to AssistantPackageDTO
     * Calculate monthly price: round(price * 30 / durationDays)
     */
    private AssistantPackageDTO mapToAssistantPackageDTO(SubscriptionPackage pkg, Map<String, PriceItem> priceItemMap) {
        // Get price from PriceItem
        String key = pkg.getCurrentPriceListId() + ":" + pkg.getPackageId();
        PriceItem priceItem = priceItemMap.get(key);

        Long price = 0L;
        if (priceItem != null && priceItem.getAmount() != null) {
            price = priceItem.getAmount().longValue();
        }

        // Calculate monthly price
        Integer durationDays = pkg.getDurationInDays();
        Long priceMonthly = calculateMonthlyPrice(price, durationDays);

        return AssistantPackageDTO.builder()
                .packageId(pkg.getPackageId())
                .name(pkg.getPackageName())
                .type(pkg.getApplicablePackageType() != null ? pkg.getApplicablePackageType().name() : "UNKNOWN")
                .durationDays(durationDays)
                .price(price)
                .priceMonthly(priceMonthly)
                .features(pkg.getFeatures())
                .priceListId(pkg.getCurrentPriceListId())
                .description(null) // SubscriptionPackage doesn't have description field
                .build();
    }

    /**
     * Calculate monthly price from total price and duration
     * Formula: round(price * 30 / durationDays)
     */
    private Long calculateMonthlyPrice(Long price, Integer durationDays) {
        if (price == null || durationDays == null || durationDays == 0) {
            return 0L;
        }

        double monthlyPrice = (double) price * 30.0 / durationDays;
        return Math.round(monthlyPrice);
    }

    /**
     * Build empty response when no data available
     */
    private AssistantPricingResponse buildEmptyResponse(LocalDate date) {
        return AssistantPricingResponse.builder()
                .date(date.toString())
                .currency("VND")
                .packages(List.of())
                .updatedAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();
    }
}
