package flim.backendcartoon.services;

import flim.backendcartoon.entities.DTO.response.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportDashboardService {
    private final DataAnalyzerService dataAnalyzerService;

    public ExportDashboardService(DataAnalyzerService dataAnalyzerService) {
        this.dataAnalyzerService = dataAnalyzerService;
    }

    /** Xuất theo khoảng ngày + groupBy, layout kế toán, hai bảng song song */
    public void exportDashboardRange(HttpServletResponse response,
                                     LocalDate start,
                                     LocalDate end,
                                     GroupByDataAnalzerResponse groupBy,
                                     String companyName,
                                     String companyAddress,
                                     boolean includePromotions,
                                     int topVoucherLimit) throws IOException {

        // --- Lấy dữ liệu doanh thu ---
        RevenueSummaryResponse summary = dataAnalyzerService.getRevenueSummaryByRange(start, end);
        RevenueChartResponse chart = dataAnalyzerService.getRevenueByRange(start, end, groupBy);
        PagedResponse<RecentTransactionResponse> paged =
                dataAnalyzerService.getRecentTransactionsPaged(1, 5000, start, end);
        List<RecentTransactionResponse> txs = paged.getItems();

        // tên file tùy theo có kèm CTKM hay không
        String fileName = (includePromotions ? "BaoCao_DoanhThu_CTKM_" : "BaoCao_DoanhThu_")
                + start + "_" + end + "_" + groupBy + ".xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ===== SHEET CHÍNH: Bảng kê Doanh thu =====
            XSSFSheet sheet = wb.createSheet("Bảng kê Doanh thu");
            sheet.setDisplayGridlines(false);
            sheet.setPrintGridlines(false);

            PrintSetup ps = sheet.getPrintSetup();
            ps.setLandscape(true);
            ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.setMargin(Sheet.LeftMargin, 0.4);
            sheet.setMargin(Sheet.RightMargin, 0.4);
            sheet.setMargin(Sheet.TopMargin, 0.6);
            sheet.setMargin(Sheet.BottomMargin, 0.6);

            int r = 0;

            Row r1 = sheet.createRow(r++);
            set(sheet, r1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
            sheet.addMergedRegion(new CellRangeAddress(0,0,0,9));

            Row r2 = sheet.createRow(r++);
            set(sheet, r2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(1,1,0,9));

            Row r3 = sheet.createRow(r++);
            set(sheet, r3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(2,2,0,9));

            r++;
            Row title = sheet.createRow(r++);
            set(sheet, title, 0, "BẢNG KÊ DOANH THU", st.title);
            sheet.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, 9));

            Row range = sheet.createRow(r++);
            set(sheet, range, 0,
                    "Từ ngày: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "     Đến ngày: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "     (Nhóm theo: " + groupBy + ")", st.centerGrey);
            sheet.addMergedRegion(new CellRangeAddress(range.getRowNum(), range.getRowNum(), 0, 9));

            r++;

            Row s1 = sheet.createRow(r++);
            set(sheet, s1, 0, "Tổng doanh thu", st.th);
            setNum(sheet, s1, 1, nz(summary.getTotalRevenue()), st.moneyRight);

            Row s2 = sheet.createRow(r++);
            set(sheet, s2, 0, "Doanh thu (khoảng)", st.th);
            setNum(sheet, s2, 1, nz(summary.getMonthlyRevenue()), st.moneyRight);

            Row s3 = sheet.createRow(r++);
            set(sheet, s3, 0, "Tổng giao dịch", st.th);
            setNum(sheet, s3, 1, nz(summary.getTotalTransactions()), st.tdRight);

            Row s4 = sheet.createRow(r++);
            set(sheet, s4, 0, "GD (khoảng)", st.th);
            setNum(sheet, s4, 1, nz(summary.getMonthlyTransactions()), st.tdRight);

            st.addBoxBorder(sheet, s1.getRowNum(), s4.getRowNum(), 0, 1);

            r++;

            // ====== PHẦN II. BẢNG KÊ CHI TIẾT (gộp theo Người dùng) ======
            r++; // khoảng trắng
            Row sec = sheet.createRow(r++);
            set(sheet, sec, 0, "II. BẢNG KÊ GIAO DỊCH THEO NGƯỜI DÙNG", st.section);
            sheet.addMergedRegion(new CellRangeAddress(sec.getRowNum(), sec.getRowNum(), 0, 6));

// Header chi tiết
            Row head = sheet.createRow(r++);
            int col = 0;
            set(sheet, head, col++, "STT",        st.header);
            set(sheet, head, col++, "Người dùng", st.header);
            set(sheet, head, col++, "Mã đơn",     st.header);
            set(sheet, head, col++, "Ngày",       st.header);
            set(sheet, head, col++, "Gói",        st.header);
            set(sheet, head, col++, "Số tiền (VND)", st.header);
            set(sheet, head, col++, "Trạng thái", st.header);

            int tableHeaderRow = head.getRowNum();

// Gom nhóm theo Người dùng (giống “NVBH” ở mẫu)
            java.util.Map<String, java.util.List<RecentTransactionResponse>> byUser =
                    new java.util.LinkedHashMap<>();
            if (txs != null) {
                for (RecentTransactionResponse t : txs) {
                    String k = (t.getUserName()==null || t.getUserName().isBlank()) ? "(không rõ)" : t.getUserName();
                    byUser.computeIfAbsent(k, _k -> new java.util.ArrayList<>()).add(t);
                }
            }

// Ghi dữ liệu nhóm
            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int runningNo = 1;
            long grand = 0;

            for (var entry : byUser.entrySet()) {
                String user = entry.getKey();
                java.util.List<RecentTransactionResponse> list = entry.getValue();

                // Dòng tiêu đề nhóm (cột 0 hiển thị số thứ tự nhóm)
                Row grp = sheet.createRow(r++);
                set(sheet, grp, 0, String.valueOf(runningNo++), st.tdCenter);
                set(sheet, grp, 1, user, st.th); // in đậm giống mẫu

                long subtotal = 0;
                // Chi tiết từng giao dịch
                for (RecentTransactionResponse t : list) {
                    Row row = sheet.createRow(r++);
                    int cc = 0;
                    set(sheet, row, cc++, "", st.td); // STT nhóm (để trống, giống mẫu)
                    set(sheet, row, cc++, user, st.td); // lặp lại cho dễ lọc
                    set(sheet, row, cc++, "#" + safe(t.getOrderId()), st.td);
                    set(sheet, row, cc++, t.getCreatedAt()==null? "" : t.getCreatedAt().format(dFmt), st.tdCenter);
                    set(sheet, row, cc++, safe(t.getPackageId()), st.td);
                    setNum(sheet, row, cc++, nz(t.getFinalAmount()), st.moneyRight);
                    set(sheet, row, cc++, safe(t.getStatus()), st.tdCenter);

                    subtotal += (long) nz(t.getFinalAmount());
                    if ((r % 2) == 0) st.paintZebra(row, 0, 6);
                }

                // Dòng "Tổng cộng (tên nhóm)"
                Row sub = sheet.createRow(r++);
                set(sheet, sub, 1, "Tổng cộng (" + user + ")", st.totalLeft);
                setNum(sheet, sub, 5, (double) subtotal, st.totalRight);

                grand += subtotal;

                // Khoảng trắng giữa các nhóm
                r++;
            }

// Dòng GRAND TOTAL (đậm, viền trên dày)
            Row grandRow = sheet.createRow(r++);
            set(sheet, grandRow, 1, "TỔNG CỘNG", st.grandLeft);
            setNum(sheet, grandRow, 5, grand,   st.grandRight);

// Kẻ khung cho toàn bộ bảng (từ header đến dòng grand)
            st.addBoxBorder(sheet, tableHeaderRow, grandRow.getRowNum(), 0, 6);

// Freeze header
            sheet.createFreezePane(0, 0);
// Width cột hợp lý A4
            sheet.setColumnWidth(0, 6*256);   // STT nhóm
            sheet.setColumnWidth(1, 26*256);  // Người dùng
            sheet.setColumnWidth(2, 20*256);  // Mã đơn
            sheet.setColumnWidth(3, 12*256);  // Ngày
            sheet.setColumnWidth(4, 14*256);  // Gói
            sheet.setColumnWidth(5, 16*256);  // Số tiền
            sheet.setColumnWidth(6, 12*256);  // Trạng thái

            wb.setPrintArea(wb.getSheetIndex(sheet), 0, 6, 0, grandRow.getRowNum());
            sheet.setRepeatingRows(new CellRangeAddress(tableHeaderRow, tableHeaderRow, 0, 6));

            r += 1;
            Row notesTitle = sheet.createRow(r++);
            set(sheet, notesTitle, 0, "Ghi chú", st.th);
            sheet.addMergedRegion(new CellRangeAddress(notesTitle.getRowNum(), notesTitle.getRowNum(), 0, 6));

            Row notesRow = sheet.createRow(r++);
            String ghiChu =
                    "- Báo cáo liệt kê giao dịch theo người dùng trong khoảng ngày đã chọn.\n"
                            + "- Số tiền là giá trị sau giảm (finalAmount).\n"
                            + "- Dòng 'Tổng cộng (Tên người dùng)' là tổng giao dịch của người đó.\n"
                            + "- Dòng 'TỔNG CỘNG' là tổng toàn bộ báo cáo.";
            set(sheet, notesRow, 0, ghiChu, st.notes);
            sheet.addMergedRegion(new CellRangeAddress(notesRow.getRowNum(), notesRow.getRowNum()+3, 0, 6));



            // ===== SHEET phụ: Revenue Chart =====
            if (chart.getLabels()!=null && !chart.getLabels().isEmpty()) {
                XSSFSheet sChart = wb.createSheet("Revenue Chart");
                int rr = 0;
                Row hr1 = sChart.createRow(rr++); set(sChart, hr1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
                sChart.addMergedRegion(new CellRangeAddress(0,0,0,11));
                Row hr2 = sChart.createRow(rr++); set(sChart, hr2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
                sChart.addMergedRegion(new CellRangeAddress(1,1,0,11));
                Row hr3 = sChart.createRow(rr++); set(sChart, hr3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
                sChart.addMergedRegion(new CellRangeAddress(2,2,0,11));

                rr++;
                Row t = sChart.createRow(rr++);
                set(sChart, t, 0, "BIỂU ĐỒ DOANH THU THEO " + groupBy, st.title);
                sChart.addMergedRegion(new CellRangeAddress(t.getRowNum(), t.getRowNum(), 0, 11));

                Row rg = sChart.createRow(rr++);
                set(sChart, rg, 0, "Từ: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        "   Đến: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
                sChart.addMergedRegion(new CellRangeAddress(rg.getRowNum(), rg.getRowNum(), 0, 11));
                rr++;

                // bảng dữ liệu
                Row hd = sChart.createRow(rr++);
                set(sChart, hd, 0, "Nhóm", st.header);
                set(sChart, hd, 1, "Doanh thu", st.header);
                int dataStart = rr;
                for (int i = 0; i < chart.getLabels().size(); i++) {
                    Row row = sChart.createRow(rr++);
                    set(sChart, row, 0, chart.getLabels().get(i), st.td);
                    setNum(sChart, row, 1, nz(chart.getData().get(i)), st.moneyRight);
                }

                // vẽ chart (neo dưới bảng)
                XSSFDrawing drawing = sChart.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0,0,0,0, 3, dataStart, 11, dataStart + 22);
                XSSFChart xssfChart = drawing.createChart(anchor);
                xssfChart.setTitleText("Doanh thu theo " + groupBy);
                xssfChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);
                XDDFCategoryAxis bottom = xssfChart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis left = xssfChart.createValueAxis(AxisPosition.LEFT);
                bottom.crossAxis(left); left.crossAxis(bottom);

                int last = chart.getLabels().size();
                var xs = XDDFDataSourcesFactory.fromStringCellRange(sChart, new CellRangeAddress(dataStart, dataStart + last - 1, 0, 0));
                var ys = XDDFDataSourcesFactory.fromNumericCellRange(sChart, new CellRangeAddress(dataStart, dataStart + last - 1, 1, 1));
                XDDFChartData data = xssfChart.createData(ChartTypes.BAR, bottom, left);
                ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
                data.addSeries(xs, ys).setTitle("Doanh thu (VND)", null);
                xssfChart.plot(data);

                sChart.setDisplayGridlines(false);
                sChart.setColumnWidth(0, 20*256);
                sChart.setColumnWidth(1, 16*256);
            }


            // ====== GỘP CTKM (nếu bật) ======
            if (includePromotions) {
                // Lấy dữ liệu CTKM
                PromoStatsSummaryResponse pSummary = dataAnalyzerService.getPromotionSummary(start, end);
                List<PromotionLineStatsResponse> lineStats =
                        dataAnalyzerService.getPromotionLineStats(start, end, null);
                List<VoucherUsageItemResponse> topVouchers =
                        dataAnalyzerService.getVoucherLeaderboard(start, end, Math.max(1, topVoucherLimit));
                PromotionRangeChartResponse usageChart =
                        dataAnalyzerService.getPromotionUsageByRange(start, end, GroupByDataAnalzerResponse.DAY);

                // ---- ONE SHEET: CTKM (dọc) ----
                XSSFSheet ctkmSheet  = wb.createSheet("CTKM");
                ctkmSheet .setDisplayGridlines(false);
                ctkmSheet .setPrintGridlines(false);
                var psSetup = ctkmSheet .getPrintSetup();
                psSetup.setLandscape(true);
                psSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
                ctkmSheet .setMargin(Sheet.LeftMargin, 0.4);
                ctkmSheet .setMargin(Sheet.RightMargin, 0.4);
                ctkmSheet .setMargin(Sheet.TopMargin, 0.6);
                ctkmSheet .setMargin(Sheet.BottomMargin, 0.6);

                int r0 = 0;

                // Header chung
                Row hr1 = ctkmSheet .createRow(r0++);
                set(ctkmSheet, hr1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
                ctkmSheet.addMergedRegion(new CellRangeAddress(0,0,0,11));

                Row hr2 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, hr2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
                ctkmSheet.addMergedRegion(new CellRangeAddress(1,1,0,11));

                Row hr3 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, hr3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
                ctkmSheet.addMergedRegion(new CellRangeAddress(2,2,0,11));

                r0++;
                Row t1 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, t1, 0, "BÁO CÁO TỔNG KẾT CTKM", st.title);
                ctkmSheet.addMergedRegion(new CellRangeAddress(t1.getRowNum(), t1.getRowNum(), 0, 11));

                Row rg = ctkmSheet.createRow(r0++);
                set(ctkmSheet, rg, 0,
                        "Thời gian: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + " → " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
                ctkmSheet.addMergedRegion(new CellRangeAddress(rg.getRowNum(), rg.getRowNum(), 0, 11));
                r0++;

                Row sec1 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, sec1, 0, "I. TỔNG QUAN CTKM", st.section);
                ctkmSheet.addMergedRegion(new CellRangeAddress(sec1.getRowNum(), sec1.getRowNum(), 0, 11));

                // --- 1) SUMMARY BOX ---
                Row sumA  = ctkmSheet.createRow(r0++); set(ctkmSheet, sumA, 0, "Tổng lượt áp dụng (redemptions)", st.th); setNum(ctkmSheet, sumA, 1, nz(pSummary.getTotalRedemptions()), st.tdRight);
                Row sumB = ctkmSheet.createRow(r0++); set(ctkmSheet, sumB, 0, "Số user dùng CTKM (unique)",        st.th); setNum(ctkmSheet, sumB, 1, nz(pSummary.getUniqueUsers()),     st.tdRight);
                Row sumC  = ctkmSheet.createRow(r0++); set(ctkmSheet, sumC, 0, "Tổng giảm giá (VND)",               st.th); setNum(ctkmSheet, sumC, 1, nz(pSummary.getTotalDiscountGranted()), st.moneyRight);
                Row sumD  = ctkmSheet.createRow(r0++); set(ctkmSheet, sumD, 0, "Doanh thu sau giảm (VND)",          st.th); setNum(ctkmSheet, sumD, 1, nz(pSummary.getTotalFinalAmount()),     st.moneyRight);
                st.addBoxBorder(ctkmSheet, sumA.getRowNum(), sumD.getRowNum(), 0, 1);

                r0 += 2;


                r0++; // khoảng trắng
                Row sec2 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, sec2, 0, "II. THỐNG KÊ THEO LINE", st.section);
                ctkmSheet.addMergedRegion(new CellRangeAddress(sec2.getRowNum(), sec2.getRowNum(), 0, 11));

                // --- 2) BẢNG LINE STATS ---
                Row ph = ctkmSheet.createRow(r0++);
                set(ctkmSheet, ph, 0, "Promotion ID", st.header);
                set(ctkmSheet, ph, 1, "Line ID",      st.header);
                set(ctkmSheet, ph, 2, "Tên Line",     st.header);
                set(ctkmSheet, ph, 3, "Loại",         st.header);
                set(ctkmSheet, ph, 4, "Lượt dùng",    st.header);
                set(ctkmSheet, ph, 5, "Giảm giá (VND)", st.header);
                set(ctkmSheet, ph, 6, "Giá gốc (VND)",  st.header);
                set(ctkmSheet, ph, 7, "Thu (sau giảm)", st.header);

                int lineStart = ph.getRowNum();
                long totalRedem = 0, totalDisc = 0, totalOri = 0, totalFin = 0;
                for (PromotionLineStatsResponse s : lineStats) {
                    Row row = ctkmSheet.createRow(r0++);
                    set(ctkmSheet, row, 0, nvl(s.getPromotionId(), ""),      st.td);
                    set(ctkmSheet, row, 1, nvl(s.getPromotionLineId(), ""),  st.td);
                    set(ctkmSheet, row, 2, nvl(s.getPromotionLineName(), ""),st.td);
                    set(ctkmSheet, row, 3, nvl(s.getType(), ""),             st.tdCenter);
                    setNum(ctkmSheet, row, 4, nz(s.getRedemptions()),        st.tdRight);
                    setNum(ctkmSheet, row, 5, nz(s.getTotalDiscount()),      st.moneyRight);
                    setNum(ctkmSheet, row, 6, nz(s.getTotalOriginal()),      st.moneyRight);
                    setNum(ctkmSheet, row, 7, nz(s.getTotalFinal()),         st.moneyRight);

                    totalRedem += (s.getRedemptions()==null?0:s.getRedemptions());
                    totalDisc  += (s.getTotalDiscount()==null?0:s.getTotalDiscount());
                    totalOri   += (s.getTotalOriginal()==null?0:s.getTotalOriginal());
                    totalFin   += (s.getTotalFinal()==null?0:s.getTotalFinal());
                }
                Row ptotal = ctkmSheet.createRow(r0++);
                set(ctkmSheet, ptotal, 2, "TỔNG CỘNG:", st.totalLeft);
                setNum(ctkmSheet, ptotal, 4, totalRedem, st.totalRight);
                setNum(ctkmSheet, ptotal, 5, totalDisc,  st.totalRight);
                setNum(ctkmSheet, ptotal, 6, totalOri,   st.totalRight);
                setNum(ctkmSheet, ptotal, 7, totalFin,   st.totalRight);
                st.addBoxBorder(ctkmSheet, lineStart, r0-1, 0, 7);

                r0 += 2;


                r0++; // khoảng trắng
                Row sec3 = ctkmSheet.createRow(r0++);
                set(ctkmSheet, sec3, 0, "III. TOP VOUCHER", st.section);
                ctkmSheet.addMergedRegion(new CellRangeAddress(sec3.getRowNum(), sec3.getRowNum(), 0, 11));

                // --- 3) TOP VOUCHER (ngay bên dưới) ---
                Row vh = ctkmSheet.createRow(r0++);
                set(ctkmSheet, vh, 0, "Voucher",      st.header);
                set(ctkmSheet, vh, 1, "Promotion ID", st.header);
                set(ctkmSheet, vh, 2, "Line ID",      st.header);
                set(ctkmSheet, vh, 3, "Lượt dùng",    st.header);
                set(ctkmSheet, vh, 4, "User duy nhất",st.header);
                set(ctkmSheet, vh, 5, "Giảm giá (VND)", st.header);
                set(ctkmSheet, vh, 6, "Giá gốc",        st.header);
                set(ctkmSheet, vh, 7, "Thu sau giảm",   st.header);
                set(ctkmSheet, vh, 8, "Max usage",      st.header);
                set(ctkmSheet, vh, 9, "Đã dùng",        st.header);
                set(ctkmSheet, vh,10, "First use",      st.header);
                set(ctkmSheet, vh,11, "Last use",       st.header);

                int vStart = vh.getRowNum();
                int rr = r0;
                DateTimeFormatter dF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (VoucherUsageItemResponse v : topVouchers) {
                    Row row = ctkmSheet.createRow(rr++);
                    set(ctkmSheet, row, 0, nvl(v.getVoucherCode(), ""),   st.td);
                    set(ctkmSheet, row, 1, nvl(v.getPromotionId(), ""),   st.td);
                    set(ctkmSheet, row, 2, nvl(v.getPromotionLineId(), ""), st.td);
                    setNum(ctkmSheet, row, 3, nz(v.getUses()),           st.tdRight);
                    setNum(ctkmSheet, row, 4, nz(v.getUniqueUsers()),    st.tdRight);
                    setNum(ctkmSheet, row, 5, nz(v.getTotalDiscount()),  st.moneyRight);
                    setNum(ctkmSheet, row, 6, nz(v.getTotalOriginal()),  st.moneyRight);
                    setNum(ctkmSheet, row, 7, nz(v.getTotalFinal()),     st.moneyRight);
                    setNum(ctkmSheet, row, 8, nz(v.getMaxUsage()==null?0L:v.getMaxUsage().longValue()), st.tdRight);
                    setNum(ctkmSheet, row, 9, nz(v.getUsedCount()==null?0L:v.getUsedCount().longValue()), st.tdRight);
                    set(ctkmSheet, row,10, v.getFirstUse()==null? "" : v.getFirstUse().format(dF), st.tdCenter);
                    set(ctkmSheet, row,11, v.getLastUse()==null  ? "" : v.getLastUse().format(dF),  st.tdCenter);
                    if (((rr - vStart) % 2) == 0) st.paintZebra(row, 0, 11);
                }
                r0 = rr;
                st.addBoxBorder(ctkmSheet, vStart, r0-1, 0, 11);

                r0 += 2;



                // --- 4) CTKM CHART (vẫn trên sheet này, neo bên dưới) ---
                if (usageChart != null && usageChart.getLabels()!=null && !usageChart.getLabels().isEmpty()) {
                    // tiêu đề nhỏ
                    r0++; // khoảng trắng
                    Row sec4 = ctkmSheet.createRow(r0++);
                    set(ctkmSheet, sec4, 0, "IV. BIỂU ĐỒ SỬ DỤNG CTKM", st.section);
                    ctkmSheet.addMergedRegion(new CellRangeAddress(sec4.getRowNum(), sec4.getRowNum(), 0, 11));

                    // bảng dữ liệu nhỏ cho chart:
                    int promoDataStart = r0;
                    Row hd2 = ctkmSheet.createRow(r0++);
                    set(ctkmSheet, hd2, 0, "Ngày/Nhóm", st.header);
                    set(ctkmSheet, hd2, 1, "Lượt dùng", st.header);
                    set(ctkmSheet, hd2, 2, "Giảm giá",  st.header);


                    for (int i = 0; i < usageChart.getLabels().size(); i++) {
                        Row row = ctkmSheet.createRow(r0++);
                        set(ctkmSheet, row, 0, usageChart.getLabels().get(i),            st.td);
                        setNum(ctkmSheet, row, 1, nz(usageChart.getRedemptions().get(i)), st.tdRight);
                        setNum(ctkmSheet, row, 2, nz(usageChart.getDiscountAmounts().get(i)), st.moneyRight);
                    }
                    int dataEnd = r0 - 1;

                    // vẽ chart, anchor bên phải hoặc full-width tùy thích
                    XSSFDrawing dr = ctkmSheet.createDrawingPatriarch();
                    // neo: cột 0..11, hàng r0..r0+~20 (đặt dưới khối data ~1 hàng)
                    XSSFClientAnchor anchor = dr.createAnchor(0,0,0,0, 0, promoDataStart + 1, 12, promoDataStart + 22);
                    XSSFChart xChart = dr.createChart(anchor);
                    xChart.setTitleText("Sử dụng CTKM theo ngày");
                    xChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);

                    XDDFCategoryAxis bottom = xChart.createCategoryAxis(AxisPosition.BOTTOM);
                    XDDFValueAxis left    = xChart.createValueAxis(AxisPosition.LEFT);
                    bottom.crossAxis(left); left.crossAxis(bottom);

                    XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(
                            ctkmSheet, new CellRangeAddress(promoDataStart+1, dataEnd, 0, 0));
                    XDDFNumericalDataSource<Double> ys1 = XDDFDataSourcesFactory.fromNumericCellRange(
                            ctkmSheet, new CellRangeAddress(promoDataStart+1, dataEnd, 1, 1));
                    XDDFNumericalDataSource<Double> ys2 = XDDFDataSourcesFactory.fromNumericCellRange(
                            ctkmSheet, new CellRangeAddress(promoDataStart+1, dataEnd, 2, 2));

                    XDDFChartData data = xChart.createData(ChartTypes.LINE, bottom, left);
                    data.addSeries(xs, ys1).setTitle("Lượt dùng", null);
                    data.addSeries(xs, ys2).setTitle("Giảm giá (VND)", null);
                    xChart.plot(data);
                }

                // width cột (12 cột cho đủ Top Voucher)
                for (int cc = 0; cc <= 11; cc++) ctkmSheet.setColumnWidth(cc, (cc<=2?24:14)*256);
            }


            // (4) Thêm sheet Doanh số KH vào CÙNG FILE
            writeCustomerSalesSheet(wb, st, start, end, companyName, companyAddress);


            // ===== Xuất HTTP =====
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String cd = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", cd);
            try (ServletOutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
        }
    }


    public void exportMovieReportRange(HttpServletResponse response,
                                       LocalDate start,
                                       LocalDate end,
                                       GroupByDataAnalzerResponse groupBy,
                                       String companyName,
                                       String companyAddress) throws IOException {
        // --- Lấy dữ liệu từ DataAnalyzerService ---
        MovieStatsSummaryResponse summary = dataAnalyzerService.getMovieSummaryByRange(start, end);
        CountChartResponse newMovies = dataAnalyzerService.getNewMoviesByRange(start, end, groupBy);
        List<CategoryCountItemResponse> byGenre   = dataAnalyzerService.getCountByGenre(20);
        List<CategoryCountItemResponse> byCountry = dataAnalyzerService.getCountByCountry(20);
        List<CategoryCountItemResponse> byStatus  = dataAnalyzerService.getStatusBreakdown();
        List<CategoryCountItemResponse> byType    = dataAnalyzerService.getTypeBreakdown();
        List<TopMovieDTOResponse> topViews  = dataAnalyzerService.getTopMoviesByViewsInRange(start, end, 10);
        List<TopMovieDTOResponse> topRating = dataAnalyzerService.getTopMoviesByRatingInRange(start, end, 10, 5);

        // fallback: nếu khoảng ngày không có phim nào, rỗng thì hiển thị all-time cho đỡ trống
        if (topViews.isEmpty())  topViews  = dataAnalyzerService.getTopMoviesByViews(10);
        if (topRating.isEmpty()) topRating = dataAnalyzerService.getTopMoviesByRating(10, 1); // có hạ ngưỡng

        String fileName = "BaoCao_Phim_" + start + "_" + end + "_" + groupBy + ".xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ===== SHEET 1: Tóm tắt =====
            XSSFSheet sh = wb.createSheet("Tổng quan phim");
            sh.setDisplayGridlines(false); sh.setPrintGridlines(false);
            var ps = sh.getPrintSetup(); ps.setLandscape(true); ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
            sh.setMargin(Sheet.LeftMargin, 0.4); sh.setMargin(Sheet.RightMargin, 0.4);
            sh.setMargin(Sheet.TopMargin, 0.6);  sh.setMargin(Sheet.BottomMargin, 0.6);

            int r = 0;
            Row r1 = sh.createRow(r++); set(sh, r1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
            sh.addMergedRegion(new CellRangeAddress(0,0,0,8));
            Row r2 = sh.createRow(r++); set(sh, r2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
            sh.addMergedRegion(new CellRangeAddress(1,1,0,8));
            Row r3 = sh.createRow(r++); set(sh, r3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
            sh.addMergedRegion(new CellRangeAddress(2,2,0,8));

            r++;
            Row title = sh.createRow(r++); set(sh, title, 0, "BÁO CÁO THỐNG KÊ PHIM", st.title);
            sh.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, 8));
            Row rg = sh.createRow(r++); set(sh, rg, 0,
                    "Từ: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "  Đến: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "  (Nhóm theo: " + groupBy + ")", st.centerGrey);
            sh.addMergedRegion(new CellRangeAddress(rg.getRowNum(), rg.getRowNum(), 0, 8));
            r++;

            // 4–8 thẻ số liệu
            Row s1 = sh.createRow(r++); set(sh, s1, 0, "Tổng phim", st.th);                setNum(sh, s1, 1, summary.getTotalMovies(), st.tdRight);
            Row s2 = sh.createRow(r++); set(sh, s2, 0, "Single / Series", st.th);          set(sh, s2, 1, summary.getTotalSingle()+" / "+summary.getTotalSeries(), st.tdRight);
            Row s3 = sh.createRow(r++); set(sh, s3, 0, "Completed / Upcoming", st.th);     set(sh, s3, 1, summary.getCompletedCount()+" / "+summary.getUpcomingCount(), st.tdRight);
            Row s4 = sh.createRow(r++); set(sh, s4, 0, "Seasons / Episodes", st.th);       set(sh, s4, 1, summary.getTotalSeasons()+" / "+summary.getTotalEpisodes(), st.tdRight);
            Row s5 = sh.createRow(r++); set(sh, s5, 0, "Thêm mới trong khoảng", st.th);    setNum(sh, s5, 1, summary.getAddedThisMonth(), st.tdRight);
            Row s6 = sh.createRow(r++); set(sh, s6, 0, "Điểm TB toàn hệ thống", st.th);    setNum(sh, s6, 1, summary.getAvgRatingAll(), st.tdRight);
            st.addBoxBorder(sh, s1.getRowNum(), s6.getRowNum(), 0, 1);
            r++;

            // ===== II. TOP THEO LƯỢT XEM (dọc) =====
            Row secViews = sh.createRow(r++);
            set(sh, secViews, 0, "II. TOP THEO LƯỢT XEM", st.section);
            sh.addMergedRegion(new CellRangeAddress(secViews.getRowNum(), secViews.getRowNum(), 0, 8));

            Row hv = sh.createRow(r++);
            set(sh, hv, 0, "STT",      st.header);
            set(sh, hv, 1, "Tên phim", st.header);
            set(sh, hv, 2, "Lượt xem", st.header);
            set(sh, hv, 3, "Điểm TB",  st.header);
// kẻ đủ width cho khối (để in A4 đẹp)
            for (int k = 4; k <= 8; k++) { Cell ccc = hv.createCell(k); ccc.setCellStyle(st.header); }
            int viewsHeader = hv.getRowNum();

            for (int i = 0; i < topViews.size(); i++) {
                Row rrw = sh.createRow(r++);
                var t = topViews.get(i);
                set(sh, rrw, 0, String.valueOf(i + 1),      st.tdCenter);
                set(sh, rrw, 1, nvl(t.getTitle(), ""),      st.td);
                setNum(sh, rrw, 2, t.getViewCount()==null?0:t.getViewCount(), st.tdRight);
                setNum(sh, rrw, 3, t.getAvgRating()==null?0:t.getAvgRating(), st.tdRight);
                if ((i % 2) == 1) st.paintZebra(rrw, 0, 8);
            }
            st.addBoxBorder(sh, viewsHeader, r - 1, 0, 8);

            r += 2; // khoảng cách giữa 2 bảng

// ===== III. TOP THEO ĐÁNH GIÁ (dọc) =====
            Row secRate = sh.createRow(r++);
            set(sh, secRate, 0, "III. TOP THEO ĐÁNH GIÁ", st.section);
            sh.addMergedRegion(new CellRangeAddress(secRate.getRowNum(), secRate.getRowNum(), 0, 8));

            Row hr = sh.createRow(r++);
            set(sh, hr, 0, "STT",         st.header);
            set(sh, hr, 1, "Tên phim",    st.header);
            set(sh, hr, 2, "Điểm TB",     st.header);
            set(sh, hr, 3, "Số đánh giá", st.header);
            for (int k = 4; k <= 8; k++) { Cell ccc = hr.createCell(k); ccc.setCellStyle(st.header); }
            int rateHeader = hr.getRowNum();

            for (int i = 0; i < topRating.size(); i++) {
                Row rrw = sh.createRow(r++);
                var t = topRating.get(i);
                set(sh, rrw, 0, String.valueOf(i + 1),         st.tdCenter);
                set(sh, rrw, 1, nvl(t.getTitle(), ""),         st.td);
                setNum(sh, rrw, 2, t.getAvgRating()==null?0:t.getAvgRating(),      st.tdRight);
                setNum(sh, rrw, 3, t.getRatingCount()==null?0:t.getRatingCount(),  st.tdRight);
                if ((i % 2) == 1) st.paintZebra(rrw, 0, 8);
            }
            st.addBoxBorder(sh, rateHeader, r - 1, 0, 8);

            // width hợp lý cho bố cục dọc
            sh.setColumnWidth(0, 6*256);
            sh.setColumnWidth(1, 40*256);
            sh.setColumnWidth(2, 14*256);
            sh.setColumnWidth(3, 14*256);
            for (int ccc = 4; ccc <= 8; ccc++) sh.setColumnWidth(ccc, 6*256);

            r += 1;
            Row notesTitle1 = sh.createRow(r++);
            set(sh, notesTitle1, 0, "Ghi chú", st.th);
            sh.addMergedRegion(new CellRangeAddress(notesTitle1.getRowNum(), notesTitle1.getRowNum(), 0, 6));

            Row notesRow1 = sh.createRow(r++);
            String moTa1 =
                    "- Thống kê phim theo lượt xem và theo đánh giá.\n" +
                            "- Bảng 'Top theo lượt xem' sắp theo view giảm dần.\n" +
                            "- Bảng 'Top theo đánh giá' lọc theo ngưỡng số đánh giá tối thiểu, sắp theo điểm trung bình.\n" +
                            "- Dữ liệu lấy từ danh sách phim trong hệ thống trong khoảng ngày đã chọn.";
            set(sh, notesRow1, 0, moTa1, st.notes);
            sh.addMergedRegion(new CellRangeAddress(notesRow1.getRowNum(), notesRow1.getRowNum(), 0, 6));
            notesRow1.setHeightInPoints(90f);

            // ===== SHEET 2: New Movies Chart =====
            if (newMovies.getLabels()!=null && !newMovies.getLabels().isEmpty()) {
                XSSFSheet sChart = wb.createSheet("Phim mới theo " + groupBy);
                sChart.setDisplayGridlines(false);
                sChart.setPrintGridlines(false);

                // Header thông tin
                int rChart = 0;
                Row rc1 = sChart.createRow(rChart++);
                set(sChart, rc1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
                sChart.addMergedRegion(new CellRangeAddress(rc1.getRowNum(), rc1.getRowNum(), 0, 8));

                Row rc2 = sChart.createRow(rChart++);
                set(sChart, rc2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
                sChart.addMergedRegion(new CellRangeAddress(rc2.getRowNum(), rc2.getRowNum(), 0, 8));

                Row rc3 = sChart.createRow(rChart++);
                set(sChart, rc3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
                sChart.addMergedRegion(new CellRangeAddress(rc3.getRowNum(), rc3.getRowNum(), 0, 8));

                rChart++;

                Row titleRow = sChart.createRow(rChart++);
                set(sChart, titleRow, 0, "PHIM MỚI THEO " + groupBy.name().toUpperCase(), st.title);
                sChart.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 8));

                Row dateRange = sChart.createRow(rChart++);
                set(sChart, dateRange, 0,
                    "Từ ngày: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + "   Đến ngày: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
                sChart.addMergedRegion(new CellRangeAddress(dateRange.getRowNum(), dateRange.getRowNum(), 0, 8));

                rChart++;

                // Bảng dữ liệu
                Row hd = sChart.createRow(rChart++);
                set(sChart, hd, 0, "Nhóm", st.header);
                set(sChart, hd, 1, "Số phim", st.header);

                int dataStartRow = rChart;
                for (int i = 0; i < newMovies.getLabels().size(); i++) {
                    Row row = sChart.createRow(rChart++);
                    set(sChart, row, 0, newMovies.getLabels().get(i), st.td);
                    setNum(sChart, row, 1, newMovies.getData().get(i), st.tdRight);
                }

                // Biểu đồ
                XSSFDrawing drawing = sChart.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0,0,0,0,3, dataStartRow-1, 11, rChart+15);
                XSSFChart xssfChart = drawing.createChart(anchor);
                xssfChart.setTitleText("Phim mới theo " + groupBy);
                xssfChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);
                XDDFCategoryAxis bottom = xssfChart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis left = xssfChart.createValueAxis(AxisPosition.LEFT);
                int last = dataStartRow + newMovies.getLabels().size() - 1;
                var xs = XDDFDataSourcesFactory.fromStringCellRange(sChart, new CellRangeAddress(dataStartRow, last, 0, 0));
                var ys = XDDFDataSourcesFactory.fromNumericCellRange(sChart, new CellRangeAddress(dataStartRow, last, 1, 1));
                XDDFChartData data = xssfChart.createData(ChartTypes.LINE, bottom, left);
                data.addSeries(xs, ys).setTitle("Số phim", null);
                xssfChart.plot(data);

                sChart.setColumnWidth(0, 20*256);
                sChart.setColumnWidth(1, 14*256);

                // Ghi chú
                int rr2 = rChart + 2;
                Row nt = sChart.createRow(rr2++);
                set(sChart, nt, 0, "Ghi chú", st.th);
                sChart.addMergedRegion(new CellRangeAddress(nt.getRowNum(), nt.getRowNum(), 0, 6));

                Row nr = sChart.createRow(rr2++);
                String moTa2 =
                        "- Biểu đồ số phim thêm mới theo " + groupBy + ".\n" +
                                "- Trục X: nhóm thời gian; Trục Y: số lượng phim mới.\n" +
                                "- Dữ liệu lấy từ ngày tạo phim.";
                set(sChart, nr, 0, moTa2, st.notes);
                sChart.addMergedRegion(new CellRangeAddress(nr.getRowNum(), nr.getRowNum(), 0, 6));
                nr.setHeightInPoints(70f);

            }

            // ===== SHEET 3: Phân tích danh mục =====
            XSSFSheet sCat = wb.createSheet("Phân rã danh mục");
            sCat.setDisplayGridlines(false);
            sCat.setPrintGridlines(false);

            // Header thông tin
            int r0 = 0;
            Row rcat1 = sCat.createRow(r0++);
            set(sCat, rcat1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
            sCat.addMergedRegion(new CellRangeAddress(rcat1.getRowNum(), rcat1.getRowNum(), 0, 8));

            Row rcat2 = sCat.createRow(r0++);
            set(sCat, rcat2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
            sCat.addMergedRegion(new CellRangeAddress(rcat2.getRowNum(), rcat2.getRowNum(), 0, 8));

            Row rcat3 = sCat.createRow(r0++);
            set(sCat, rcat3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
            sCat.addMergedRegion(new CellRangeAddress(rcat3.getRowNum(), rcat3.getRowNum(), 0, 8));

            r0++;

            Row titleCat = sCat.createRow(r0++);
            set(sCat, titleCat, 0, "PHÂN RÃ DANH MỤC PHIM", st.title);
            sCat.addMergedRegion(new CellRangeAddress(titleCat.getRowNum(), titleCat.getRowNum(), 0, 8));

            Row dateRangeCat = sCat.createRow(r0++);
            set(sCat, dateRangeCat, 0,
                "Từ ngày: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + "   Đến ngày: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
            sCat.addMergedRegion(new CellRangeAddress(dateRangeCat.getRowNum(), dateRangeCat.getRowNum(), 0, 8));

            r0++;

            Row hh = sCat.createRow(r0++); set(sCat, hh, 0, "Top Thể loại", st.header); set(sCat, hh, 3, "Top Quốc gia", st.header);
            // thể loại
            Row h1 = sCat.createRow(r0++); set(sCat, h1, 0, "Thể loại", st.th); set(sCat, h1, 1, "Số phim", st.th);
            int base = r0;
            for (var it : byGenre) { Row row = sCat.createRow(r0++); set(sCat, row, 0, it.getKey(), st.td); setNum(sCat, row, 1, it.getCount(), st.tdRight); }
            st.addBoxBorder(sCat, h1.getRowNum(), r0-1, 0, 1);

            // quốc gia (cột bên phải)
            int rR = base; Row h2 = sCat.getRow(h1.getRowNum()); set(sCat, h2, 3, "Quốc gia", st.th); set(sCat, sCat.getRow(h2.getRowNum()), 4, "Số phim", st.th);
            for (var it : byCountry) { Row row = sCat.getRow(rR); if (row==null) row = sCat.createRow(rR); set(sCat, row, 3, it.getKey(), st.td); setNum(sCat, row, 4, it.getCount(), st.tdRight); rR++; }
            st.addBoxBorder(sCat, h1.getRowNum(), Math.max(r0-1, rR-1), 3, 4);

            // status & type dưới cùng
            int rB = Math.max(r0, rR) + 1;
            Row h3 = sCat.createRow(rB++); set(sCat, h3, 0, "Trạng thái", st.header); set(sCat, h3, 3, "Loại phim", st.header);
            Row h3a = sCat.createRow(rB++); set(sCat, h3a, 0, "Status", st.th); set(sCat, h3a, 1, "Số phim", st.th);
            for (var it : byStatus) { Row row = sCat.createRow(rB++); set(sCat, row, 0, it.getKey(), st.td); setNum(sCat, row, 1, it.getCount(), st.tdRight); }
            st.addBoxBorder(sCat, h3a.getRowNum(), rB-1, 0, 1);
            int rT = h3a.getRowNum(); set(sCat, sCat.getRow(rT), 3, "Type", st.th); set(sCat, sCat.getRow(rT), 4, "Số phim", st.th);
            int rT2 = rT+1;
            for (var it : byType) { Row row = sCat.getRow(rT2); if (row==null) row = sCat.createRow(rT2); set(sCat, row, 3, it.getKey(), st.td); setNum(sCat, row, 4, it.getCount(), st.tdRight); rT2++; }
            st.addBoxBorder(sCat, rT, rT2-1, 3, 4);
            sCat.setColumnWidth(0, 24*256); sCat.setColumnWidth(1, 10*256);
            sCat.setColumnWidth(3, 24*256); sCat.setColumnWidth(4, 10*256);


            int tail = Math.max(rB, rT2) + 2;
            Row nt3 = sCat.createRow(tail++);
            set(sCat, nt3, 0, "Ghi chú", st.th);
            sCat.addMergedRegion(new CellRangeAddress(nt3.getRowNum(), nt3.getRowNum(), 0, 6));

            Row nr3 = sCat.createRow(tail++);
            String moTa3 =
                    "- Top thể loại/quốc gia được sắp theo số lượng phim giảm dần.\n" +
                            "- Hai bảng 'Trạng thái' và 'Loại phim' tổng hợp toàn hệ thống.";
            set(sCat, nr3, 0, moTa3, st.notes);
            sCat.addMergedRegion(new CellRangeAddress(nr3.getRowNum(), nr3.getRowNum(), 0, 6));
            nr3.setHeightInPoints(60f);


            // ===== Xuất HTTP =====
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String cd = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", cd);
            try (ServletOutputStream out = response.getOutputStream()) { wb.write(out); }
        }
    }



// ExportDashboardService.java

    private void writeCustomerSalesSheet(XSSFWorkbook wb,
                                         Styles st,
                                         LocalDate start, LocalDate end,
                                         String companyName, String companyAddress) {
        CustomerSalesReportResponse rpt = dataAnalyzerService.getCustomerSalesByRange(start, end);

        XSSFSheet sh = wb.createSheet("Doanh số KH");
        sh.setDisplayGridlines(false); sh.setPrintGridlines(false);
        var ps = sh.getPrintSetup(); ps.setLandscape(true); ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        sh.setMargin(Sheet.LeftMargin, 0.4); sh.setMargin(Sheet.RightMargin, 0.4);
        sh.setMargin(Sheet.TopMargin, 0.6);  sh.setMargin(Sheet.BottomMargin, 0.6);

        int r = 0;
        Row r1 = sh.createRow(r++); set(sh, r1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,13));
        Row r2 = sh.createRow(r++); set(sh, r2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
        sh.addMergedRegion(new CellRangeAddress(1,1,0,13));
        Row r3 = sh.createRow(r++); set(sh, r3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
        sh.addMergedRegion(new CellRangeAddress(2,2,0,13));

        r++;
        Row title = sh.createRow(r++); set(sh, title, 0, "DOANH SỐ THEO KHÁCH HÀNG", st.title);
        sh.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, 13));
        Row rg = sh.createRow(r++); set(sh, rg, 0,
                "Từ ngày: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + "   Đến ngày: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
        sh.addMergedRegion(new CellRangeAddress(rg.getRowNum(), rg.getRowNum(), 0, 13));
        r++;

        // Header chi tiết
        Row h = sh.createRow(r++);
        int c = 0;
        set(sh, h, c++, "STT", st.header);
        set(sh, h, c++, "Mã KH", st.header);
        set(sh, h, c++, "Tên KH", st.header);
        set(sh, h, c++, "SĐT", st.header);
        set(sh, h, c++, "Email", st.header);
        set(sh, h, c++, "Số GD", st.header);
        set(sh, h, c++, "Doanh số trước CK", st.header);
        set(sh, h, c++, "Chiết khấu", st.header);
        set(sh, h, c++, "Doanh số sau CK", st.header);
        set(sh, h, c++, "First buy", st.header);
        set(sh, h, c++, "Last buy", st.header);
        for (int k=c; k<=13; k++) { Cell cc = h.createCell(k); cc.setCellStyle(st.header); }
        int headerRow = h.getRowNum();

        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int stt = 1; long totOri=0, totDis=0, totFin=0, totTx=0;
        List<CustomerSalesItemResponse> rows = rpt.getRows()==null? List.of(): rpt.getRows();

        for (CustomerSalesItemResponse rowData : rows) {
            Row rr = sh.createRow(r++);
            int j=0;
            set(sh, rr, j++, String.valueOf(stt++), st.tdCenter);
            set(sh, rr, j++, nvl(rowData.getUserId(), ""), st.td);
            set(sh, rr, j++, nvl(rowData.getUserName(), ""), st.td);
            set(sh, rr, j++, nvl(rowData.getPhoneNumber(), ""), st.td);
            set(sh, rr, j++, nvl(rowData.getEmail(), ""), st.td);
            setNum(sh, rr, j++, rowData.getTxCount(),        st.tdRight);
            setNum(sh, rr, j++, rowData.getTotalOriginal(),  st.moneyRight);
            setNum(sh, rr, j++, rowData.getTotalDiscount(),  st.moneyRight);
            setNum(sh, rr, j++, rowData.getTotalFinal(),     st.moneyRight);
            set(sh, rr, j++, rowData.getFirstDate()==null? "" : rowData.getFirstDate().format(dFmt), st.tdCenter);
            set(sh, rr, j++, rowData.getLastDate()==null ? "" : rowData.getLastDate().format(dFmt),  st.tdCenter);

            totOri += rowData.getTotalOriginal();
            totDis += rowData.getTotalDiscount();
            totFin += rowData.getTotalFinal();
            totTx  += rowData.getTxCount();
            if ((r % 2)==0) st.paintZebra(rr, 0, 13);
        }

// TOTAL hàng cuối bảng
        Row total = sh.createRow(r++);
        set(sh, total, 0, "TỔNG CỘNG", st.totalLeft);
        sh.addMergedRegion(new CellRangeAddress(total.getRowNum(), total.getRowNum(), 0, 5));
        setNum(sh, total, 6, totOri, st.totalRight);
        setNum(sh, total, 7, totDis, st.totalRight);
        setNum(sh, total, 8, totFin, st.totalRight);

// kẻ khung toàn bảng
        st.addBoxBorder(sh, headerRow, total.getRowNum(), 0, 13);

// Box 4 dòng (đặt DƯỚI bảng)
        r += 1;
        Row s1 = sh.createRow(r++); set(sh, s1, 0, "Doanh số trước CK", st.th); setNum(sh, s1, 1, totOri, st.moneyRight);
        Row s2 = sh.createRow(r++); set(sh, s2, 0, "Chiết khấu",        st.th); setNum(sh, s2, 1, totDis, st.moneyRight);
        Row s3 = sh.createRow(r++); set(sh, s3, 0, "Doanh số sau CK",   st.th); setNum(sh, s3, 1, totFin, st.moneyRight);
        Row s4 = sh.createRow(r++); set(sh, s4, 0, "Số giao dịch",      st.th); setNum(sh, s4, 1, totTx,  st.tdRight);
        st.addBoxBorder(sh, s1.getRowNum(), s4.getRowNum(), 0, 1);

// --- GHI CHÚ ---
        r += 1;

// Tiêu đề
        Row notesTitle = sh.createRow(r++);
        set(sh, notesTitle, 0, "Ghi chú", st.th);
        sh.addMergedRegion(new CellRangeAddress(notesTitle.getRowNum(), notesTitle.getRowNum(), 0, 10));

// Nội dung mô tả
        Row notesRow = sh.createRow(r++);
        String moTa =
                "- Thông tin khách hàng\n" +
                        "- Chiết khấu: bao gồm khuyến mãi % hoặc đơn và số tiền cụ thể.\n" +
                        "- Doanh số trước chiết khấu: tổng tiền chưa trừ chiết khấu.\n" +
                        "- Doanh số sau chiết khấu: tổng tiền đã trừ chiết khấu.\n\n" +
                        "Lấy dữ liệu từ bảng khách hàng, sản phẩm, hóa đơn bán hàng (không tính các hóa đơn mua đã trả).";
        set(sh, notesRow, 0, moTa, st.notes);
        sh.addMergedRegion(new CellRangeAddress(notesRow.getRowNum(), notesRow.getRowNum(), 0, 10));
        notesRow.setHeightInPoints(110f);

// Freeze/print & width
        wb.setPrintArea(wb.getSheetIndex(sh), 0, 13, 0, total.getRowNum());
        sh.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, 0, 13));
        int[] w = {6,18,24,14,26,8,16,16,16,12,12,8,8,8};
        for (int i=0;i<w.length;i++) sh.setColumnWidth(i, w[i]*256);
    }



    // ===== Helpers =====
    private static Row getOrCreateRow(Sheet s, int rowIndex) {
        Row r = s.getRow(rowIndex);
        return (r != null) ? r : s.createRow(rowIndex);
    }
    private static String nvl(String s, String def) { return (s==null || s.isBlank()) ? def : s; }
    private static String safe(Object o) { return o==null? "" : o.toString(); }
    private static double nz(Double v) { return v == null ? 0.0 : v; }
    private static double nz(Long v) { return v == null ? 0.0 : v.doubleValue(); }

    private static void set(XSSFSheet sh, Row row, int col, String val, CellStyle st) {
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(st);
    }
    private static void setNum(XSSFSheet sh, Row row, int col, double val, CellStyle st) {
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(st);
    }
    private static void setFormula(XSSFSheet sh, Row row, int col, String formula, CellStyle st) {
        Cell c = row.createCell(col); c.setCellFormula(formula); c.setCellStyle(st);
    }
    private static String ref(int colZeroBased, int rowOneBased) {
        return CellReference.convertNumToColString(colZeroBased) + rowOneBased;
    }

    /** Styles: Times New Roman, border, zebra, box border, formats */
    static class Styles {
        final XSSFWorkbook wb;
        final Font font;
        final CellStyle title, hdrBoldRed, smallGrey, centerGrey;
        final CellStyle header, headerMuted, th, td, tdRight, tdCenter, totalLeft, totalRight;
        final CellStyle moneyRight;
        final CellStyle section;
        final CellStyle grandLeft, grandRight;
        final CellStyle notes; // block ghi chú (wrap text)


        Styles(XSSFWorkbook wb) {
            this.wb = wb;

            // Base font
            font = wb.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short)11);

            // ONE shared DataFormat
            final DataFormat df = wb.createDataFormat();

            // Title
            title = wb.createCellStyle();
            Font ft = wb.createFont(); ft.setBold(true); ft.setFontHeightInPoints((short)16); ft.setFontName("Times New Roman");
            title.setFont(ft); title.setAlignment(HorizontalAlignment.CENTER);

            // Small headers
            hdrBoldRed = wb.createCellStyle();
            Font fr = wb.createFont(); fr.setBold(true); fr.setColor(IndexedColors.DARK_RED.getIndex()); fr.setFontName("Times New Roman");
            hdrBoldRed.setFont(fr);

            smallGrey = wb.createCellStyle();
            Font fg = wb.createFont(); fg.setFontName("Times New Roman"); fg.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            fg.setFontHeightInPoints((short)9); smallGrey.setFont(fg);

            centerGrey = wb.createCellStyle();
            centerGrey.setFont(font); centerGrey.setAlignment(HorizontalAlignment.CENTER);

            // Table headers
            header = baseBorder();
            Font fh = wb.createFont(); fh.setBold(true); fh.setFontName("Times New Roman");
            header.setFont(fh);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);

            headerMuted = baseBorder();
            headerMuted.setAlignment(HorizontalAlignment.CENTER);
            headerMuted.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerMuted.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            th = baseBorder(); th.setFont(fh); th.setAlignment(HorizontalAlignment.LEFT);
            td = baseBorder(); td.setFont(font);
            tdRight = baseBorder(); tdRight.setFont(font); tdRight.setAlignment(HorizontalAlignment.RIGHT);
            tdCenter = baseBorder(); tdCenter.setFont(font); tdCenter.setAlignment(HorizontalAlignment.CENTER);

            // Money cells
            moneyRight = baseBorder();
            Font f = wb.createFont(); f.setFontName("Times New Roman");
            moneyRight.setFont(f);
            moneyRight.setAlignment(HorizontalAlignment.RIGHT);
            moneyRight.setDataFormat(df.getFormat("#,##0"));

            // Totals
            totalLeft = baseBorder();
            Font ftb = wb.createFont(); ftb.setBold(true); ftb.setFontName("Times New Roman");
            totalLeft.setFont(ftb); totalLeft.setAlignment(HorizontalAlignment.RIGHT);
            totalLeft.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            totalLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalRight = baseBorder();
            totalRight.setFont(ftb);
            totalRight.setAlignment(HorizontalAlignment.RIGHT);
            totalRight.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            totalRight.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalRight.setDataFormat(df.getFormat("#,##0"));


            // Section title (to đậm, căn giữa, nền xám nhạt)
            section = wb.createCellStyle();
            Font fs = wb.createFont();
            fs.setBold(true);
            fs.setFontName("Times New Roman");
            fs.setFontHeightInPoints((short)12);
            section.setFont(fs);
            section.setAlignment(HorizontalAlignment.LEFT);
            section.setVerticalAlignment(VerticalAlignment.CENTER);
            section.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            section.setBorderBottom(BorderStyle.THIN);
            section.setBorderTop(BorderStyle.THIN);
            section.setBorderLeft(BorderStyle.THIN);
            section.setBorderRight(BorderStyle.THIN);


            // Grand total (đậm hơn, viền trên dày)
            grandLeft = baseBorder();
            grandRight = baseBorder();
            Font fgt = wb.createFont(); fgt.setBold(true); fgt.setFontName("Times New Roman");
            grandLeft.setFont(fgt); grandRight.setFont(fgt);
            grandLeft.setAlignment(HorizontalAlignment.RIGHT);
            grandRight.setAlignment(HorizontalAlignment.RIGHT);
            grandRight.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            // viền trên nét vừa để tách mạnh
            grandLeft.setBorderTop(BorderStyle.MEDIUM);
            grandRight.setBorderTop(BorderStyle.MEDIUM);

            // Notes
            notes = wb.createCellStyle();
            notes.setFont(font);
            notes.setWrapText(true);

        }

        private CellStyle baseBorder() {
            CellStyle s = wb.createCellStyle();
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        void paintZebra(Row row, int fromCol, int toCol) {
            for (int c = fromCol; c <= toCol; c++) {
                Cell cell = row.getCell(c); if (cell==null) cell = row.createCell(c);
                CellStyle z = wb.createCellStyle(); z.cloneStyleFrom(cell.getCellStyle());
                z.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                z.setFillPattern(FillPatternType.LESS_DOTS);
                cell.setCellStyle(z);
            }
        }

        void addBoxBorder(Sheet sh, int r1, int r2, int c1, int c2) {
            for (int r = r1; r <= r2; r++) {
                Row row = sh.getRow(r); if (row==null) row = sh.createRow(r);
                for (int c = c1; c <= c2; c++) {
                    Cell cell = row.getCell(c); if (cell==null) cell = row.createCell(c);
                    CellStyle s = wb.createCellStyle(); s.cloneStyleFrom(cell.getCellStyle());
                    s.setBorderBottom(BorderStyle.THIN);
                    s.setBorderTop(BorderStyle.THIN);
                    s.setBorderLeft(BorderStyle.THIN);
                    s.setBorderRight(BorderStyle.THIN);
                    cell.setCellStyle(s);
                }
            }
        }
    }
}
