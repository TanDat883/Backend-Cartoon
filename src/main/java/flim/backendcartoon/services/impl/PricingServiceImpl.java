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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<PriceList> getAllPriceLists() {
        return priceListRepository.getAll();
    }

    @Override
    public void createPriceList(CreatePriceListRequest priceListRequest) {
        PriceList priceList = new PriceList();
        String priceListId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String priceListStatus = "ACTIVE";
        priceList.setPriceListId(priceListId);
        priceList.setName(priceListRequest.getName());
        priceList.setStartDate(priceListRequest.getStartDate());
        priceList.setEndDate(priceListRequest.getEndDate());
        priceList.setStatus(priceListStatus);
        priceList.setCreatedAt(LocalDate.now());

        // Kiểm tra tính hợp lệ của khoảng thời gian
        if (priceList.getEndDate().isBefore(priceList.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Lưu bảng giá vào repository
        priceListRepository.save(priceList);
    }

    @Override
    public List<PriceList> getPriceListsByStatusAndStartDate(String status, LocalDate startDate) {
        return priceListRepository.findByStatusAndStartDate(status, startDate);
    }

    @Override
    @Transactional
    public int expireOutdatedPriceLists() {
        // Dùng đúng timezone hệ thống của bạn
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        // Chỉ lấy các list có endDate < today và chưa EXPIRED
        List<PriceList> expired = priceListRepository.findByEndDateBeforeAndStatusNot(today, "EXPIRED");

        int clearedPkgs = 0;

        for (PriceList pl : expired) {
            final String listId = pl.getPriceListId();

            pl.setStatus("EXPIRED");
            priceListRepository.save(pl);

            List<SubscriptionPackage> pkgs = subscriptionPackageRepository.findByCurrentPriceListId(listId);
            for (SubscriptionPackage sp : pkgs) {
                if (listId.equals(sp.getCurrentPriceListId())) {
                    sp.setCurrentPriceListId(null);
                    subscriptionPackageRepository.save(sp);
                    clearedPkgs++;
                }
            }
        }
        return clearedPkgs;
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
    @Transactional
    public int activatePriceList(String priceListId) {
        PriceList list = priceListRepository.get(priceListId);
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + priceListId);

        if (!"ACTIVE".equalsIgnoreCase(list.getStatus())) {
            throw new IllegalStateException("PriceList must be ACTIVE before activation: " + list.getStatus());
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        boolean listCoversToday =
                !today.isBefore(list.getStartDate()) &&
                        (list.getEndDate() == null || !today.isAfter(list.getEndDate()));
        if (!listCoversToday) return 0;

        List<String> pkgIdsCoveredToday =
                priceItemRepository.findPackageIdsCoveringDate(priceListId, today);
        if (pkgIdsCoveredToday.isEmpty()) return 0;

        int updated = 0;
        for (String packageId : pkgIdsCoveredToday) {
            if (packageId == null) continue;

            SubscriptionPackage sp = subscriptionPackageRepository.get(packageId);
            if (sp == null) continue;

            // Chỉ update khi khác để tránh ghi thừa
            if (!priceListId.equals(sp.getCurrentPriceListId())) {
                sp.setCurrentPriceListId(priceListId);
                subscriptionPackageRepository.save(sp);
                updated++;
            }
        }
        return updated;
    }


    @Override
    public List<PriceItem> getPriceItemsByPriceListId(String priceListId) {
        PriceList list = priceListRepository.get(priceListId);
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + priceListId);

        // Lấy tất cả packageId có trong PriceItem của priceListId
        List<String> packageIds = priceItemRepository.findPackageIdsByPriceListId(priceListId);
        if (packageIds.isEmpty()) return List.of();

        return packageIds.stream()
                .map(pkgId -> priceItemRepository.get(priceListId, pkgId))
                .toList();
    }

    @Override
    public void updateEffectiveEndOfCurrentPriceItem(String priceListId, String packageId, LocalDate newEndDate) {
        PriceList list = priceListRepository.get(priceListId);
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + priceListId);

        SubscriptionPackage subscriptionPackage = subscriptionPackageRepository.get(packageId);
        if (subscriptionPackage == null) {
            throw new ResourceNotFoundException("Subscription package not found for ID: " + packageId);
        }
        String currentPriceListId = subscriptionPackage.getCurrentPriceListId();
        if (currentPriceListId == null || currentPriceListId.isBlank()) {
            throw new ResourceNotFoundException("currentPriceListId is not set for package " + packageId);
        }
        if (!currentPriceListId.equals(priceListId)) {
            throw new IllegalStateException("The provided priceListId is not the current one for package " + packageId);
        }
        if (newEndDate.isBefore(list.getStartDate()) || newEndDate.isAfter(list.getEndDate())) {
            throw new IllegalArgumentException("newEndDate must be within the range of the PriceList's start and end dates");
        }

        LocalDate start = list.getStartDate();
        LocalDate end   = list.getEndDate();

        // 1) Tìm các bản giá cùng package có effectiveStart <= end
        var candidates = priceItemRepository.findPossibleOverlaps(packageId, end);

        // 2) Lọc overlap thực sự: effectiveEnd >= start
        boolean hasOverlap = candidates.stream().anyMatch(it ->
                it.getPackageId().equals(packageId)
                        && !it.getPriceListId().equals(priceListId)
                        && !it.getEffectiveEnd().isBefore(start)
        );
        if (hasOverlap) {
            throw new IllegalStateException("Overlap price period for package " + packageId
                    + " between " + start + " and " + end);
        }

        priceItemRepository.updateEffectiveEnd(priceListId, packageId, newEndDate);
    }

    @Transactional
    public void extendPriceListEnd(String priceListId, LocalDate newEndDate, boolean carryForwardMissing) {
        PriceList list = priceListRepository.get(priceListId);
        if (list == null) throw new IllegalArgumentException("PriceList not found: " + priceListId);

        LocalDate oldStart = list.getStartDate();
        LocalDate oldEnd   = list.getEndDate();

        if (newEndDate.isBefore(oldStart) || !newEndDate.isAfter(oldEnd)) {
            throw new IllegalArgumentException("newEndDate must be > current end and >= start");
        }

        // 1) Lấy toàn bộ items của list này
        List<PriceItem> items = priceItemRepository.findByPriceList(priceListId);

        // 2) Kéo dài các item đang chạm biên cũ (end == oldEnd), chống overlap với "next"
        for (PriceItem it : items) {
            if (!it.getEffectiveEnd().isEqual(oldEnd)) continue;

            LocalDate proposedEnd = newEndDate;

            PriceItem next = priceItemRepository.findNextByStart(
                    it.getPackageId(), it.getEffectiveStart(), priceListId);

            if (next != null && !next.getEffectiveStart().isAfter(proposedEnd)) {
                proposedEnd = next.getEffectiveStart().minusDays(1); // SNAP
            }

            if (proposedEnd.isBefore(it.getEffectiveStart())) {
                throw new IllegalStateException(
                        "Cannot extend package " + it.getPackageId() +
                                " beyond next start " + (next != null ? next.getEffectiveStart() : "N/A"));
            }

            priceItemRepository.updateEffectiveEnd(priceListId, it.getPackageId(), proposedEnd);
        }

        // 3) (Tuỳ chọn) Carry-forward cho các package chưa có item ở list này
        if (carryForwardMissing) {
            var allPkgs = subscriptionPackageRepository.findAll(); // hoặc danh sách mong muốn
            var pkgInList = items.stream().map(PriceItem::getPackageId).collect(Collectors.toSet());

            for (SubscriptionPackage sp : allPkgs) {
                if (pkgInList.contains(sp.getPackageId())) continue;

                PriceItem last = priceItemRepository.findLastBefore(sp.getPackageId(), oldEnd, priceListId);
                if (last == null) continue; // không có giá quá khứ để carry

                LocalDate newStart = oldEnd.plusDays(1);
                LocalDate newEnd   = newEndDate;

                PriceItem next = priceItemRepository.findNextByStart(sp.getPackageId(), newStart.minusDays(1), priceListId);
                if (next != null && !next.getEffectiveStart().isAfter(newEnd)) {
                    newEnd = next.getEffectiveStart().minusDays(1);
                }
                if (!newEnd.isBefore(newStart)) {
                    PriceItem carry = new PriceItem();
                    carry.setPriceListId(priceListId);
                    carry.setPackageId(sp.getPackageId());
                    carry.setAmount(last.getAmount());
                    carry.setCurrency(last.getCurrency());
                    carry.setEffectiveStart(newStart);
                    carry.setEffectiveEnd(newEnd);

                    priceItemRepository.putIfNotExists(carry);
                }
            }
        }

        // 4) Cập nhật end của PriceList
        list.setEndDate(newEndDate);
        priceListRepository.save(list);
    }

}
