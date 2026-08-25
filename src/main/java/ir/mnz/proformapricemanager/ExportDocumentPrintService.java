package ir.mnz.proformapricemanager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExportDocumentPrintService {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MAX_ITEMS_PER_PAGE = 7;

    private static final DecimalFormat NUMBER =
            new DecimalFormat(
                    "#,##0.###",
                    DecimalFormatSymbols.getInstance(Locale.US)
            );

    private static final DecimalFormat MONEY =
            new DecimalFormat(
                    "#,##0.00",
                    DecimalFormatSymbols.getInstance(Locale.US)
            );

    private ExportDocumentPrintService() {
    }

    public enum PriceType {

        SYSTEM,
        SECOND
    }

    public enum DocumentKind {

        PROFORMA(
                "PROFORMA INVOICE",
                "P/I Number",
                "P/I Date"
        ),

        INVOICE(
                "COMMERCIAL INVOICE",
                "Invoice Number",
                "Invoice Date"
        );

        private final String title;
        private final String numberLabel;
        private final String dateLabel;

        DocumentKind(
                String title,
                String numberLabel,
                String dateLabel
        ) {
            this.title = title;
            this.numberLabel = numberLabel;
            this.dateLabel = dateLabel;
        }

        public String title() {
            return title;
        }

        public String numberLabel() {
            return numberLabel;
        }

        public String dateLabel() {
            return dateLabel;
        }
    }

    public record Item(
            int itemNo,
            String partCode,
            String description,
            double qty,
            String qtyOnePallet,
            String palletTotal,
            double pricePer1000,
            double totalPrice
    ) {
    }

    public record PrintData(
            DocumentKind kind,
            String code,
            String revision,
            String documentNo,
            String documentDate,
            String customerCode,
            String customerName,
            String customerAddress,
            String payment,
            String delivery,
            String packing,
            String grossWeight,
            String salesManager,
            String managingDirector,
            String footerText,
            boolean letterhead,
            double letterheadTopMm,
            int fontSize,
            PriceType priceType,
            List<Item> items
    ) {

        public PrintData {
            items = items == null
                    ? List.of()
                    : List.copyOf(items);
        }

        public double grandTotal() {

            double total = 0.0;

            for (Item item : items) {
                total += item.totalPrice();
            }

            return total;
        }
    }

    private static int getBaseFontSize(
            PrintData data
    ) {

        return Math.max(
                7,
                Math.min(
                        16,
                        data.fontSize()
                )
        );
    }

    // =========================================================
    // Preview
    // =========================================================

    public static void showPreview(
            Window owner,
            PrintData data
    ) {

        PreviewDialog dialog =
                new PreviewDialog(
                        owner,
                        data
                );

        /*
         * Preview یک JDialog واقعی و Owner-aware است.
         * به همین دلیل همیشه روی فرم تنظیمات چاپ می‌ماند
         * و پشت پنجره Modal قبلی نمی‌رود.
         */
        dialog.setVisible(true);
    }

    // =========================================================
    // Print
    // =========================================================

    public static void print(
            Component parent,
            PrintData data
    ) {

        PrinterJob job =
                PrinterJob.getPrinterJob();

        PageFormat format =
                createA4PageFormat(
                        job
                );

        job.setPrintable(
                new DocumentPrintable(
                        data
                ),
                format
        );

        if (!job.printDialog()) {
            return;
        }

        try {

            job.print();

        } catch (PrinterException ex) {

            JOptionPane.showMessageDialog(
                    parent,
                    "خطا در چاپ:\n\n"
                            + ex.getMessage(),
                    "خطای چاپ",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static PageFormat createA4PageFormat(
            PrinterJob job
    ) {

        PageFormat format =
                job.defaultPage();

        Paper paper =
                new Paper();

        // A4 in points (72 dpi)
        paper.setSize(
                PAGE_WIDTH,
                PAGE_HEIGHT
        );

        // حدود 7 میلی‌متر حاشیه
        paper.setImageableArea(
                20,
                20,
                PAGE_WIDTH - 40,
                PAGE_HEIGHT - 40
        );

        format.setPaper(paper);
        format.setOrientation(
                PageFormat.PORTRAIT
        );

        return format;
    }

    // =========================================================
    // Printable
    // =========================================================

    private static final class DocumentPrintable
            implements Printable {

        private final PrintData data;

        private DocumentPrintable(
                PrintData data
        ) {
            this.data = data;
        }

        @Override
        public int print(
                Graphics graphics,
                PageFormat pageFormat,
                int pageIndex
        ) {

            int pageCount =
                    getPageCount(
                            data
                    );

            if (pageIndex < 0
                    || pageIndex >= pageCount) {

                return NO_SUCH_PAGE;
            }

            Graphics2D g =
                    (Graphics2D)
                            graphics.create();

            try {

                configureGraphics(g);

                double availableWidth =
                        pageFormat
                                .getImageableWidth();

                double availableHeight =
                        pageFormat
                                .getImageableHeight();

                double scale =
                        Math.min(
                                availableWidth
                                        / PAGE_WIDTH,
                                availableHeight
                                        / PAGE_HEIGHT
                        );

                g.translate(
                        pageFormat.getImageableX(),
                        pageFormat.getImageableY()
                );

                g.scale(
                        scale,
                        scale
                );

                drawPage(
                        g,
                        data,
                        pageIndex
                );

                return PAGE_EXISTS;

            } finally {

                g.dispose();
            }
        }
    }

    // =========================================================
    // Rendering
    // =========================================================

    private static void drawPage(
            Graphics2D g,
            PrintData data,
            int pageIndex
    ) {

        configureGraphics(g);

        g.setColor(Color.WHITE);

        g.fillRect(
                0,
                0,
                PAGE_WIDTH,
                PAGE_HEIGHT
        );

        int left = 24;
        int right = PAGE_WIDTH - 24;
        int width = right - left;

        int topOffset =
                getTopOffsetPoints(
                        data
                );

        // -----------------------------------------------------
        // Title
        // -----------------------------------------------------

        PriceType priceType =
                data.priceType() == null
                        ? PriceType.SECOND
                        : data.priceType();

        Color headerBackground =
                data.kind() == DocumentKind.INVOICE
                        // Commercial Invoice: نارنجی روشن
                        ? new Color(255, 237, 213)
                        : (
                            priceType == PriceType.SYSTEM
                                    // Proforma + System Price: آبی روشن
                                    ? new Color(219, 234, 254)
                                    // Proforma + Second Price: صورتی روشن
                                    : new Color(252, 231, 243)
                        );

        Color headerForeground =
                data.kind() == DocumentKind.INVOICE
                        // Commercial Invoice: نارنجی پررنگ
                        ? new Color(194, 65, 12)
                        : (
                            priceType == PriceType.SYSTEM
                                    // Proforma + System Price
                                    ? new Color(29, 78, 216)
                                    // Proforma + Second Price
                                    : new Color(190, 24, 93)
                        );

        g.setColor(
                headerBackground
        );

        g.fillRoundRect(
                left,
                10 + topOffset,
                width,
                32,
                10,
                10
        );

        g.setColor(
                headerForeground
        );

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        getBaseFontSize(data) + 8
                )
        );

        drawCentered(
                g,
                data.kind().title(),
                PAGE_WIDTH / 2,
                31 + topOffset
        );

        g.setColor(Color.BLACK);

        // -----------------------------------------------------
        // Meta
        // -----------------------------------------------------

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        Math.max(7, getBaseFontSize(data) - 1)
                )
        );

        drawText(
                g,
                "Code: "
                        + safe(data.code()),
                left + 4,
                51 + topOffset
        );

        drawText(
                g,
                "Revision No: "
                        + safe(data.revision()),
                left + 4,
                65 + topOffset
        );

        drawRightText(
                g,
                data.kind().numberLabel()
                        + " : "
                        + safe(data.documentNo()),
                right - 4,
                51 + topOffset
        );

        drawRightText(
                g,
                data.kind().dateLabel()
                        + " : "
                        + safe(data.documentDate()),
                right - 4,
                65 + topOffset
        );

        drawLine(
                g,
                left,
                72 + topOffset,
                right,
                72 + topOffset
        );

        // -----------------------------------------------------
        // Customer
        // -----------------------------------------------------

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        Math.max(7, getBaseFontSize(data) - 1)
                )
        );

        drawText(
                g,
                "Customer:",
                left + 4,
                86 + topOffset
        );

        String customerName =
                safe(
                        data.customerName()
                );

        if (!safe(data.customerCode())
                .isBlank()) {

            customerName +=
                    "   [" + data.customerCode() + "]";
        }

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        getBaseFontSize(data)
                )
        );

        drawText(
                g,
                customerName,
                left + 4,
                101 + topOffset
        );

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        Math.max(7, getBaseFontSize(data) - 1)
                )
        );

        drawWrappedText(
                g,
                data.customerAddress(),
                left + 4,
                115 + topOffset,
                width - 8,
                12,
                5
        );

        drawLine(
                g,
                left,
                170 + topOffset,
                right,
                170 + topOffset
        );

        // -----------------------------------------------------
        // Table
        // -----------------------------------------------------

        int tableY =
                180 + topOffset;

        drawItemsTable(
                g,
                data,
                pageIndex,
                left,
                tableY,
                width
        );

        // -----------------------------------------------------
        // Footer only on last page
        // -----------------------------------------------------

        int pageCount =
                getPageCount(
                        data
                );

        boolean lastPage =
                pageIndex
                        == pageCount - 1;

        if (lastPage) {

            drawLastPageFooter(
                    g,
                    data,
                    left,
                    right
            );
        }

        // -----------------------------------------------------
        // Page number
        // -----------------------------------------------------

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        Math.max(6, getBaseFontSize(data) - 2)
                )
        );

        g.setColor(
                new Color(
                        100,
                        100,
                        100
                )
        );

        drawCentered(
                g,
                "Page "
                        + (pageIndex + 1)
                        + " / "
                        + pageCount,
                PAGE_WIDTH / 2,
                822
        );
    }

    private static void drawItemsTable(
            Graphics2D g,
            PrintData data,
            int pageIndex,
            int x,
            int y,
            int width
    ) {

        int[] widths = {
                32,   // Item
                190,  // Description
                68,   // One pallet
                58,   // pallets
                64,   // total qty
                63,   // price/1000
                width - 475 // total
        };

        String[] headers = {
                "Item",
                "Description",
                "Qty/pcs.\nOne Pallet",
                "Qty/pallet\nTotal",
                "Qty/pcs.\nTotal",
                "Price/1000\n$",
                "Total Price\n$"
        };

        int headerHeight = 38;
        int rowHeight = 44;

        int currentX =
                x;

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        Math.max(6, getBaseFontSize(data) - 2)
                )
        );

        for (
                int i = 0;
                i < widths.length;
                i++
        ) {

            Color tableHeaderBackground =
                    data.kind() == DocumentKind.INVOICE
                            // Commercial Invoice: نارنجی
                            ? new Color(
                                    251,
                                    146,
                                    60
                            )
                            : (
                                data.priceType() == PriceType.SYSTEM
                                        // Proforma + System Price: آبی
                                        ? new Color(
                                                147,
                                                197,
                                                253
                                        )
                                        // Proforma + Second Price: صورتی
                                        : new Color(
                                                244,
                                                114,
                                                182
                                        )
                            );

            g.setColor(
                    tableHeaderBackground
            );

            g.fillRect(
                    currentX,
                    y,
                    widths[i],
                    headerHeight
            );

            g.setColor(Color.BLACK);

            g.drawRect(
                    currentX,
                    y,
                    widths[i],
                    headerHeight
            );

            drawCellMultilineCentered(
                    g,
                    headers[i],
                    currentX,
                    y,
                    widths[i],
                    headerHeight
            );

            currentX +=
                    widths[i];
        }

        int itemsPerPage =
                getItemsPerPage(
                        data
                );

        int from =
                pageIndex
                        * itemsPerPage;

        int to =
                Math.min(
                        from + itemsPerPage,
                        data.items().size()
                );

        int rowY =
                y + headerHeight;

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        getBaseFontSize(data)
                )
        );

        for (
                int row = from;
                row < to;
                row++
        ) {

            Item item =
                    data.items()
                            .get(row);

            currentX = x;

            String description =
                    safe(
                            item.description()
                    );

            if (!safe(item.partCode())
                    .isBlank()) {

                description +=
                        "\n("
                                + item.partCode()
                                + ")";
            }

            String[] values = {
                    String.valueOf(item.itemNo()),
                    description,
                    safe(item.qtyOnePallet()),
                    safe(item.palletTotal()),
                    NUMBER.format(item.qty()),
                    NUMBER.format(item.pricePer1000()),
                    MONEY.format(item.totalPrice())
            };

            for (
                    int col = 0;
                    col < widths.length;
                    col++
            ) {

                g.setColor(Color.WHITE);

                g.fillRect(
                        currentX,
                        rowY,
                        widths[col],
                        rowHeight
                );

                g.setColor(Color.BLACK);

                g.drawRect(
                        currentX,
                        rowY,
                        widths[col],
                        rowHeight
                );

                if (col == 1) {

                    drawCellMultilineLeft(
                            g,
                            values[col],
                            currentX,
                            rowY,
                            widths[col],
                            rowHeight
                    );

                } else {

                    drawCellMultilineCentered(
                            g,
                            values[col],
                            currentX,
                            rowY,
                            widths[col],
                            rowHeight
                    );
                }

                currentX +=
                        widths[col];
            }

            rowY +=
                    rowHeight;
        }

        // Empty rows, so printed form remains visually consistent.
        for (
                int row = to - from;
                row < itemsPerPage;
                row++
        ) {

            currentX = x;

            for (
                    int col = 0;
                    col < widths.length;
                    col++
            ) {

                g.setColor(Color.WHITE);

                g.fillRect(
                        currentX,
                        rowY,
                        widths[col],
                        rowHeight
                );

                g.setColor(
                        new Color(
                                215,
                                215,
                                215
                        )
                );

                g.drawRect(
                        currentX,
                        rowY,
                        widths[col],
                        rowHeight
                );

                currentX +=
                        widths[col];
            }

            rowY +=
                    rowHeight;
        }
    }

    private static void drawLastPageFooter(
            Graphics2D g,
            PrintData data,
            int left,
            int right
    ) {

        int width =
                right - left;

        int y = 540;

        g.setColor(Color.BLACK);

        g.drawRect(
                left,
                y,
                width,
                25
        );

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        getBaseFontSize(data) + 1
                )
        );

        drawText(
                g,
                "Amount",
                left + 35,
                y + 16
        );

        drawRightText(
                g,
                MONEY.format(
                        data.grandTotal()
                ),
                right - 8,
                y + 16
        );

        y += 25;

        g.drawRect(
                left,
                y,
                width,
                25
        );

        drawText(
                g,
                "Total FCA",
                left + 35,
                y + 16
        );

        drawRightText(
                g,
                MONEY.format(
                        data.grandTotal()
                ),
                right - 8,
                y + 16
        );

        // -----------------------------------------------------
        // Terms
        // -----------------------------------------------------

        y += 38;

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        getBaseFontSize(data)
                )
        );

        drawText(
                g,
                "Payment: "
                        + safe(data.payment()),
                left + 35,
                y
        );

        drawText(
                g,
                "Delivery: "
                        + safe(data.delivery()),
                left + 35,
                y + 15
        );

        drawText(
                g,
                "Packing: "
                        + safe(data.packing()),
                left + 35,
                y + 30
        );

        drawText(
                g,
                "Gross Weight: "
                        + safe(data.grossWeight()),
                left + 35,
                y + 45
        );

        // -----------------------------------------------------
        // Signatures
        // -----------------------------------------------------

        /*
         * دو باکس امضا:
         *
         * Proforma Invoice:
         *   Accent = آبی روشن
         *
         * Commercial Invoice:
         *   Accent = زیتونی / طلایی ملایم
         *
         * رنگ Header اصلی و Header جدول همچنان بر اساس
         * System Price / Second Price باقی می‌ماند.
         */
        Color signatureAccent =
                data.kind()
                        == DocumentKind.PROFORMA
                        ? new Color(
                                219,
                                234,
                                254
                        )
                        // Commercial Invoice: نارنجی روشن
                        : new Color(
                                255,
                                237,
                                213
                        );

        Color signatureBorder =
                data.kind()
                        == DocumentKind.PROFORMA
                        ? new Color(
                                96,
                                165,
                                250
                        )
                        // Commercial Invoice: نارنجی پررنگ
                        : new Color(
                                234,
                                88,
                                12
                        );

        int signY =
                y + 66;

        int signWidth =
                155;

        int signHeight =
                80;

        int signGap =
                54;

        int signaturesTotalWidth =
                signWidth * 2
                        + signGap;

        int firstSignX =
                left
                        + (width - signaturesTotalWidth)
                        / 2;

        int secondSignX =
                firstSignX
                        + signWidth
                        + signGap;

        drawSignatureBox(
                g,
                firstSignX,
                signY,
                signWidth,
                signHeight,
                "Sales Manager",
                safe(
                        data.salesManager()
                ),
                signatureAccent,
                signatureBorder,
                getBaseFontSize(data)
        );

        drawSignatureBox(
                g,
                secondSignX,
                signY,
                signWidth,
                signHeight,
                "Managing Director",
                safe(
                        data.managingDirector()
                ),
                signatureAccent,
                signatureBorder,
                getBaseFontSize(data)
        );

        // -----------------------------------------------------
        // Certification footer
        // -----------------------------------------------------

        g.setFont(
                new Font(
                        "Serif",
                        Font.ITALIC,
                        Math.max(7, getBaseFontSize(data) - 1)
                )
        );

        g.setColor(
                new Color(
                        45,
                        45,
                        45
                )
        );

        drawWrappedCenteredText(
                g,
                data.footerText(),
                PAGE_WIDTH / 2,
                772,
                width - 50,
                12,
                3
        );

        g.setColor(Color.BLACK);

        drawLine(
                g,
                left,
                798,
                right,
                798
        );
    }

    private static void drawSignatureBox(
            Graphics2D g,
            int x,
            int y,
            int width,
            int height,
            String role,
            String name,
            Color accent,
            Color border,
            int fontSize
    ) {

        // عنوان Signature بالای کادر
        g.setColor(
                new Color(
                        45,
                        45,
                        45
                )
        );

        g.setFont(
                new Font(
                        "Serif",
                        Font.ITALIC,
                        Math.max(
                                7,
                                fontSize - 1
                        )
                )
        );

        drawCentered(
                g,
                "Signature:",
                x + width / 2,
                y - 5
        );

        // کادر
        g.setColor(
                Color.WHITE
        );

        g.fillRect(
                x,
                y,
                width,
                height
        );

        g.setColor(
                border
        );

        g.drawRect(
                x,
                y,
                width,
                height
        );

        // نوار عنوان کادر
        int roleHeight =
                22;

        g.setColor(
                accent
        );

        g.fillRect(
                x + 1,
                y + 1,
                width - 1,
                roleHeight
        );

        g.setColor(
                Color.BLACK
        );

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        fontSize
                )
        );

        drawCentered(
                g,
                role,
                x + width / 2,
                y + 16
        );

        // نام
        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        fontSize
                )
        );

        drawCentered(
                g,
                name,
                x + width / 2,
                y + 40
        );
    }

    // =========================================================
    // Preview Dialog
    // =========================================================

    private static final class PreviewDialog
            extends JDialog {

        private final PrintData data;

        private Rectangle normalBounds;

        private boolean maximized;

        private final JLabel imageLabel =
                new JLabel();

        private final JLabel pageLabel =
                new JLabel(
                        "",
                        SwingConstants.CENTER
                );

        private int pageIndex =
                0;

        private PreviewDialog(
                Window owner,
                PrintData data
        ) {

            super(
                    owner,
                    "پیش‌نمایش چاپ",
                    ModalityType.APPLICATION_MODAL
            );

            this.data =
                    data;

            setDefaultCloseOperation(
                    DISPOSE_ON_CLOSE
            );

            /*
             * JDialog در ویندوز دکمه Maximize استاندارد ندارد،
             * ولی قابل Resize است و دکمه بزرگ‌نمایی اختصاصی
             * در نوار پایین Preview اضافه شده است.
             */
            setResizable(true);

            setMinimumSize(
                    new Dimension(
                            700,
                            600
                    )
            );

            setSize(
                    850,
                    760
            );

            setLocationRelativeTo(owner);

            setLayout(
                    new BorderLayout(
                            0,
                            8
                    )
            );

            imageLabel.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            JScrollPane scroll =
                    new JScrollPane(
                            imageLabel
                    );

            scroll.getVerticalScrollBar()
                    .setUnitIncrement(20);

            add(
                    scroll,
                    BorderLayout.CENTER
            );

            add(
                    createToolbar(),
                    BorderLayout.SOUTH
            );

            refreshPage();
        }

        private JPanel createToolbar() {

            JPanel panel =
                    new JPanel(
                            new BorderLayout()
                    );

            JPanel navigation =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.CENTER,
                                    6,
                                    6
                            )
                    );

            JButton btnPrevious =
                    new JButton("صفحه قبل");

            JButton btnNext =
                    new JButton("صفحه بعد");

            btnPrevious.addActionListener(
                    e -> {

                        if (pageIndex > 0) {

                            pageIndex--;
                            refreshPage();
                        }
                    }
            );

            btnNext.addActionListener(
                    e -> {

                        if (pageIndex
                                < getPageCount(data) - 1) {

                            pageIndex++;
                            refreshPage();
                        }
                    }
            );

            navigation.add(btnPrevious);
            navigation.add(pageLabel);
            navigation.add(btnNext);

            JButton btnMaximize =
                    new JButton("بزرگ‌نمایی");

            btnMaximize.addActionListener(
                    e -> toggleMaximize(
                            btnMaximize
                    )
            );

            JButton btnPrint =
                    new JButton("چاپ");

            btnPrint.setFont(
                    btnPrint.getFont()
                            .deriveFont(
                                    Font.BOLD
                            )
            );

            btnPrint.addActionListener(
                    e -> ExportDocumentPrintService
                            .print(
                                    this,
                                    data
                            )
            );

            JButton btnClose =
                    new JButton("بستن");

            btnClose.addActionListener(
                    e -> dispose()
            );

            JPanel actions =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.LEFT,
                                    6,
                                    6
                            )
                    );

            actions.add(btnMaximize);
            actions.add(btnPrint);
            actions.add(btnClose);

            panel.add(
                    navigation,
                    BorderLayout.CENTER
            );

            panel.add(
                    actions,
                    BorderLayout.WEST
            );

            return panel;
        }

        private void toggleMaximize(
                JButton button
        ) {

            if (!maximized) {

                normalBounds =
                        getBounds();

                GraphicsConfiguration gc =
                        getGraphicsConfiguration();

                Rectangle screenBounds =
                        gc.getBounds();

                Insets insets =
                        Toolkit
                                .getDefaultToolkit()
                                .getScreenInsets(
                                        gc
                                );

                int x =
                        screenBounds.x
                                + insets.left;

                int y =
                        screenBounds.y
                                + insets.top;

                int width =
                        screenBounds.width
                                - insets.left
                                - insets.right;

                int height =
                        screenBounds.height
                                - insets.top
                                - insets.bottom;

                setBounds(
                        x,
                        y,
                        width,
                        height
                );

                maximized =
                        true;

                button.setText(
                        "اندازه قبلی"
                );

            } else {

                if (normalBounds != null) {

                    setBounds(
                            normalBounds
                    );
                }

                maximized =
                        false;

                button.setText(
                        "بزرگ‌نمایی"
                );
            }
        }

        private void refreshPage() {

            int totalPages =
                    getPageCount(
                            data
                    );

            pageLabel.setText(
                    "صفحه "
                            + (pageIndex + 1)
                            + " از "
                            + totalPages
            );

            imageLabel.setIcon(
                    new ImageIcon(
                            renderPreviewPage(
                                    data,
                                    pageIndex
                            )
                    )
            );
        }
    }

    private static BufferedImage renderPreviewPage(
            PrintData data,
            int pageIndex
    ) {

        double scale =
                1.25;

        BufferedImage image =
                new BufferedImage(
                        (int) Math.round(
                                PAGE_WIDTH * scale
                        ),
                        (int) Math.round(
                                PAGE_HEIGHT * scale
                        ),
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g =
                image.createGraphics();

        try {

            configureGraphics(g);

            g.scale(
                    scale,
                    scale
            );

            drawPage(
                    g,
                    data,
                    pageIndex
            );

        } finally {

            g.dispose();
        }

        return image;
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static int getPageCount(
            PrintData data
    ) {

        if (data.items().isEmpty()) {
            return 1;
        }

        int itemsPerPage =
                getItemsPerPage(
                        data
                );

        return (int)
                Math.ceil(
                        data.items().size()
                                / (double) itemsPerPage
                );
    }

    private static int getItemsPerPage(
            PrintData data
    ) {

        int topOffset =
                getTopOffsetPoints(
                        data
                );

        /*
         * Footer صفحه آخر از y=540 شروع می‌شود.
         * در حالت سربرگ، اقلام کمتری در هر صفحه قرار می‌گیرند
         * تا جدول با بخش Amount / Total FCA تداخل نداشته باشد.
         */
        int availableRowsHeight =
                526
                        - (180 + topOffset)
                        - 38;

        int rows =
                availableRowsHeight
                        / 44;

        return Math.max(
                1,
                Math.min(
                        MAX_ITEMS_PER_PAGE,
                        rows
                )
        );
    }

    private static int getTopOffsetPoints(
            PrintData data
    ) {

        if (!data.letterhead()) {

            return 0;
        }

        double mm =
                Math.max(
                        0.0,
                        Math.min(
                                80.0,
                                data.letterheadTopMm()
                        )
                );

        return (int)
                Math.round(
                        mm
                                * 72.0
                                / 25.4
                );
    }

    private static void configureGraphics(
            Graphics2D g
    ) {

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setStroke(
                new BasicStroke(
                        0.7f
                )
        );
    }

    private static String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private static void drawLine(
            Graphics2D g,
            int x1,
            int y1,
            int x2,
            int y2
    ) {

        g.drawLine(
                x1,
                y1,
                x2,
                y2
        );
    }

    private static void drawText(
            Graphics2D g,
            String text,
            int x,
            int baseline
    ) {

        g.drawString(
                safe(text),
                x,
                baseline
        );
    }

    private static void drawRightText(
            Graphics2D g,
            String text,
            int rightX,
            int baseline
    ) {

        String value =
                safe(text);

        int width =
                g.getFontMetrics()
                        .stringWidth(value);

        g.drawString(
                value,
                rightX - width,
                baseline
        );
    }

    private static void drawCentered(
            Graphics2D g,
            String text,
            int centerX,
            int baseline
    ) {

        String value =
                safe(text);

        int width =
                g.getFontMetrics()
                        .stringWidth(value);

        g.drawString(
                value,
                centerX - width / 2,
                baseline
        );
    }

    private static void drawCellMultilineCentered(
            Graphics2D g,
            String text,
            int x,
            int y,
            int width,
            int height
    ) {

        List<String> lines =
                splitLines(
                        text
                );

        FontMetrics metrics =
                g.getFontMetrics();

        int lineHeight =
                metrics.getHeight();

        int blockHeight =
                lines.size()
                        * lineHeight;

        int baseline =
                y
                        + (height - blockHeight) / 2
                        + metrics.getAscent();

        for (String line : lines) {

            int lineWidth =
                    metrics.stringWidth(
                            line
                    );

            g.drawString(
                    line,
                    x
                            + (width - lineWidth) / 2,
                    baseline
            );

            baseline +=
                    lineHeight;
        }
    }

    private static void drawCellMultilineLeft(
            Graphics2D g,
            String text,
            int x,
            int y,
            int width,
            int height
    ) {

        List<String> lines =
                new ArrayList<>();

        for (
                String paragraph
                : splitLines(text)
        ) {

            lines.addAll(
                    wrapLine(
                            g,
                            paragraph,
                            width - 10
                    )
            );
        }

        if (lines.size() > 3) {

            lines =
                    new ArrayList<>(
                            lines.subList(
                                    0,
                                    3
                            )
                    );
        }

        FontMetrics metrics =
                g.getFontMetrics();

        int lineHeight =
                metrics.getHeight();

        int blockHeight =
                lines.size()
                        * lineHeight;

        int baseline =
                y
                        + (height - blockHeight) / 2
                        + metrics.getAscent();

        for (String line : lines) {

            g.drawString(
                    line,
                    x + 5,
                    baseline
            );

            baseline +=
                    lineHeight;
        }
    }

    private static int drawWrappedText(
            Graphics2D g,
            String text,
            int x,
            int y,
            int maxWidth,
            int lineHeight,
            int maxLines
    ) {

        List<String> allLines =
                new ArrayList<>();

        for (
                String paragraph
                : splitLines(text)
        ) {

            allLines.addAll(
                    wrapLine(
                            g,
                            paragraph,
                            maxWidth
                    )
            );
        }

        int currentY =
                y;

        int count =
                0;

        for (String line : allLines) {

            if (count >= maxLines) {
                break;
            }

            g.drawString(
                    line,
                    x,
                    currentY
            );

            currentY +=
                    lineHeight;

            count++;
        }

        return currentY;
    }

    private static void drawWrappedCenteredText(
            Graphics2D g,
            String text,
            int centerX,
            int y,
            int maxWidth,
            int lineHeight,
            int maxLines
    ) {

        List<String> lines =
                new ArrayList<>();

        for (
                String paragraph
                : splitLines(text)
        ) {

            lines.addAll(
                    wrapLine(
                            g,
                            paragraph,
                            maxWidth
                    )
            );
        }

        int currentY =
                y;

        for (
                int i = 0;
                i < lines.size()
                        && i < maxLines;
                i++
        ) {

            drawCentered(
                    g,
                    lines.get(i),
                    centerX,
                    currentY
            );

            currentY +=
                    lineHeight;
        }
    }

    private static List<String> wrapLine(
            Graphics2D g,
            String text,
            int maxWidth
    ) {

        List<String> result =
                new ArrayList<>();

        String value =
                safe(text);

        if (value.isEmpty()) {

            result.add("");
            return result;
        }

        String[] words =
                value.split("\\s+");

        StringBuilder line =
                new StringBuilder();

        for (String word : words) {

            String candidate =
                    line.length() == 0
                            ? word
                            : line + " " + word;

            if (
                    g.getFontMetrics()
                            .stringWidth(candidate)
                            <= maxWidth
            ) {

                line.setLength(0);
                line.append(candidate);

            } else {

                if (line.length() > 0) {

                    result.add(
                            line.toString()
                    );
                }

                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {

            result.add(
                    line.toString()
            );
        }

        return result;
    }

    private static List<String> splitLines(
            String text
    ) {

        String value =
                safe(text);

        String[] parts =
                value.split("\\R", -1);

        List<String> result =
                new ArrayList<>();

        for (String part : parts) {
            result.add(part);
        }

        return result;
    }
}
