/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:24 PM
 */

import flim.backendcartoon.entities.DTO.request.AddPriceRequest;
import flim.backendcartoon.entities.DTO.request.CreatePriceListRequest;
import flim.backendcartoon.entities.DTO.request.PriceView;
import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.PriceItemRepository;
import flim.backendcartoon.repositories.PriceListRepository;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.PricingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PricingServiceImpl implements PricingService {
    private final PriceListRepository priceListRepository;
    private final PriceItemRepository priceItemRepository;
    private final SubscriptionPackageRepository subscriptionPackageRepository;

    public PricingServiceImpl(PriceListRepository priceListRepository,
                              PriceItemRepository priceItemRepository,
                              SubscriptionPackageRepository subscriptionPackageRepository) {
        this.priceListRepository = priceListRepository;
        this.priceItemRepository = priceItemRepository;
        this.subscriptionPackageRepository = subscriptionPackageRepository;
    }


    @Override
    public PriceView getPriceForPackage(String packageId) {
        // Lấy thông tin gói cước từ repository
        var subscriptionPackage = subscriptionPackageRepository.get(packageId);
        if (subscriptionPackage == null) {
            throw new ResourceNotFoundException("Subscription package not found for ID: " + packageId);
        }

        // Lấy thông tin bảng giá hiện tại (giả sử chỉ có một bảng giá hiện tại)
        String priceListId = subscriptionPackage.getCurrentPriceListId();
        if (priceListId == null || priceListId.isBlank()) {
            throw new ResourceNotFoundException("currentPriceListId is not set for package " + packageId);
        }

        // Lấy thông tin mục giá cho gói cước
        var priceItem = priceItemRepository.get(priceListId, packageId);
        if (priceItem == null) {
            throw new ResourceNotFoundException("No price item found for package ID: " + packageId);
        }

        PriceList list = priceListRepository.get(priceListId);

        // Tạo và trả về PriceView
        PriceView priceView = new PriceView();
        priceView.setPackageId(packageId);
        priceView.setPackageName(subscriptionPackage.getPackageName());
        priceView.setPackageImage(subscriptionPackage.getImageUrl());
        priceView.setAmount(priceItem.getAmount());
        priceView.setCurrency(priceItem.getCurrency());
        priceView.setPriceListId(priceListId);
        if (list != null) {
            priceView.setPriceListName(list.getName());
            priceView.setStartDate(list.getStartDate());
            priceView.setEndDate(list.getEndDate());
        }

        return priceView;
    }

    @Override
    public void createPriceList(CreatePriceListRequest priceListRequest) {
        PriceList priceList = new PriceList();
        priceList.setPriceListId(priceListRequest.getPriceListId());
        priceList.setName(priceListRequest.getName());
        priceList.setStartDate(priceListRequest.getStartDate());
        priceList.setEndDate(priceListRequest.getEndDate());
        priceList.setStatus(priceListRequest.getStatus());

        // Kiểm tra tính hợp lệ của khoảng thời gian
        if (priceList.getEndDate().isBefore(priceList.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Lưu bảng giá vào repository
        priceListRepository.save(priceList);
    }

    @Override
    public PriceList getPriceListById(String priceListId) {
        return priceListRepository.get(priceListId);
    }

    @Override
    public void createPriceItem(PriceItem priceItem) {
        PriceList list = priceListRepository.get(priceItem.getPriceListId());
        if (list == null) {
            throw new ResourceNotFoundException("PriceList not found for ID: " + priceItem.getPriceListId());
        }
        priceItemRepository.save(priceItem);
    }

    @Override
    public void addPrice(AddPriceRequest addPriceRequest) {
        PriceList list = priceListRepository.get(addPriceRequest.getPriceListId());
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + addPriceRequest.getPriceListId());

        LocalDate start = list.getStartDate();
        LocalDate end   = list.getEndDate();

        // 1) Tìm các bản giá cùng package có effectiveStart <= end
        var candidates = priceItemRepository.findPossibleOverlaps(addPriceRequest.getPackageId(), end);

        // 2) Lọc overlap thực sự: effectiveEnd >= start
        boolean hasOverlap = candidates.stream().anyMatch(it ->
                it.getPackageId().equals(addPriceRequest.getPackageId())
                        && !it.getPriceListId().equals(addPriceRequest.getPriceListId())
                        && !it.getEffectiveEnd().isBefore(start)
        );
        if (hasOverlap) {
            throw new IllegalStateException("Overlap price period for package " + addPriceRequest.getPackageId()
                    + " between " + start + " and " + end);
        }

        // 3) Ghi item (chặn trùng trong cùng priceList bằng condition)
        PriceItem item = new PriceItem();
        item.setPriceListId(addPriceRequest.getPriceListId());
        item.setPackageId(addPriceRequest.getPackageId());
        item.setAmount(addPriceRequest.getAmount());
        item.setEffectiveStart(start);
        item.setEffectiveEnd(end);

        priceItemRepository.putIfNotExists(item);
    }

    @Override
    public int activatePriceList(String priceListId) {
        PriceList list = priceListRepository.get(priceListId);
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + priceListId);

        // (khuyến nghị) chỉ cho activate khi status = ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(list.getStatus())) {
            throw new IllegalStateException("PriceList must be ACTIVE before activation: " + list.getStatus());
        }

        // Lấy tất cả packageId có trong PriceItem của priceListId
        List<String> packageIds = priceItemRepository.findPackageIdsByPriceListId(priceListId);
        if (packageIds.isEmpty()) return 0;

        int updated = 0;
        for (String packageId : packageIds) {
            SubscriptionPackage subscriptionPackage = subscriptionPackageRepository.get(packageId);
            if (packageId == null) continue;

            // Chỉ update khi khác để tránh ghi thừa
            if (!priceListId.equals(subscriptionPackage.getCurrentPriceListId())) {
                subscriptionPackage.setCurrentPriceListId(priceListId);
                subscriptionPackageRepository.save(subscriptionPackage);
                updated++;
            }
        }
        return updated;
    }
}
