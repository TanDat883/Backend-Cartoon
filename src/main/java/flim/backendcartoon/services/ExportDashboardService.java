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
                                     String companyAddress) throws IOException {

        // --- Lấy dữ liệu ---
        RevenueSummaryResponse summary = dataAnalyzerService.getRevenueSummaryByRange(start, end);
        RevenueChartResponse chart = dataAnalyzerService.getRevenueByRange(start, end, groupBy);
        PagedResponse<RecentTransactionResponse> paged =
                dataAnalyzerService.getRecentTransactionsPaged(1, 5000, start, end);
        List<RecentTransactionResponse> txs = paged.getItems();

        String fileName = "BaoCao_DoanhThu_" + start + "_" + end + "_" + groupBy + ".xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ===== SHEET CHÍNH =====
            XSSFSheet sheet = wb.createSheet("Bảng kê Doanh thu");
            sheet.setDisplayGridlines(false);
            sheet.setPrintGridlines(false);

            // A4 landscape + margin
            PrintSetup ps = sheet.getPrintSetup();
            ps.setLandscape(true);
            ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.setMargin(Sheet.LeftMargin, 0.4);
            sheet.setMargin(Sheet.RightMargin, 0.4);
            sheet.setMargin(Sheet.TopMargin, 0.6);
            sheet.setMargin(Sheet.BottomMargin, 0.6);

            int r = 0;

            // --- Header công ty ---
            Row r1 = sheet.createRow(r++);
            set(sheet, r1, 0, nvl(companyName, "CartoonToo — Web xem phim trực tuyến"), st.hdrBoldRed);
            sheet.addMergedRegion(new CellRangeAddress(0,0,0,9));

            Row r2 = sheet.createRow(r++);
            set(sheet, r2, 0, nvl(companyAddress, "cartoontoo.example • Việt Nam"), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(1,1,0,9));

            Row r3 = sheet.createRow(r++);
            set(sheet, r3, 0, "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), st.smallGrey);
            sheet.addMergedRegion(new CellRangeAddress(2,2,0,9));

            // --- Tiêu đề ---
            r++; // trống
            Row title = sheet.createRow(r++);
            set(sheet, title, 0, "BẢNG KÊ DOANH THU", st.title);
            sheet.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, 9));

            Row range = sheet.createRow(r++);
            set(sheet, range, 0,
                    "Từ ngày: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "     Đến ngày: " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + "     (Nhóm theo: " + groupBy + ")", st.centerGrey);
            sheet.addMergedRegion(new CellRangeAddress(range.getRowNum(), range.getRowNum(), 0, 9));

            r++; // trống

            // --- Summary (2 cột) ---
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

            r++; // trống

            // ===== BẢNG CHI TIẾT SONG SONG =====
            final int TX_COL = 3;        // cột bắt đầu của bảng giao dịch (D)
            final int AMT_COL = TX_COL + 4; // cột “Số tiền (VND)”
            int headerRow = r;
            Row head = sheet.createRow(r++);
            // trái (A,B)
            set(sheet, head, 0, "Nhóm", st.header);
            set(sheet, head, 1, "Doanh thu", st.header);
            // ngăn cách (C)
            set(sheet, head, 2, "—", st.headerMuted);
            // phải (D..J)
            int c = TX_COL;
            set(sheet, head, c++, "STT", st.header);
            set(sheet, head, c++, "Mã đơn", st.header);
            set(sheet, head, c++, "Người dùng", st.header);
            set(sheet, head, c++, "Gói", st.header);
            set(sheet, head, c++, "Số tiền (VND)", st.header);
            set(sheet, head, c++, "Ngày", st.header);
            set(sheet, head, c++, "Trạng thái", st.header);

            // Ghi 2 bảng dùng chung chỉ số hàng
            int dataStart = r;

            // --- Bảng trái: nhóm-doanh thu ---
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

            // --- Bảng phải: giao dịch ---
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

                    // zebra cho phần giao dịch
                    if ((stt % 2) == 0) st.paintZebra(row, TX_COL, AMT_COL + 2);
                }
            }
            int txEnd = (txs==null || txs.isEmpty())
                    ? (dataStart - 1) : (dataStart + txs.size() - 1);

            // Hàng tổng cho cả hai bảng (cùng 1 hàng)
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

            // viền toàn khối
            st.addBoxBorder(sheet, headerRow, endRow  + 1, 0, AMT_COL + 2);

            // Freeze tiêu đề
            sheet.createFreezePane(0, headerRow + 1);

            // width cột
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

            // In lặp header
            wb.setPrintArea(wb.getSheetIndex(sheet), 0, 9, 0, endRow  + 1);
            sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, 0, 9));


            // ===== Sheet phụ: Revenue Chart =====
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

            // ===== Xuất HTTP =====
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
