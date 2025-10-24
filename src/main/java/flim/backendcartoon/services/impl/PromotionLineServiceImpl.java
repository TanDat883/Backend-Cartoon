/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:52 AM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionLineRequest;
import flim.backendcartoon.entities.PromotionLine;
import flim.backendcartoon.repositories.PromotionLineRepository;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.services.PromotionLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromotionLineServiceImpl implements PromotionLineService {
    private final PromotionLineRepository promotionLineRepository;

    @Autowired
    public PromotionLineServiceImpl(PromotionLineRepository promotionLineRepository) {
        this.promotionLineRepository = promotionLineRepository;
    }


    @Override
    public void createPromotionLine(CreatePromotionLineRequest request) {

        PromotionLine line = PromotionLine.of(
                request.getPromotionId(),
                request.getPromotionLineId(),
                request.getPromotionLineType(),
                request.getPromotionLineName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
        );
        promotionLineRepository.save(line);
    }

    @Override
    public void updatePromotionLine(String promotionId, String promotionLineId, CreatePromotionLineRequest request) {
        PromotionLine existingLine = promotionLineRepository.get(promotionId, promotionLineId);
        if (existingLine == null) {
            throw new IllegalArgumentException("Promotion Line with ID " + promotionLineId + " does not exist.");
        }

        existingLine.setPromotionLineType(request.getPromotionLineType());
        existingLine.setPromotionLineName(request.getPromotionLineName());
        if (request.getStartDate() != null) {
            existingLine.setStartDate(request.getStartDate());
        }
//        existingLine.setStartDate(request.getStartDate());
        existingLine.setEndDate(request.getEndDate());
        existingLine.setStatus(request.getStatus());

        promotionLineRepository.update(existingLine);
    }

    @Override
    public List<PromotionLine> getPromotionLinesByPromotion(String promotionId) {
        return promotionLineRepository.listByPromotionId(promotionId);
    }

    @Override
    public void expireOutdatedPromotionLines() {
        List<PromotionLine> allLines = promotionLineRepository.listAll();
        LocalDate today = LocalDate.now();

        for (PromotionLine line : allLines) {
            if (line.getEndDate().isBefore(today) && !"EXPIRED".equals(line.getStatus())) {
                line.setStatus("EXPIRED");
                promotionLineRepository.update(line);
            }
        }
    }
}
