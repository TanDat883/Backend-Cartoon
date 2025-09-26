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
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.PriceItemRepository;
import flim.backendcartoon.repositories.PromotionPackageRepository;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.SubscriptionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final PriceItemRepository priceItemRepository;
    private final PromotionPackageRepository promotionPackageRepository;
    private final PromotionRepository promotionRepository;

    @Autowired
    public SubscriptionPackageServiceImpl(SubscriptionPackageRepository subscriptionPackageRepository,
                                          PromotionPackageRepository promotionPackageRepository, PromotionRepository promotionRepository
                                          , PriceItemRepository priceItemRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.promotionPackageRepository = promotionPackageRepository;
        this.promotionRepository = promotionRepository;
        this.priceItemRepository = priceItemRepository;
    }

    @Override
    public void saveSubscriptionPackage(SubscriptionPackageRequest subscriptionPackage) {
        SubscriptionPackage pkg = new SubscriptionPackage();
        pkg.setPackageId(subscriptionPackage.getPackageId());
        pkg.setPackageName(subscriptionPackage.getPackageName());
        pkg.setImageUrl(subscriptionPackage.getImageUrl());
        pkg.setApplicablePackageType(subscriptionPackage.getApplicablePackageType());
        pkg.setDurationInDays(subscriptionPackage.getDurationInDays());
        pkg.setFeatures(subscriptionPackage.getFeatures());

        subscriptionPackageRepository.save(pkg);
    }

    @Override
    public SubscriptionPackageResponse findSubscriptionPackageById(String packageId) {
        return findAllSubscriptionPackages().stream()
                .filter(pkg -> packageId.equals(pkg.getPackageId()))
                .findFirst()
                .orElse(null);
    }

//    @Override
//    public List<SubscriptionPackageResponse> findAllSubscriptionPackages() {
//        List<SubscriptionPackage> packages = subscriptionPackageRepository.findAll();
//
//        return packages.stream()
//                .map(pkg -> {
//                    List<String> ids = normalizeIdsFromString(pkg.getPackageId());
//
//                    // lấy toàn bộ promotions áp dụng cho package này
//                    List<PromotionPackage> promos =
//                            Optional.ofNullable(promotionPackageRepository.findPromotionsByPackageId(ids))
//                                    .orElse(List.of());
//
//                    // chọn promo hợp lệ có % giảm lớn nhất
//                    PromotionPackage best = pickBestPromotion(promos);
//
//                    double base = defaultDouble(pkg.getAmount());
//                    int percent = (best == null || best.getDiscountPercent() == null) ? 0 : best.getDiscountPercent();
//                    double effective = calcPrice(base, percent);
//
//                    SubscriptionPackageResponse dto = new SubscriptionPackageResponse();
//                    dto.setPackageId(pkg.getPackageId());
//                    dto.setNamePackage(pkg.getNamePackage());
//                    dto.setAmount(base);
//                    dto.setDiscountedAmount(effective);               // giá sau giảm
//                    dto.setApplicablePackageType(pkg.getApplicableVipLevel());
//                    dto.setDurationInDays(pkg.getDurationInDays());
//                    dto.setFeatures(pkg.getFeatures());
//                    dto.setAppliedDiscountPercent(percent);           // % giảm áp dụng
//                    if (best != null) dto.setAppliedPromotionId(best.getPromotionId());
//                    return dto;
//                })
//                .toList();
//    }

    /** Chọn promotion hợp lệ có % giảm lớn nhất (theo ngày + status) */
