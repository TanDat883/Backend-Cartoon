/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:41 PM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionType;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.PromotionPackageRepository;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.services.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;

    @Autowired
    public PromotionServiceImpl(PromotionRepository promotionRepository, PromotionPackageRepository promotionPackageRepository) {
        this.promotionRepository = promotionRepository;
    }


    @Override
    public void createPromotion(CreatePromotionRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        String promotionId = "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String promotionStatus = "ACTIVE";

        while (promotionRepository.findById(promotionId).isPresent()) {
            promotionId = "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Promotion promotion = Promotion.of(
                promotionId,
                request.getPromotionName(),
                request.getDescription(),
                request.getPromotionType(),
                request.getStartDate(),
                request.getEndDate(),
                promotionStatus
        );
        promotionRepository.save(promotion);
    }

    @Override
    public Promotion getPromotionById(String promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new BaseException("Promotion with id " + promotionId + " not found"));
    }

    @Override
    public void updateStatus(String promotionId, String status) {
        Promotion promotion = getPromotionById(promotionId);
        promotion.setStatus(status);
        promotionRepository.save(promotion);
    }

    @Override
    public void updateDates(String promotionId, LocalDate start, LocalDate end) {

    }

    @Override
    public List<Promotion> listActive() {
        return List.of();
    }

    @Override
    public void delete(String promotionId) {

    }

    @Override
    public List<Promotion> listByType(PromotionType type) {
        return promotionRepository.findByType(type.toString());
    }

    @Override
    public List<Promotion> listAll() {
        return promotionRepository.findAll();
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("startDate/endDate is required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }
    }
}
