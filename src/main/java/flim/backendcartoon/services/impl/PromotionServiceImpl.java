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
import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.services.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Autowired
    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }


    @Override
    public void createPromotion(CreatePromotionRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Promotion promotion = Promotion.of(
                request.getPromotionId(),
                request.getPromotionName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
                );
        promotionRepository.save(promotion);
    }

    @Override
    public void updatePromotion(String promotionId, CreatePromotionRequest request) {
        Promotion existingPromotion = promotionRepository.get(promotionId);
        if (existingPromotion == null) {
            throw new IllegalArgumentException("Promotion with ID " + promotionId + " does not exist.");
        }
        validateDates(request.getStartDate(), request.getEndDate());

        existingPromotion.setPromotionName(request.getPromotionName());
        existingPromotion.setDescription(request.getDescription());
        existingPromotion.setStartDate(request.getStartDate());
        existingPromotion.setEndDate(request.getEndDate());
        existingPromotion.setStatus(request.getStatus());

        promotionRepository.save(existingPromotion);
    }

    @Override
    public Page<Promotion> listAll(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<Promotion> promotions;
        long total;

        if (keyword != null && !keyword.isEmpty()) {
            promotions = promotionRepository.findByKeyword(keyword, pageable);
            total = promotionRepository.countByKeyword(keyword);
        } else {
            promotions = promotionRepository.findAllPromotions(pageable);
            total = promotionRepository.countAllPromotions();
        }

        return new PageImpl<>(promotions, pageable, total);
    }

    @Override
    public void expireOutdatedPromotions() {
        List<Promotion> promotions = promotionRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Promotion promo : promotions) {
            if (promo.getEndDate().isBefore(today) && !promo.getStatus().equals("EXPIRED")) {
                promo.setStatus("EXPIRED");
                promotionRepository.save(promo);
            }
        }
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
