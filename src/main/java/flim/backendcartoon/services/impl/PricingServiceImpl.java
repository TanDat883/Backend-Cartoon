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
import flim.backendcartoon.entities.User;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.PriceItemRepository;
import flim.backendcartoon.repositories.PriceListRepository;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.PricingService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
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
    public Page<PriceList> getAllPriceLists(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<PriceList> priceList;
        long total;

        if (keyword != null && !keyword.isEmpty()) {
            priceList = priceListRepository.findByKeyword(keyword, pageable);
            total = priceListRepository.countByKeyword(keyword);
        } else {
            priceList = priceListRepository.findAllPriceList(pageable);
            total = priceListRepository.countAllPriceList();
        }

        return new PageImpl<>(priceList, pageable, total);
    }

    @Override
    public void createPriceList(CreatePriceListRequest priceListRequest) {
        if (priceListRepository.get(priceListRequest.getId()) != null) {
            throw new IllegalArgumentException("PriceList ID already exists: " + priceListRequest.getId());
        }
        PriceList priceList = new PriceList();
        priceList.setPriceListId(priceListRequest.getId());
        priceList.setName(priceListRequest.getName());
        priceList.setStatus(priceListRequest.getStatus());
        priceList.setStartDate(priceListRequest.getStartDate());
        priceList.setEndDate(priceListRequest.getEndDate());
        priceList.setCreatedAt(LocalDate.now());

        // Kiểm tra tính hợp lệ của khoảng thời gian
        if (priceList.getEndDate().isBefore(priceList.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Lưu bảng giá vào repository
        priceListRepository.save(priceList);
    }

    @Override
    public void updatePriceList(String priceListId, CreatePriceListRequest req) {
        PriceList existing = priceListRepository.get(priceListId);
        if (existing == null) {
            throw new ResourceNotFoundException("PriceList not found for ID: " + priceListId);
        }

        if ("EXPIRED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("EXPIRED price list cannot be edited");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        // New values
        String newName     = req.getName();
        String newStatus   = req.getStatus();
        LocalDate newStart = req.getStartDate();
        LocalDate newEnd   = req.getEndDate();

        if (newName == null || newStatus == null || newStart == null || newEnd == null) {
            throw new IllegalArgumentException("name/status/startDate/endDate must not be null");
        }

        if (newEnd.isBefore(today)) {
            throw new IllegalArgumentException("End date must be today or in the future");
        }

        boolean hasStarted = !today.isBefore(existing.getStartDate()); // today >= existing.startDate
        if (hasStarted) {
            if (!newStart.equals(existing.getStartDate())) {
                throw new IllegalStateException("Cannot change startDate after the price list has started");
            }
        } else {
            // Not started yet: allow change but start must be >= today
            if (newStart.isBefore(today)) {
                throw new IllegalArgumentException("Start date must be today or in the future");
            }
        }

        if (newEnd.isBefore(newStart)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // ====== BỔ SUNG: CHẶN OVERLAP THEO PACKAGE KHI UPDATE ======
        // Lấy danh sách package của PriceList hiện tại
        java.util.Set<String> currentPkgIds =
                new java.util.HashSet<>(priceItemRepository.findPackageIdsByPriceListId(priceListId));

        if (!currentPkgIds.isEmpty()) {
            for (PriceList other : priceListRepository.getAll()) {
                if (other == null) continue;
                if (priceListId.equals(other.getPriceListId())) continue; // bỏ qua chính nó

                // Kiểm tra giao nhau khoảng ngày: [newStart, newEnd] ∩ [oStart, oEnd] ≠ ∅
                LocalDate oStart = other.getStartDate();
                LocalDate oEnd   = other.getEndDate();
                boolean overlapRange = !(newEnd.isBefore(oStart) || oEnd.isBefore(newStart));
                if (!overlapRange) continue;

                // Nếu giao ngày, kiểm tra giao package
                java.util.List<String> otherPkgIds = priceItemRepository.findPackageIdsByPriceListId(other.getPriceListId());
                for (String pid : otherPkgIds) {
                    if (currentPkgIds.contains(pid)) {
                        // Gặp package trùng ở list khác trong khoảng giao ngày -> cấm
                        throw new IllegalStateException(
                                "Date range overlaps with price list " + other.getPriceListId()
                                        + " for package " + pid
                                        + " ([" + newStart + " ~ " + newEnd + "] vs [" + oStart + " ~ " + oEnd + "])"
                        );
                    }
                }
            }
        }

        // Apply & save
        existing.setName(newName);
        existing.setStatus(newStatus);
        existing.setStartDate(newStart);
        existing.setEndDate(newEnd);

        priceListRepository.save(existing);
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

//    @Override
//    @Transactional
//    public int autoFlipInactiveListsStartingToday() {
//        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
//        LocalDate today = LocalDate.now(zone);
//
//        // Lấy các list đang INACTIVE và có startDate == hôm nay
//        List<PriceList> toFlip = priceListRepository.findByStatusAndStartDate("INACTIVE", today);
//        int listsActivated = 0;
//
//        for (PriceList pl : toFlip) {
//            // Đổi trạng thái sang ACTIVE
//            pl.setStatus("ACTIVE");
//            priceListRepository.save(pl);
//
//            // Gán vào currentPriceListId cho các package thuộc list này (nếu cover hôm nay)
//            try {
//                activatePriceList(pl.getPriceListId());
//            } catch (Exception ex) {
//                System.err.println("Failed to activate packages for list " + pl.getPriceListId() + ": " + ex.getMessage());
//            }
//
//            listsActivated++;
//        }
//
//        return listsActivated;
//    }


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
    public void addPrice(AddPriceRequest req) {
        PriceList currentList = priceListRepository.get(req.getPriceListId());
        if (currentList == null) throw new IllegalArgumentException("PriceList not found: " + req.getPriceListId());

        final LocalDate newStart = currentList.getStartDate();
        final LocalDate newEnd   = currentList.getEndDate();

        // Lấy toàn bộ PriceItem cùng package trên các PriceList khác
        List<PriceItem> samePkgItems = priceItemRepository.findByPackageId(req.getPackageId());

        // Kiểm tra giao nhau khoảng ngày (dựa trên PriceList của từng item)
        for (PriceItem it : samePkgItems) {
            // bỏ qua chính list hiện tại (trường hợp add lại)
            if (req.getPriceListId().equals(it.getPriceListId())) continue;

            PriceList otherList = priceListRepository.get(it.getPriceListId());
            if (otherList == null) continue; // đề phòng dữ liệu mồ côi

            LocalDate oStart = otherList.getStartDate();
            LocalDate oEnd   = otherList.getEndDate();

            // Overlap nếu: [newStart, newEnd] ∩ [oStart, oEnd] ≠ ∅
            boolean overlap = !newStart.isAfter(oEnd) && !oStart.isAfter(newEnd);
            if (overlap) {
                throw new IllegalStateException(
                        "Overlapped price period for package " + req.getPackageId() +
                                " between list " + req.getPriceListId() + " [" + newStart + " ~ " + newEnd + "]" +
                                " and list " + it.getPriceListId() + " [" + oStart + " ~ " + oEnd + "]"
                );
            }
        }

        PriceItem item = new PriceItem();
        item.setPriceListId(req.getPriceListId());
        item.setPackageId(req.getPackageId());
        item.setAmount(req.getAmount());
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

        // Vì PriceItem không có khoảng ngày, "package được cover hôm nay" = toàn bộ item của list này
        List<String> pkgIds = priceItemRepository.findPackageIdsByPriceListId(priceListId);
        if (pkgIds.isEmpty()) return 0;

        int updated = 0;
        for (String packageId : pkgIds) {
            if (packageId == null) continue;
            SubscriptionPackage sp = subscriptionPackageRepository.get(packageId);
            if (sp == null) continue;

            if (!priceListId.equals(sp.getCurrentPriceListId())) {
                sp.setCurrentPriceListId(priceListId);
                subscriptionPackageRepository.save(sp);
                updated++;
            }
        }

        // Với các package đang trỏ vào list này nhưng KHÔNG còn item trong list → bỏ trỏ
        List<SubscriptionPackage> pointing = subscriptionPackageRepository.findByCurrentPriceListId(priceListId);
        for (SubscriptionPackage sp : pointing) {
            if (!pkgIds.contains(sp.getPackageId())) {
                sp.setCurrentPriceListId(null);
                subscriptionPackageRepository.save(sp);
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

}
