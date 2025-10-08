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

            final int TX_COL = 3;
            final int AMT_COL = TX_COL + 4;
            int headerRow = r;
            Row head = sheet.createRow(r++);
            set(sheet, head, 0, "Nhóm", st.header);
            set(sheet, head, 1, "Doanh thu", st.header);
            set(sheet, head, 2, "—", st.headerMuted);
            int c = TX_COL;
            set(sheet, head, c++, "STT", st.header);
            set(sheet, head, c++, "Mã đơn", st.header);
            set(sheet, head, c++, "Người dùng", st.header);
            set(sheet, head, c++, "Gói", st.header);
            set(sheet, head, c++, "Số tiền (VND)", st.header);
            set(sheet, head, c++, "Ngày", st.header);
            set(sheet, head, c++, "Trạng thái", st.header);

            int dataStart = r;

            int grpStart = dataStart;
            if (chart.getLabels() != null) {
                for (int i = 0; i < chart.getLabels().size(); i++) {
                    Row row = getOrCreateRow(sheet, dataStart + i);
                    set(sheet, row, 0, chart.getLabels().get(i), st.td);
                    setNum(sheet, row, 1, nz(chart.getData().get(i)), st.moneyRight);
                }
            }
            int grpEnd = (chart.getLabels()==null || chart.getLabels().isEmpty())
                    ? (dataStart - 1) : (dataStart + chart.getLabels().size() - 1);

            int txStart = dataStart;
            int stt = 1;
            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (txs != null) {
                for (int i = 0; i < txs.size(); i++) {
                    RecentTransactionResponse tx = txs.get(i);
                    Row row = getOrCreateRow(sheet, dataStart + i);
                    int cc2 = TX_COL;
                    set(sheet, row, cc2++, String.valueOf(stt++), st.tdCenter);
                    set(sheet, row, cc2++, "#" + safe(tx.getOrderId()), st.td);
                    set(sheet, row, cc2++, safe(tx.getUserName()), st.td);
                    set(sheet, row, cc2++, safe(tx.getPackageId()), st.td);
                    setNum(sheet, row, AMT_COL, nz(tx.getFinalAmount()), st.moneyRight);
                    set(sheet, row, AMT_COL + 1, tx.getCreatedAt()==null ? "" : tx.getCreatedAt().format(dFmt), st.tdCenter);
                    set(sheet, row, AMT_COL + 2, safe(tx.getStatus()), st.tdCenter);
                    if ((stt % 2) == 0) st.paintZebra(row, TX_COL, AMT_COL + 2);
                }
            }
            int txEnd = (txs==null || txs.isEmpty())
                    ? (dataStart - 1) : (dataStart + txs.size() - 1);

            int endRow  = Math.max(grpEnd, txEnd);
            Row total = sheet.createRow(endRow  + 1);

            if (grpEnd >= grpStart) {
                set(sheet, total, 0, "Tổng doanh thu (bảng trái):", st.totalLeft);
                setFormula(sheet, total, 1,
                        "SUM(" + ref(1, grpStart+1) + ":" + ref(1, grpEnd+1) + ")",
                        st.totalRight);
            }
            if (txEnd >= txStart) {
                set(sheet, total, AMT_COL - 1, "TỔNG CỘNG:", st.totalLeft);
                setFormula(sheet, total, AMT_COL,
                        "SUM(" + ref(AMT_COL, txStart+1) + ":" + ref(AMT_COL, txEnd+1) + ")",
                        st.totalRight);
            }

            st.addBoxBorder(sheet, headerRow, endRow  + 1, 0, AMT_COL + 2);
            sheet.createFreezePane(0, headerRow + 1);

            sheet.setColumnWidth(0, 18*256);
            sheet.setColumnWidth(1, 16*256);
            sheet.setColumnWidth(2, 3*256);
            sheet.setColumnWidth(3, 6*256);
            sheet.setColumnWidth(4, 18*256);
            sheet.setColumnWidth(5, 18*256);
            sheet.setColumnWidth(6, 12*256);
            sheet.setColumnWidth(7, 16*256);
            sheet.setColumnWidth(8, 12*256);
            sheet.setColumnWidth(9, 12*256);

            wb.setPrintArea(wb.getSheetIndex(sheet), 0, 9, 0, endRow  + 1);
            sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, 0, 9));

            // ===== SHEET phụ: Revenue Chart =====
            if (chart.getLabels()!=null && !chart.getLabels().isEmpty()) {
                XSSFSheet sChart = wb.createSheet("Revenue Chart");
                Row hd = sChart.createRow(0);
                hd.setHeightInPoints(22);
                set(sChart, hd, 0, "Nhóm", st.header);
                set(sChart, hd, 1, "Doanh thu", st.header);

                for (int i = 0; i < chart.getLabels().size(); i++) {
                    Row row = sChart.createRow(i + 1);
                    set(sChart, row, 0, chart.getLabels().get(i), st.td);
                    setNum(sChart, row, 1, nz(chart.getData().get(i)), st.moneyRight);
                }

                XSSFDrawing drawing = sChart.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0,0,0,0,3,1, 11, 24);
                XSSFChart xssfChart = drawing.createChart(anchor);
                xssfChart.setTitleText("Doanh thu theo " + groupBy);
                xssfChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);

                XDDFCategoryAxis bottom = xssfChart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis left = xssfChart.createValueAxis(AxisPosition.LEFT);
                left.setCrosses(AxisCrosses.AUTO_ZERO);
                bottom.crossAxis(left); left.crossAxis(bottom);

                int last = chart.getLabels().size();
                XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(
                        sChart, new CellRangeAddress(1, last, 0, 0));
                XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(
                        sChart, new CellRangeAddress(1, last, 1, 1));

                XDDFChartData data = xssfChart.createData(ChartTypes.BAR, bottom, left);
                ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
                XDDFChartData.Series s = data.addSeries(xs, ys);
                s.setTitle("Doanh thu (VND)", null);
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

                // ---- SHEET: Báo cáo CTKM ----
                XSSFSheet ps1 = wb.createSheet("Báo cáo CTKM");
                ps1.setDisplayGridlines(false);
                ps1.setPrintGridlines(false);
                PrintSetup psP = ps1.getPrintSetup();
                psP.setLandscape(true);
                psP.setPaperSize(PrintSetup.A4_PAPERSIZE);
                ps1.setMargin(Sheet.LeftMargin, 0.4);
                ps1.setMargin(Sheet.RightMargin, 0.4);
                ps1.setMargin(Sheet.TopMargin, 0.6);
                ps1.setMargin(Sheet.BottomMargin, 0.6);

                int pr = 0;
                Row pr1 = ps1.createRow(pr++);
                set(ps1, pr1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
                ps1.addMergedRegion(new CellRangeAddress(0,0,0,11));

                Row pr2 = ps1.createRow(pr++);
                set(ps1, pr2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
                ps1.addMergedRegion(new CellRangeAddress(1,1,0,11));

                Row pr3 = ps1.createRow(pr++);
                set(ps1, pr3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
                ps1.addMergedRegion(new CellRangeAddress(2,2,0,11));

                pr++;
                Row ptitle = ps1.createRow(pr++);
                set(ps1, ptitle, 0, "BÁO CÁO TỔNG KẾT CTKM", st.title);
                ps1.addMergedRegion(new CellRangeAddress(ptitle.getRowNum(), ptitle.getRowNum(), 0, 11));

                Row prange = ps1.createRow(pr++);
                set(ps1, prange, 0,
                        "Thời gian: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + " → " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.centerGrey);
                ps1.addMergedRegion(new CellRangeAddress(prange.getRowNum(), prange.getRowNum(), 0, 11));
                pr++;

                Row ps1a = ps1.createRow(pr++);
                set(ps1, ps1a, 0, "Tổng lượt áp dụng (redemptions)", st.th);
                setNum(ps1, ps1a, 1, nz(pSummary.getTotalRedemptions()), st.tdRight);

                Row ps1b = ps1.createRow(pr++);
                set(ps1, ps1b, 0, "Số user dùng CTKM (unique)", st.th);
                setNum(ps1, ps1b, 1, nz(pSummary.getUniqueUsers()), st.tdRight);

                Row ps1c = ps1.createRow(pr++);
                set(ps1, ps1c, 0, "Tổng giảm giá (VND)", st.th);
                setNum(ps1, ps1c, 1, nz(pSummary.getTotalDiscountGranted()), st.moneyRight);

                Row ps1d = ps1.createRow(pr++);
                set(ps1, ps1d, 0, "Doanh thu sau giảm (VND)", st.th);
                setNum(ps1, ps1d, 1, nz(pSummary.getTotalFinalAmount()), st.moneyRight);

                st.addBoxBorder(ps1, ps1a.getRowNum(), ps1d.getRowNum(), 0, 1);
                pr++;

                int pHeaderRow = pr;
                Row ph = ps1.createRow(pr++);
                set(ps1, ph, 0, "Promotion ID", st.header);
                set(ps1, ph, 1, "Line ID", st.header);
                set(ps1, ph, 2, "Tên Line", st.header);
                set(ps1, ph, 3, "Loại", st.header);
                set(ps1, ph, 4, "Lượt dùng", st.header);
                set(ps1, ph, 5, "Giảm giá (VND)", st.header);
                set(ps1, ph, 6, "Giá gốc (VND)", st.header);
                set(ps1, ph, 7, "Thu (sau giảm)", st.header);

                long totalRedem = 0, totalDisc = 0, totalOri = 0, totalFin = 0;
                for (PromotionLineStatsResponse s : lineStats) {
                    Row row = ps1.createRow(pr++);
                    set(ps1, row, 0, nvl(s.getPromotionId(), ""), st.td);
                    set(ps1, row, 1, nvl(s.getPromotionLineId(), ""), st.td);
                    set(ps1, row, 2, nvl(s.getPromotionLineName(), ""), st.td);
                    set(ps1, row, 3, nvl(s.getType(), ""), st.tdCenter);
                    setNum(ps1, row, 4, nz(s.getRedemptions()), st.tdRight);
                    setNum(ps1, row, 5, nz(s.getTotalDiscount()), st.moneyRight);
                    setNum(ps1, row, 6, nz(s.getTotalOriginal()), st.moneyRight);
                    setNum(ps1, row, 7, nz(s.getTotalFinal()), st.moneyRight);

                    totalRedem += (s.getRedemptions()==null?0:s.getRedemptions());
                    totalDisc  += (s.getTotalDiscount()==null?0:s.getTotalDiscount());
                    totalOri   += (s.getTotalOriginal()==null?0:s.getTotalOriginal());
                    totalFin   += (s.getTotalFinal()==null?0:s.getTotalFinal());
                }
                Row ptotal = ps1.createRow(pr++);
                set(ps1, ptotal, 2, "TỔNG CỘNG:", st.totalLeft);
                setNum(ps1, ptotal, 4, totalRedem, st.totalRight);
                setNum(ps1, ptotal, 5, totalDisc,  st.totalRight);
                setNum(ps1, ptotal, 6, totalOri,   st.totalRight);
                setNum(ps1, ptotal, 7, totalFin,   st.totalRight);

                st.addBoxBorder(ps1, pHeaderRow, pr-1, 0, 7);

                ps1.setColumnWidth(0, 20*256);
                ps1.setColumnWidth(1, 24*256);
                ps1.setColumnWidth(2, 26*256);
                ps1.setColumnWidth(3, 12*256);
                ps1.setColumnWidth(4, 12*256);
                ps1.setColumnWidth(5, 16*256);
                ps1.setColumnWidth(6, 16*256);
                ps1.setColumnWidth(7, 16*256);

                // ---- SHEET: Top Voucher ----
                XSSFSheet ps2 = wb.createSheet("Top Voucher");
                Row h2 = ps2.createRow(0);
                set(ps2, h2, 0, "Voucher", st.header);
                set(ps2, h2, 1, "Promotion ID", st.header);
                set(ps2, h2, 2, "Line ID", st.header);
                set(ps2, h2, 3, "Lượt dùng", st.header);
                set(ps2, h2, 4, "User duy nhất", st.header);
                set(ps2, h2, 5, "Giảm giá (VND)", st.header);
                set(ps2, h2, 6, "Giá gốc", st.header);
                set(ps2, h2, 7, "Thu sau giảm", st.header);
                set(ps2, h2, 8, "Max usage", st.header);
                set(ps2, h2, 9, "Đã dùng", st.header);
                set(ps2, h2,10, "First use", st.header);
                set(ps2, h2,11, "Last use", st.header);

                int rr = 1;
                DateTimeFormatter dF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (VoucherUsageItemResponse v : topVouchers) {
                    Row row = ps2.createRow(rr++);
                    set(ps2, row, 0, nvl(v.getVoucherCode(), ""), st.td);
                    set(ps2, row, 1, nvl(v.getPromotionId(), ""), st.td);
                    set(ps2, row, 2, nvl(v.getPromotionLineId(), ""), st.td);
                    setNum(ps2, row, 3, nz(v.getUses()), st.tdRight);
                    setNum(ps2, row, 4, nz(v.getUniqueUsers()), st.tdRight);
                    setNum(ps2, row, 5, nz(v.getTotalDiscount()), st.moneyRight);
                    setNum(ps2, row, 6, nz(v.getTotalOriginal()), st.moneyRight);
                    setNum(ps2, row, 7, nz(v.getTotalFinal()), st.moneyRight);
                    setNum(ps2, row, 8, nz(v.getMaxUsage()==null?0L:v.getMaxUsage().longValue()), st.tdRight);
                    setNum(ps2, row, 9, nz(v.getUsedCount()==null?0L:v.getUsedCount().longValue()), st.tdRight);
                    set(ps2, row,10, v.getFirstUse()==null? "" : v.getFirstUse().format(dF), st.tdCenter);
                    set(ps2, row,11, v.getLastUse()==null ? "" : v.getLastUse().format(dF),  st.tdCenter);
                    if ((rr % 2) == 0) st.paintZebra(row, 0, 11);
                }
                for (int cc = 0; cc <= 11; cc++) ps2.setColumnWidth(cc, (cc<=2?22:14)*256);

                // ---- SHEET: CTKM Chart ----
                if (usageChart != null && usageChart.getLabels()!=null && !usageChart.getLabels().isEmpty()) {
                    XSSFSheet ps3 = wb.createSheet("CTKM Chart");
                    Row hd2 = ps3.createRow(0);
                    set(ps3, hd2, 0, "Ngày/Nhóm",  st.header);
                    set(ps3, hd2, 1, "Lượt dùng",  st.header);
                    set(ps3, hd2, 2, "Giảm giá",   st.header);

                    for (int i = 0; i < usageChart.getLabels().size(); i++) {
                        Row row = ps3.createRow(i + 1);
                        set(ps3, row, 0, usageChart.getLabels().get(i), st.td);
                        setNum(ps3, row, 1, nz(usageChart.getRedemptions().get(i)), st.tdRight);
                        setNum(ps3, row, 2, nz(usageChart.getDiscountAmounts().get(i)), st.moneyRight);
                    }

                    XSSFDrawing dr = ps3.createDrawingPatriarch();
                    XSSFClientAnchor anchor = dr.createAnchor(0,0,0,0,4,1, 14, 22);
                    XSSFChart xChart = dr.createChart(anchor);
                    xChart.setTitleText("Sử dụng CTKM theo ngày");
                    xChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);

                    XDDFCategoryAxis bottom = xChart.createCategoryAxis(AxisPosition.BOTTOM);
                    XDDFValueAxis left = xChart.createValueAxis(AxisPosition.LEFT);
                    bottom.crossAxis(left); left.crossAxis(bottom);

                    int last = usageChart.getLabels().size();
                    XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(
                            ps3, new CellRangeAddress(1, last, 0, 0));
                    XDDFNumericalDataSource<Double> ys1 = XDDFDataSourcesFactory.fromNumericCellRange(
                            ps3, new CellRangeAddress(1, last, 1, 1));
                    XDDFNumericalDataSource<Double> ys2 = XDDFDataSourcesFactory.fromNumericCellRange(
                            ps3, new CellRangeAddress(1, last, 2, 2));

                    XDDFChartData data = xChart.createData(ChartTypes.LINE, bottom, left);
                    data.addSeries(xs, ys1).setTitle("Lượt dùng", null);
                    data.addSeries(xs, ys2).setTitle("Giảm giá (VND)", null);
                    xChart.plot(data);

                    ps3.setDisplayGridlines(false);
                    ps3.setColumnWidth(0, 18*256);
                    ps3.setColumnWidth(1, 14*256);
                    ps3.setColumnWidth(2, 14*256);
                }
            }

            // ===== Xuất HTTP =====
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String cd = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", cd);
            try (ServletOutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
        }
    }




    // ================= PROMOTION EXPORT =================

    /** Xuất Excel báo cáo khuyến mãi theo khoảng ngày */
    public void exportPromotionReportRange(HttpServletResponse response,
                                           LocalDate start,
                                           LocalDate end,
                                           int topVoucherLimit,
                                           String companyName,
                                           String companyAddress) throws IOException {

        // ---- Lấy dữ liệu BE đã sẵn có ----
        PromoStatsSummaryResponse summary = dataAnalyzerService.getPromotionSummary(start, end);
        List<PromotionLineStatsResponse> lineStats =
                dataAnalyzerService.getPromotionLineStats(start, end, null);
        List<VoucherUsageItemResponse> topVouchers =
                dataAnalyzerService.getVoucherLeaderboard(start, end, Math.max(1, topVoucherLimit));
        PromotionRangeChartResponse usageChart =
                dataAnalyzerService.getPromotionUsageByRange(start, end, GroupByDataAnalzerResponse.DAY);

        String fileName = "BaoCao_CTKM_" + start + "_" + end + ".xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ===== SHEET 1: Tổng kết CTKM =====
            XSSFSheet sheet = wb.createSheet("Báo cáo CTKM");
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
            // Header công ty
            Row r1 = sheet.createRow(r++);
            set(sheet, r1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
            sheet.addMergedRegion(new CellRangeAddress(0,0,0,11));

            Row r2 = sheet.createRow(r++);
            set(sheet, r2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(1,1,0,11));

            Row r3 = sheet.createRow(r++);
            set(sheet, r3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(2,2,0,11));

            // Title + Range
            r++;
            Row title = sheet.createRow(r++);
            set(sheet, title, 0, "BÁO CÁO TỔNG KẾT CTKM", st.title);
            sheet.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, 11));

            Row range = sheet.createRow(r++);
            set(sheet, range, 0,
                    "Thời gian: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + " → " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    st.centerGrey);
            sheet.addMergedRegion(new CellRangeAddress(range.getRowNum(), range.getRowNum(), 0, 11));
            r++;

            // ===== Thẻ tóm tắt (4 hàng) =====
            Row s1 = sheet.createRow(r++);
            set(sheet, s1, 0, "Tổng lượt áp dụng (redemptions)", st.th);
            setNum(sheet, s1, 1, nz(summary.getTotalRedemptions()), st.tdRight);

            Row s2 = sheet.createRow(r++);
            set(sheet, s2, 0, "Số user dùng CTKM (unique)", st.th);
            setNum(sheet, s2, 1, nz(summary.getUniqueUsers()), st.tdRight);

            Row s3 = sheet.createRow(r++);
            set(sheet, s3, 0, "Tổng giảm giá (VND)", st.th);
            setNum(sheet, s3, 1, nz(summary.getTotalDiscountGranted()), st.moneyRight);

            Row s4 = sheet.createRow(r++);
            set(sheet, s4, 0, "Doanh thu sau giảm (VND)", st.th);
            setNum(sheet, s4, 1, nz(summary.getTotalFinalAmount()), st.moneyRight);

            st.addBoxBorder(sheet, s1.getRowNum(), s4.getRowNum(), 0, 1);
            r++;

            // ===== Bảng chi tiết theo Promotion Line =====
            int headerRow = r;
            Row head = sheet.createRow(r++);
            set(sheet, head, 0,  "Promotion ID",      st.header);
            set(sheet, head, 1,  "Line ID",           st.header);
            set(sheet, head, 2,  "Tên Line",          st.header);
            set(sheet, head, 3,  "Loại",              st.header); // VOUCHER/PACKAGE
            set(sheet, head, 4,  "Lượt dùng",         st.header);
            set(sheet, head, 5,  "Giảm giá (VND)",    st.header);
            set(sheet, head, 6,  "Giá gốc (VND)",     st.header);
            set(sheet, head, 7,  "Thu (sau giảm)",    st.header);

            int startRow = r;
            long totalRedem = 0, totalDisc = 0, totalOri = 0, totalFin = 0;

            for (PromotionLineStatsResponse s : lineStats) {
                Row row = sheet.createRow(r++);
                set(sheet, row, 0,  nvl(s.getPromotionId(), ""), st.td);
                set(sheet, row, 1,  nvl(s.getPromotionLineId(), ""), st.td);
                set(sheet, row, 2,  nvl(s.getPromotionLineName(), ""), st.td);
                set(sheet, row, 3,  nvl(s.getType(), ""), st.tdCenter);
                setNum(sheet, row, 4, nz(s.getRedemptions()), st.tdRight);
                setNum(sheet, row, 5, nz(s.getTotalDiscount()), st.moneyRight);
                setNum(sheet, row, 6, nz(s.getTotalOriginal()), st.moneyRight);
                setNum(sheet, row, 7, nz(s.getTotalFinal()), st.moneyRight);

                totalRedem += (s.getRedemptions()==null?0:s.getRedemptions());
                totalDisc  += (s.getTotalDiscount()==null?0:s.getTotalDiscount());
                totalOri   += (s.getTotalOriginal()==null?0:s.getTotalOriginal());
                totalFin   += (s.getTotalFinal()==null?0:s.getTotalFinal());
            }

            // Hàng tổng
            Row total = sheet.createRow(r++);
            set(sheet, total, 2, "TỔNG CỘNG:", st.totalLeft);
            setNum(sheet, total, 4, totalRedem, st.totalRight);
            setNum(sheet, total, 5, totalDisc,  st.totalRight);
            setNum(sheet, total, 6, totalOri,   st.totalRight);
            setNum(sheet, total, 7, totalFin,   st.totalRight);

            st.addBoxBorder(sheet, headerRow, r-1, 0, 7);

            // Width
            sheet.setColumnWidth(0, 20*256);
            sheet.setColumnWidth(1, 24*256);
            sheet.setColumnWidth(2, 26*256);
            sheet.setColumnWidth(3, 12*256);
            sheet.setColumnWidth(4, 12*256);
            sheet.setColumnWidth(5, 16*256);
            sheet.setColumnWidth(6, 16*256);
            sheet.setColumnWidth(7, 16*256);

            // ===== SHEET 2: Top Voucher =====
            XSSFSheet s2Sheet = wb.createSheet("Top Voucher");
            Row h2 = s2Sheet.createRow(0);
            set(s2Sheet, h2, 0, "Voucher",        st.header);
            set(s2Sheet, h2, 1, "Promotion ID",   st.header);
            set(s2Sheet, h2, 2, "Line ID",        st.header);
            set(s2Sheet, h2, 3, "Lượt dùng",      st.header);
            set(s2Sheet, h2, 4, "User duy nhất",  st.header);
            set(s2Sheet, h2, 5, "Giảm giá (VND)", st.header);
            set(s2Sheet, h2, 6, "Giá gốc",        st.header);
            set(s2Sheet, h2, 7, "Thu sau giảm",   st.header);
            set(s2Sheet, h2, 8, "Max usage",      st.header);
            set(s2Sheet, h2, 9, "Đã dùng",        st.header);
            set(s2Sheet, h2,10, "First use",      st.header);
            set(s2Sheet, h2,11, "Last use",       st.header);

            int rr = 1;
            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (VoucherUsageItemResponse v : topVouchers) {
                Row row = s2Sheet.createRow(rr++);
                set(s2Sheet, row, 0, nvl(v.getVoucherCode(), ""), st.td);
                set(s2Sheet, row, 1, nvl(v.getPromotionId(), ""), st.td);
                set(s2Sheet, row, 2, nvl(v.getPromotionLineId(), ""), st.td);
                setNum(s2Sheet, row, 3, nz(v.getUses()), st.tdRight);
                setNum(s2Sheet, row, 4, nz(v.getUniqueUsers()), st.tdRight);
                setNum(s2Sheet, row, 5, nz(v.getTotalDiscount()), st.moneyRight);
                setNum(s2Sheet, row, 6, nz(v.getTotalOriginal()), st.moneyRight);
                setNum(s2Sheet, row, 7, nz(v.getTotalFinal()), st.moneyRight);
                setNum(s2Sheet, row, 8, nz(v.getMaxUsage()==null?0L:v.getMaxUsage().longValue()), st.tdRight);
                setNum(s2Sheet, row, 9, nz(v.getUsedCount()==null?0L:v.getUsedCount().longValue()), st.tdRight);
                set(s2Sheet, row,10, v.getFirstUse()==null? "" : v.getFirstUse().format(dFmt), st.tdCenter);
                set(s2Sheet, row,11, v.getLastUse()==null ? "" : v.getLastUse().format(dFmt),  st.tdCenter);
                if ((rr % 2) == 0) st.paintZebra(row, 0, 11);
            }
            for (int c = 0; c <= 11; c++) s2Sheet.setColumnWidth(c, (c<=2?22:14)*256);

            // ===== SHEET 3: Usage Chart (lượt dùng & giảm giá) =====
            if (usageChart != null && usageChart.getLabels()!=null && !usageChart.getLabels().isEmpty()) {
                XSSFSheet sChart = wb.createSheet("CTKM Chart");
                Row hd = sChart.createRow(0);
                set(sChart, hd, 0, "Ngày/Nhóm",  st.header);
                set(sChart, hd, 1, "Lượt dùng",  st.header);
                set(sChart, hd, 2, "Giảm giá",   st.header);

                for (int i = 0; i < usageChart.getLabels().size(); i++) {
                    Row row = sChart.createRow(i + 1);
                    set(sChart, row, 0, usageChart.getLabels().get(i), st.td);
                    setNum(sChart, row, 1, nz(usageChart.getRedemptions().get(i)), st.tdRight);     // <== sửa
                    setNum(sChart, row, 2, nz(usageChart.getDiscountAmounts().get(i)), st.moneyRight); // <== sửa
                }


                XSSFDrawing drawing = sChart.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0,0,0,0,4,1, 14, 22);
                XSSFChart xssfChart = drawing.createChart(anchor);
                xssfChart.setTitleText("Sử dụng CTKM theo ngày");
                xssfChart.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);

                XDDFCategoryAxis bottom = xssfChart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis left = xssfChart.createValueAxis(AxisPosition.LEFT);
                bottom.crossAxis(left); left.crossAxis(bottom);

                int last = usageChart.getLabels().size();
                XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(
                        sChart, new CellRangeAddress(1, last, 0, 0));
                XDDFNumericalDataSource<Double> ys1 = XDDFDataSourcesFactory.fromNumericCellRange(
                        sChart, new CellRangeAddress(1, last, 1, 1));
                XDDFNumericalDataSource<Double> ys2 = XDDFDataSourcesFactory.fromNumericCellRange(
                        sChart, new CellRangeAddress(1, last, 2, 2));

                XDDFChartData data = xssfChart.createData(ChartTypes.LINE, bottom, left);
                data.addSeries(xs, ys1).setTitle("Lượt dùng", null);
                data.addSeries(xs, ys2).setTitle("Giảm giá (VND)", null);
                xssfChart.plot(data);

                sChart.setDisplayGridlines(false);
                sChart.setColumnWidth(0, 18*256);
                sChart.setColumnWidth(1, 14*256);
                sChart.setColumnWidth(2, 14*256);
            }

            // ===== HTTP Response =====
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String cd = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", cd);
            try (ServletOutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
        }
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
