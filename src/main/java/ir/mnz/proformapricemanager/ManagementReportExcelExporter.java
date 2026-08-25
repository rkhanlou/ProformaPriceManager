package ir.mnz.proformapricemanager;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ManagementReportExcelExporter {

    private ManagementReportExcelExporter() {
    }

    public record ExportInfo(
            String companyName,
            String documentType,
            String rangeTitle
    ) {
    }

    private static final class Styles {

        private final CellStyle title;
        private final CellStyle meta;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle number;
        private final CellStyle kpiTitle;
        private final CellStyle kpiNumber;
        private final CellStyle complete;
        private final CellStyle partial;
        private final CellStyle notPriced;

        private Styles(
                CellStyle title,
                CellStyle meta,
                CellStyle header,
                CellStyle text,
                CellStyle number,
                CellStyle kpiTitle,
                CellStyle kpiNumber,
                CellStyle complete,
                CellStyle partial,
                CellStyle notPriced
        ) {

            this.title = title;
            this.meta = meta;
            this.header = header;
            this.text = text;
            this.number = number;
            this.kpiTitle = kpiTitle;
            this.kpiNumber = kpiNumber;
            this.complete = complete;
            this.partial = partial;
            this.notPriced = notPriced;
        }
    }

    public static void export(
            Path target,
            ExportInfo info,
            DocumentRepository.ManagementReportData data
    ) throws IOException {

        if (target == null) {

            throw new IllegalArgumentException(
                    "مسیر فایل Excel مشخص نشده است."
            );
        }

        if (data == null
                || data.documentCount() <= 0) {

            throw new IllegalArgumentException(
                    "گزارش هیچ سندی برای خروجی Excel ندارد."
            );
        }

        Path parent =
                target
                        .toAbsolutePath()
                        .getParent();

        if (parent != null) {

            Files.createDirectories(
                    parent
            );
        }

        try (
                XSSFWorkbook workbook =
                        new XSSFWorkbook()
        ) {

            Styles styles =
                    createStyles(
                            workbook
                    );

            createSummarySheet(
                    workbook,
                    info,
                    data,
                    styles
            );

            createDetailsSheet(
                    workbook,
                    info,
                    data,
                    styles
            );

            try (
                    OutputStream out =
                            new BufferedOutputStream(
                                    Files.newOutputStream(
                                            target
                                    )
                            )
            ) {

                workbook.write(
                        out
                );
            }
        }

        if (!Files.exists(target)
                || Files.size(target) < 1000) {

            throw new IOException(
                    "فایل Excel به‌درستی ساخته نشد."
            );
        }
    }

    // =========================================================
    // Summary
    // =========================================================

    private static void createSummarySheet(
            XSSFWorkbook workbook,
            ExportInfo info,
            DocumentRepository.ManagementReportData data,
            Styles styles
    ) {

        XSSFSheet sheet =
                workbook.createSheet(
                        "خلاصه"
                );

        configureSheet(
                sheet
        );

        setSummaryWidths(
                sheet
        );

        // -----------------------------------------------------
        // Title
        // -----------------------------------------------------

        Row titleRow =
                sheet.createRow(
                        0
                );

        titleRow.setHeightInPoints(
                28
        );

        Cell titleCell =
                titleRow.createCell(
                        0
                );

        titleCell.setCellValue(
                "گزارش مدیریتی "
                        + safe(
                                info.documentType()
                        )
        );

        titleCell.setCellStyle(
                styles.title
        );

        sheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        10
                )
        );

        // -----------------------------------------------------
        // Meta
        // -----------------------------------------------------

        Row metaRow =
                sheet.createRow(
                        1
                );

        setText(
                metaRow,
                0,
                "شرکت: "
                        + safe(
                                info.companyName()
                        ),
                styles.meta
        );

        setText(
                metaRow,
                5,
                "بازه: "
                        + safe(
                                info.rangeTitle()
                        ),
                styles.meta
        );

        Row generatedRow =
                sheet.createRow(
                        2
                );

        String generatedAt =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm",
                                        Locale.US
                                )
                        );

        setText(
                generatedRow,
                0,
                "تاریخ تهیه گزارش: "
                        + generatedAt,
                styles.meta
        );

        sheet.addMergedRegion(
                new CellRangeAddress(
                        2,
                        2,
                        0,
                        10
                )
        );

        // -----------------------------------------------------
        // KPI
        // -----------------------------------------------------

        Row kpiHeader =
                sheet.createRow(
                        4
                );

        String[] kpiTitles = {
                "تعداد اسناد",
                "ثبت شده",
                "ناقص",
                "ثبت نشده",
                "مبلغ سیستم",
                "مبلغ دوم",
                "اختلاف"
        };

        for (
                int i = 0;
                i < kpiTitles.length;
                i++
        ) {

            setText(
                    kpiHeader,
                    i,
                    kpiTitles[i],
                    styles.kpiTitle
            );
        }

        Row kpiValue =
                sheet.createRow(
                        5
                );

        setNumber(
                kpiValue,
                0,
                data.documentCount(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                1,
                data.completeCount(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                2,
                data.partialCount(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                3,
                data.notPricedCount(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                4,
                data.systemTotal(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                5,
                data.secondTotal(),
                styles.kpiNumber
        );

        setNumber(
                kpiValue,
                6,
                data.differenceTotal(),
                styles.kpiNumber
        );

        // -----------------------------------------------------
        // Table
        // -----------------------------------------------------

        int headerIndex = 6;

        Row header =
                sheet.createRow(
                        headerIndex
                );

        header.setHeightInPoints(
                28
        );

        String[] headers = {
                "شماره سند",
                "سال",
                "تاریخ میلادی",
                "تاریخ شمسی",
                "کد مشتری",
                "نام مشتری",
                "تعداد اقلام",
                "وضعیت قیمت دوم",
                "مبلغ سیستم",
                "مبلغ دوم",
                "اختلاف"
        };

        for (
                int i = 0;
                i < headers.length;
                i++
        ) {

            setText(
                    header,
                    i,
                    headers[i],
                    styles.header
            );
        }

        List<DocumentRepository.ManagementReportDocument> documents =
                data.documents();

        int rowIndex =
                headerIndex + 1;

        for (
                DocumentRepository.ManagementReportDocument document
                : documents
        ) {

            Row row =
                    sheet.createRow(
                            rowIndex++
                    );

            setNumber(
                    row,
                    0,
                    document.vchNo(),
                    styles.text
            );

            setNumber(
                    row,
                    1,
                    document.year(),
                    styles.text
            );

            setText(
                    row,
                    2,
                    document.gregorianDate(),
                    styles.text
            );

            setText(
                    row,
                    3,
                    document.shamsiDate(),
                    styles.text
            );

            setText(
                    row,
                    4,
                    document.customerCode(),
                    styles.text
            );

            setText(
                    row,
                    5,
                    document.customerTitle(),
                    styles.text
            );

            setNumber(
                    row,
                    6,
                    document.itemCount(),
                    styles.text
            );

            setText(
                    row,
                    7,
                    document.priceStatus()
                            .toString(),
                    getStatusStyle(
                            document.priceStatus(),
                            styles
                    )
            );

            setNumber(
                    row,
                    8,
                    document.systemTotal(),
                    styles.number
            );

            setNumber(
                    row,
                    9,
                    document.secondTotal(),
                    styles.number
            );

            setNumber(
                    row,
                    10,
                    document.difference(),
                    styles.number
            );
        }

        sheet.createFreezePane(
                0,
                7
        );

        if (!documents.isEmpty()) {

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            headerIndex,
                            rowIndex - 1,
                            0,
                            10
                    )
            );
        }

        sheet.setSelected(
                true
        );

        workbook.setActiveSheet(
                workbook.getSheetIndex(
                        sheet
                )
        );
    }

    // =========================================================
    // Details
    // =========================================================

    private static void createDetailsSheet(
            XSSFWorkbook workbook,
            ExportInfo info,
            DocumentRepository.ManagementReportData data,
            Styles styles
    ) {

        XSSFSheet sheet =
                workbook.createSheet(
                        "ریز اقلام"
                );

        configureSheet(
                sheet
        );

        setDetailsWidths(
                sheet
        );

        Row titleRow =
                sheet.createRow(
                        0
                );

        titleRow.setHeightInPoints(
                28
        );

        Cell title =
                titleRow.createCell(
                        0
                );

        title.setCellValue(
                "ریز اقلام "
                        + safe(
                                info.documentType()
                        )
        );

        title.setCellStyle(
                styles.title
        );

        sheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        15
                )
        );

        Row metaRow =
                sheet.createRow(
                        1
                );

        setText(
                metaRow,
                0,
                "شرکت: "
                        + safe(
                                info.companyName()
                        ),
                styles.meta
        );

        setText(
                metaRow,
                5,
                "بازه: "
                        + safe(
                                info.rangeTitle()
                        ),
                styles.meta
        );

        int headerIndex =
                4;

        Row header =
                sheet.createRow(
                        headerIndex
                );

        header.setHeightInPoints(
                30
        );

        String[] headers = {
                "شماره سند",
                "سال",
                "تاریخ میلادی",
                "تاریخ شمسی",
                "کد مشتری",
                "نام مشتری",
                "ردیف",
                "کد کالا",
                "نام فارسی کالا",
                "نام لاتین کالا",
                "تعداد",
                "قیمت سیستم / 1000",
                "مبلغ سیستم",
                "قیمت دوم / 1000",
                "مبلغ دوم",
                "اختلاف"
        };

        for (
                int i = 0;
                i < headers.length;
                i++
        ) {

            setText(
                    header,
                    i,
                    headers[i],
                    styles.header
            );
        }

        List<DocumentRepository.ManagementReportItem> items =
                data.items();

        int rowIndex =
                headerIndex + 1;

        for (
                DocumentRepository.ManagementReportItem item
                : items
        ) {

            Row row =
                    sheet.createRow(
                            rowIndex++
                    );

            setNumber(
                    row,
                    0,
                    item.vchNo(),
                    styles.text
            );

            setNumber(
                    row,
                    1,
                    item.year(),
                    styles.text
            );

            setText(
                    row,
                    2,
                    item.gregorianDate(),
                    styles.text
            );

            setText(
                    row,
                    3,
                    item.shamsiDate(),
                    styles.text
            );

            setText(
                    row,
                    4,
                    item.customerCode(),
                    styles.text
            );

            setText(
                    row,
                    5,
                    item.customerTitle(),
                    styles.text
            );

            setNumber(
                    row,
                    6,
                    item.seq(),
                    styles.text
            );

            setText(
                    row,
                    7,
                    item.partCode(),
                    styles.text
            );

            setText(
                    row,
                    8,
                    item.partName(),
                    styles.text
            );

            setText(
                    row,
                    9,
                    item.latinName(),
                    styles.text
            );

            setNumber(
                    row,
                    10,
                    item.qty(),
                    styles.number
            );

            setNumber(
                    row,
                    11,
                    item.systemPricePer1000(),
                    styles.number
            );

            setNumber(
                    row,
                    12,
                    item.systemAmount(),
                    styles.number
            );

            setNullableNumber(
                    row,
                    13,
                    item.secondPricePer1000(),
                    styles.number,
                    styles.text
            );

            setNullableNumber(
                    row,
                    14,
                    item.secondAmount(),
                    styles.number,
                    styles.text
            );

            setNullableNumber(
                    row,
                    15,
                    item.difference(),
                    styles.number,
                    styles.text
            );
        }

        sheet.createFreezePane(
                0,
                5
        );

        if (!items.isEmpty()) {

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            headerIndex,
                            rowIndex - 1,
                            0,
                            15
                    )
            );
        }
    }

    // =========================================================
    // Styles
    // =========================================================

    private static Styles createStyles(
            XSSFWorkbook workbook
    ) {

        DataFormat dataFormat =
                workbook.createDataFormat();

        // -----------------------------------------------------
        // Fonts
        // -----------------------------------------------------

        Font titleFont =
                workbook.createFont();

        titleFont.setBold(
                true
        );

        titleFont.setFontHeightInPoints(
                (short) 16
        );

        titleFont.setColor(
                IndexedColors.DARK_BLUE
                        .getIndex()
        );

        Font boldFont =
                workbook.createFont();

        boldFont.setBold(
                true
        );

        boldFont.setFontHeightInPoints(
                (short) 11
        );

        Font headerFont =
                workbook.createFont();

        headerFont.setBold(
                true
        );

        headerFont.setFontHeightInPoints(
                (short) 10
        );

        headerFont.setColor(
                IndexedColors.WHITE
                        .getIndex()
        );

        // -----------------------------------------------------
        // Title
        // -----------------------------------------------------

        CellStyle title =
                workbook.createCellStyle();

        title.setFont(
                titleFont
        );

        title.setAlignment(
                HorizontalAlignment.CENTER
        );

        title.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        // -----------------------------------------------------
        // Meta
        // -----------------------------------------------------

        CellStyle meta =
                workbook.createCellStyle();

        meta.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        // -----------------------------------------------------
        // Header
        // -----------------------------------------------------

        CellStyle header =
                workbook.createCellStyle();

        header.setFont(
                headerFont
        );

        header.setFillForegroundColor(
                IndexedColors.BLUE_GREY
                        .getIndex()
        );

        header.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        header.setAlignment(
                HorizontalAlignment.CENTER
        );

        header.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        header.setWrapText(
                true
        );

        applyBorders(
                header
        );

        // -----------------------------------------------------
        // Text
        // -----------------------------------------------------

        CellStyle text =
                workbook.createCellStyle();

        text.setAlignment(
                HorizontalAlignment.CENTER
        );

        text.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        applyBorders(
                text
        );

        // -----------------------------------------------------
        // Number
        // -----------------------------------------------------

        CellStyle number =
                workbook.createCellStyle();

        number.cloneStyleFrom(
                text
        );

        number.setDataFormat(
                dataFormat.getFormat(
                        "#,##0.00"
                )
        );

        // -----------------------------------------------------
        // KPI title
        // -----------------------------------------------------

        CellStyle kpiTitle =
                workbook.createCellStyle();

        kpiTitle.cloneStyleFrom(
                header
        );

        kpiTitle.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE
                        .getIndex()
        );

        kpiTitle.setFont(
                boldFont
        );

        // -----------------------------------------------------
        // KPI number
        // -----------------------------------------------------

        CellStyle kpiNumber =
                workbook.createCellStyle();

        kpiNumber.cloneStyleFrom(
                number
        );

        kpiNumber.setFont(
                boldFont
        );

        kpiNumber.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex()
        );

        kpiNumber.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        // -----------------------------------------------------
        // Status
        // -----------------------------------------------------

        CellStyle complete =
                createStatusStyle(
                        workbook,
                        boldFont,
                        IndexedColors.LIGHT_GREEN
                );

        CellStyle partial =
                createStatusStyle(
                        workbook,
                        boldFont,
                        IndexedColors.LIGHT_YELLOW
                );

        CellStyle notPriced =
                createStatusStyle(
                        workbook,
                        boldFont,
                        IndexedColors.ROSE
                );

        return new Styles(
                title,
                meta,
                header,
                text,
                number,
                kpiTitle,
                kpiNumber,
                complete,
                partial,
                notPriced
        );
    }

    private static CellStyle createStatusStyle(
            XSSFWorkbook workbook,
            Font font,
            IndexedColors color
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFont(
                font
        );

        style.setFillForegroundColor(
                color.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        applyBorders(
                style
        );

        return style;
    }

    private static void applyBorders(
            CellStyle style
    ) {

        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );
    }

    // =========================================================
    // Sheet configuration
    // =========================================================

    private static void configureSheet(
            XSSFSheet sheet
    ) {

        sheet.setRightToLeft(
                true
        );

        sheet.setDisplayGridlines(
                true
        );

        sheet.setFitToPage(
                true
        );

        sheet.getPrintSetup()
                .setLandscape(
                        true
                );

        sheet.getPrintSetup()
                .setFitWidth(
                        (short) 1
                );

        sheet.getPrintSetup()
                .setFitHeight(
                        (short) 0
                );
    }

    private static void setSummaryWidths(
            XSSFSheet sheet
    ) {

        int[] widths = {
                13,
                9,
                16,
                16,
                14,
                32,
                13,
                18,
                18,
                18,
                18
        };

        setWidths(
                sheet,
                widths
        );
    }

    private static void setDetailsWidths(
            XSSFSheet sheet
    ) {

        int[] widths = {
                12,
                9,
                16,
                16,
                14,
                30,
                8,
                17,
                28,
                30,
                15,
                18,
                18,
                18,
                18,
                18
        };

        setWidths(
                sheet,
                widths
        );
    }

    private static void setWidths(
            XSSFSheet sheet,
            int[] widths
    ) {

        for (
                int i = 0;
                i < widths.length;
                i++
        ) {

            sheet.setColumnWidth(
                    i,
                    widths[i] * 256
            );
        }
    }

    // =========================================================
    // Cell helpers
    // =========================================================

    private static void setText(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {

        Cell cell =
                row.createCell(
                        column
                );

        cell.setCellValue(
                safe(value)
        );

        cell.setCellStyle(
                style
        );
    }

    private static void setNumber(
            Row row,
            int column,
            double value,
            CellStyle style
    ) {

        Cell cell =
                row.createCell(
                        column
                );

        cell.setCellValue(
                value
        );

        cell.setCellStyle(
                style
        );
    }

    private static void setNullableNumber(
            Row row,
            int column,
            Double value,
            CellStyle numberStyle,
            CellStyle emptyStyle
    ) {

        if (value == null) {

            setText(
                    row,
                    column,
                    "",
                    emptyStyle
            );

            return;
        }

        setNumber(
                row,
                column,
                value,
                numberStyle
        );
    }

    private static CellStyle getStatusStyle(
            DocumentRepository.PriceStatus status,
            Styles styles
    ) {

        if (status == null) {
            return styles.text;
        }

        return switch (status) {

            case COMPLETE ->
                    styles.complete;

            case PARTIAL ->
                    styles.partial;

            case NOT_PRICED ->
                    styles.notPriced;
        };
    }

    private static String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
