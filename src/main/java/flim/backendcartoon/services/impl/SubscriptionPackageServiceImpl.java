/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:32 PM
 */

import flim.backendcartoon.entities.DTO.request.SubscriptionPackageRequest;
import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionDetail;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.*;
import flim.backendcartoon.services.SubscriptionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final PriceItemRepository priceItemRepository;
    private final PromotionDetailRepository promotionPackageRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionLineRepository promotionLineRepository;

    @Autowired
    public SubscriptionPackageServiceImpl(SubscriptionPackageRepository subscriptionPackageRepository,
                                          PromotionDetailRepository promotionPackageRepository, PromotionRepository promotionRepository
                                          , PriceItemRepository priceItemRepository, PromotionLineRepository promotionLineRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.promotionPackageRepository = promotionPackageRepository;
        this.promotionRepository = promotionRepository;
        this.priceItemRepository = priceItemRepository;
        this.promotionLineRepository = promotionLineRepository;
    }

    @Override
    public void saveSubscriptionPackage(SubscriptionPackageRequest subscriptionPackage) {
        if (subscriptionPackageRepository.get(subscriptionPackage.getPackageId()) != null) {
            throw new IllegalArgumentException("SubscriptionPackage with ID " + subscriptionPackage.getPackageId() + " already exists.");
        }
        SubscriptionPackage pkg = new SubscriptionPackage();
        pkg.setPackageId(subscriptionPackage.getPackageId());
        pkg.setPackageName(subscriptionPackage.getPackageName());
        pkg.setImageUrl(subscriptionPackage.getImageUrl());
        pkg.setApplicablePackageType(subscriptionPackage.getApplicablePackageType());
        pkg.setDurationInDays(subscriptionPackage.getDurationInDays());
        pkg.setFeatures(subscriptionPackage.getFeatures());
        pkg.setCreatedAt(LocalDate.now());

        subscriptionPackageRepository.save(pkg);
    }

    @Override
    public SubscriptionPackageResponse findSubscriptionPackageById(String packageId) {
        return findAllSubscriptionPackages().stream()
                .filter(pkg -> packageId.equals(pkg.getPackageId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<SubscriptionPackageResponse> findAllSubscriptionPackages() {
        List<SubscriptionPackage> packages = subscriptionPackageRepository.findAll();

        return packages.stream()
                .map(pkg -> {
                    // --- LẤY GIÁ GỐC THEO PriceList/PriceItem ---
                    double base = getBasePriceFromCurrentPriceList(pkg);

                    // --- ÁP KHUYẾN MÃI  ---
                    List<String> ids = normalizeIdsFromString(pkg.getPackageId());
                    List<PromotionDetail> promos =
                            Optional.ofNullable(promotionPackageRepository.findPromotionsByPackageId(ids))
                                    .orElse(List.of());
                    PromotionDetail best = pickBestPromotion(promos);

                    int percent = (best == null || best.getDiscountPercent() == null) ? 0 : best.getDiscountPercent();
                    double effective = calcPrice(base, percent);

                    SubscriptionPackageResponse dto = new SubscriptionPackageResponse();
                    dto.setPackageId(pkg.getPackageId());
                    dto.setNamePackage(pkg.getPackageName());
                    dto.setImageUrl(pkg.getImageUrl());
                    dto.setAmount(base);
                    dto.setDiscountedAmount(effective);
                    dto.setApplicablePackageType(pkg.getApplicablePackageType());
                    dto.setDurationInDays(pkg.getDurationInDays());
                    dto.setFeatures(pkg.getFeatures());
                    dto.setAppliedDiscountPercent(percent);
                    if (best != null) dto.setAppliedPromotionId(best.getPromotionId());

                    return dto;
                })
                .toList();
    }

    @Override
    public List<SubscriptionPackage> getAll() {
        return subscriptionPackageRepository.findAll();
    }

    @Override
    public void deleteSubscriptionPackage(String packageId) {
        subscriptionPackageRepository.delete(packageId);
    }

    @Override
    public void updateSubscriptionPackage(String packageId, SubscriptionPackageRequest subscriptionPackage) {
        SubscriptionPackage optionalPkg = subscriptionPackageRepository.get(packageId);
        if (optionalPkg == null) {
            throw new NoSuchElementException("SubscriptionPackage with ID " + packageId + " not found.");
        }

        SubscriptionPackage pkg = optionalPkg;
        pkg.setPackageName(subscriptionPackage.getPackageName());
        pkg.setApplicablePackageType(subscriptionPackage.getApplicablePackageType());
        pkg.setDurationInDays(subscriptionPackage.getDurationInDays());
        pkg.setFeatures(subscriptionPackage.getFeatures());

        subscriptionPackageRepository.save(pkg);
    }

    /** NEW: lấy giá gốc từ PriceItem theo currentPriceListId + packageId */
    private double getBasePriceFromCurrentPriceList(SubscriptionPackage pkg) {
        String priceListId = pkg.getCurrentPriceListId();
        if (priceListId == null || priceListId.isBlank()) return 0.0;

        var item = priceItemRepository.get(priceListId, pkg.getPackageId());
        return item != null ? defaultDouble(item.getAmount()) : 0.0;
    }

    /** Chọn promotion hợp lệ có % giảm lớn nhất (theo ngày + status) */
    private PromotionDetail pickBestPromotion(List<PromotionDetail> promos) {
        if (promos == null || promos.isEmpty()) return null;

        LocalDate today = LocalDate.now();

        return promos.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
                // --- Filter theo chính PromotionDetail (optional window + status) ---
                // --- Filter theo Promotion cha ---
                .filter(p -> {
                    Promotion promo = promotionRepository.findById(p.getPromotionId()).orElse(null);
                    if (promo == null) return false;
                    boolean statusOk = "ACTIVE".equalsIgnoreCase(promo.getStatus());
                    boolean startOk  = promo.getStartDate() == null || !today.isBefore(promo.getStartDate());
                    boolean endOk    = promo.getEndDate() == null   || !today.isAfter(promo.getEndDate());
                    return statusOk && startOk && endOk;
                })
                // --- Filter theo PromotionLine chứa detail ---
                .filter(p -> {
                    if (p.getPromotionLineId() == null || p.getPromotionLineId().isBlank()) {
                        // Nếu chưa gắn line, cho qua (tuỳ business). Nếu bắt buộc phải có line -> return false;
                        return true;
                    }
                    var line = promotionLineRepository.get(p.getPromotionId(), p.getPromotionLineId());
                    if (line == null) return false;

                    boolean statusOk = line.getStatus() == null || "ACTIVE".equalsIgnoreCase(line.getStatus());
                    boolean typeOk   = "PACKAGE".equalsIgnoreCase(String.valueOf(line.getPromotionLineType()));
                    boolean startOk  = line.getStartDate() == null || !today.isBefore(line.getStartDate());
                    boolean endOk    = line.getEndDate() == null   || !today.isAfter(line.getEndDate());

                    return statusOk && typeOk && startOk && endOk;
                })
                // --- chọn % lớn nhất ---
                .max(Comparator.comparingInt(PromotionDetail::getDiscountPercent))
                .orElse(null);
    }


    /** Giá sau giảm theo % */
    private double calcPrice(double base, int percent) {
        if (base <= 0) return base;
        if (percent <= 0) return base;
        if (percent >= 100) return 0.0;
        return base * (100 - percent) / 100.0;
    }

    private double defaultDouble(Double v) { return v == null ? 0.0 : v; }

    public static List<String> normalizeIdsFromString(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String s = raw.trim();

        if (s.startsWith("PACKAGE#[")) {
            s = s.substring("PACKAGE#".length());
        }
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
        }

        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(x -> x.startsWith("PACKAGE#") ? x.substring("PACKAGE#".length()) : x)
                .filter(str -> !str.isEmpty())
                .toList();
    }

}