//    private PromotionPackage pickBestPromotion(List<PromotionPackage> promos) {
//        if (promos == null || promos.isEmpty()) return null;
//
//        LocalDate today = LocalDate.now();
//
//        return promos.stream()
//                .filter(Objects::nonNull)
//                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
//                .filter(p -> {
//                    // Join sang PROMO cha để kiểm tra hiệu lực
//                    Promotion promoParent = promotionRepository.findById(p.getPromotionId()).orElse(null);
//                    if (promoParent == null) return false;
//
//                    boolean active = "ACTIVE".equalsIgnoreCase(promoParent.getStatus());
//                    boolean startOk = promoParent.getStartDate() == null || !today.isBefore(promoParent.getStartDate());
//                    boolean endOk   = promoParent.getEndDate() == null   || !today.isAfter(promoParent.getEndDate());
//                    return active && startOk && endOk;
//                })
//                .max(Comparator.comparingInt(PromotionPackage::getDiscountPercent))
//                .orElse(null);
//    }
//
//
//    /** Giá sau giảm theo % */
//    private double calcPrice(double base, int percent) {
//        if (base <= 0) return base;
//        if (percent <= 0) return base;
//        if (percent >= 100) return 0.0;
//        return base * (100 - percent) / 100.0;
//    }
//
//    private double defaultDouble(Double v) {
//        return v == null ? 0.0 : v;
//    }
//
//    public static List<String> normalizeIdsFromString(String raw) {
//        if (raw == null || raw.isBlank()) return List.of();
//        String s = raw.trim();
//
//        // Nếu có prefix "PACKAGE#["..."]"
//        if (s.startsWith("PACKAGE#[")) {
//            s = s.substring("PACKAGE#".length()); // còn "[a, b]"
//        }
//        // Bỏ ngoặc vuông nếu có
//        if (s.startsWith("[") && s.endsWith("]")) {
//            s = s.substring(1, s.length() - 1);
//        }
//
//        return Arrays.stream(s.split(","))
//                .map(String::trim)
//                .map(x -> x.startsWith("PACKAGE#") ? x.substring("PACKAGE#".length()) : x)
//                .filter(str -> !str.isEmpty())
//                .toList();
//    }

    @Override
    public List<SubscriptionPackageResponse> findAllSubscriptionPackages() {
        List<SubscriptionPackage> packages = subscriptionPackageRepository.findAll();

        return packages.stream()
                .map(pkg -> {
                    // --- LẤY GIÁ GỐC THEO PriceList/PriceItem ---
                    double base = getBasePriceFromCurrentPriceList(pkg);

                    // --- ÁP KHUYẾN MÃI (giữ logic cũ) ---
                    List<String> ids = normalizeIdsFromString(pkg.getPackageId());
                    List<PromotionPackage> promos =
                            Optional.ofNullable(promotionPackageRepository.findPromotionsByPackageId(ids))
                                    .orElse(List.of());
                    PromotionPackage best = pickBestPromotion(promos);

                    int percent = (best == null || best.getDiscountPercent() == null) ? 0 : best.getDiscountPercent();
                    double effective = calcPrice(base, percent);

                    // --- BUILD DTO ---
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

    /** NEW: lấy giá gốc từ PriceItem theo currentPriceListId + packageId */
    private double getBasePriceFromCurrentPriceList(SubscriptionPackage pkg) {
        String priceListId = pkg.getCurrentPriceListId();
        if (priceListId == null || priceListId.isBlank()) return 0.0;

        // CHÚ Ý: Bảng PriceItem phải có primary key (priceListId PK, packageId SK)
        var item = priceItemRepository.get(priceListId, pkg.getPackageId());
        return item != null ? defaultDouble(item.getAmount()) : 0.0;
    }

    /** Chọn promotion hợp lệ có % giảm lớn nhất (theo ngày + status) */
    private PromotionPackage pickBestPromotion(List<PromotionPackage> promos) {
        if (promos == null || promos.isEmpty()) return null;

        LocalDate today = LocalDate.now();

        return promos.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
                .filter(p -> {
                    Promotion promoParent = promotionRepository.findById(p.getPromotionId()).orElse(null);
                    if (promoParent == null) return false;

                    boolean active = "ACTIVE".equalsIgnoreCase(promoParent.getStatus());
                    boolean startOk = promoParent.getStartDate() == null || !today.isBefore(promoParent.getStartDate());
                    boolean endOk   = promoParent.getEndDate() == null   || !today.isAfter(promoParent.getEndDate());
                    return active && startOk && endOk;
                })
                .max(Comparator.comparingInt(PromotionPackage::getDiscountPercent))
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
