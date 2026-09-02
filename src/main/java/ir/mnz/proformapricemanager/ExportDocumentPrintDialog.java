package ir.mnz.proformapricemanager;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExportDocumentPrintDialog {

    private ExportDocumentPrintDialog() {
    }

    private enum PriceSource {

        SYSTEM("قیمت سیستم"),
        SECOND("قیمت دوم");

        private final String title;

        PriceSource(
                String title
        ) {

            this.title =
                    title;
        }

        @Override
        public String toString() {

            return title;
        }
    }

    public record SourceItem(
            long partRef,
            int itemNo,
            String partCode,
            String partName,
            String latinName,
            double qty,
            Double qtyOnePallet,
            Double palletTotal,

            double systemPricePer1000,
            double systemTotalPrice,

            Double secondPricePer1000,
            Double secondTotalPrice
    ) {
    }

    public static void showDialog(
            Frame owner,
            CompanyConfig company,
            ExportDocumentPrintService.DocumentKind kind,
            String documentNo,
            String documentDate,
            String customerCode,
            String customerName,
            String customerAddress,
            List<SourceItem> sourceItems
    ) {

        PrintDialog dialog =
                new PrintDialog(
                        owner,
                        company,
                        kind,
                        documentNo,
                        documentDate,
                        customerCode,
                        customerName,
                        customerAddress,
                        sourceItems
                );

        dialog.setVisible(true);
    }

    // =========================================================
    // Dialog
    // =========================================================

    private static final class PrintDialog
            extends JDialog {

        private final CompanyConfig company;

        private final DocumentRepository documentRepository =
                new DocumentRepository();

        private final ExportDocumentPrintService.DocumentKind kind;
        private final String documentNo;
        private final String documentDate;
        private final String customerCode;
        private final List<SourceItem> sourceItems;

        private final JTextField txtCode =
                new JTextField("CF 13");

        private final JTextField txtRevision =
                new JTextField("01");

        private final JTextField txtCustomerName =
                new JTextField();

        private final JTextArea txtCustomerAddress =
                new JTextArea();

        private final JTextField txtPayment =
                new JTextField();

        private final JTextField txtDelivery =
                new JTextField();

        private final JTextField txtPacking =
                new JTextField(
                        "Standard Export Packing"
                );

        private final JTextField txtGrossWeight =
                new JTextField();

        private final JTextField txtSalesManager =
                new JTextField("Neda Agah");

        private final JTextField txtManagingDirector =
                new JTextField("Hadi Mousavi");

        private final JTextArea txtFooter =
                new JTextArea();

        private final JCheckBox chkLetterhead =
                new JCheckBox(
                        "فعال",
                        true
                );

        private final JSpinner spnLetterheadTopMm =
                new JSpinner(
                        new SpinnerNumberModel(
                                45,
                                0,
                                80,
                                5
                        )
                );

        private final JSpinner spnPrintFontSize =
                new JSpinner(
                        new SpinnerNumberModel(
                                11,
                                7,
                                16,
                                1
                        )
                );


        private final JComboBox<PriceSource> cmbPriceSource =
                new JComboBox<>(
                        PriceSource.values()
                );

        private final DecimalFormat numberFormat =
                new DecimalFormat(
                    "#,##0.###",
                    DecimalFormatSymbols.getInstance(Locale.US)
            );

        private final DefaultTableModel palletModel =
                new DefaultTableModel(
                        new Object[]{
                                "ردیف",
                                "کد کالا",
                                "نام فارسی کالا",
                                "نام لاتین کالا",
                                "تعداد",
                                "تعداد در هر پالت",
                                "تعداد پالت",
                                "قیمت / 1000",
                                "مبلغ"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {

                        return column == 3
                                || column == 5
                                || column == 6;
                    }
                };

        private final JTable palletTable =
                new JTable(
                        palletModel
                );

        private PrintDialog(
                Frame owner,
                CompanyConfig company,
                ExportDocumentPrintService.DocumentKind kind,
                String documentNo,
                String documentDate,
                String customerCode,
                String customerName,
                String customerAddress,
                List<SourceItem> sourceItems
        ) {

            super(
                    owner,
                    kind == ExportDocumentPrintService.DocumentKind.PROFORMA
                            ? "چاپ پیش‌فاکتور صادراتی"
                            : "چاپ فاکتور صادراتی",
                    true
            );

            this.company =
                    company;

            this.kind =
                    kind;

            this.documentNo =
                    documentNo;

            this.documentDate =
                    documentDate;

            this.customerCode =
                    customerCode;

            List<SourceItem> safeSourceItems =
                    sourceItems == null
                            ? List.of()
                            : List.copyOf(
                                    sourceItems
                            );

            /*
             * پیش‌فاکتور دقیقاً با همان ردیف‌های ERP باقی می‌ماند.
             *
             * Commercial Invoice باید اقلام تکراری یک کالا را
             * در یک ردیف نمایش دهد؛ بنابراین فقط برای INVOICE
             * ردیف‌ها را قبل از نمایش/چاپ تجمیع می‌کنیم.
             *
             * Query اصلی فاکتور دست نمی‌خورد چون VchItmId برای
             * ثبت قیمت دوم و ارتباط با ERP لازم است.
             */
            this.sourceItems =
                    kind == ExportDocumentPrintService.DocumentKind.INVOICE
                            ? aggregateCommercialInvoiceItems(
                                    safeSourceItems
                            )
                            : safeSourceItems;

            txtCustomerName.setText(
                    safe(
                            customerName
                    )
            );

            txtCustomerAddress.setText(
                    safe(
                            customerAddress
                    )
            );

            setDefaults();

            initializeUi();

            // رفتار فعلی حفظ می‌شود: پیش‌فرض چاپ با قیمت دوم است.
            cmbPriceSource.setSelectedItem(
                    PriceSource.SECOND
            );

            cmbPriceSource.addActionListener(
                    e -> refreshPriceColumns()
            );

            loadItems();

            setSize(
                    1000,
                    720
            );

            setMinimumSize(
                    new Dimension(
                            900,
                            650
                    )
            );

            setLocationRelativeTo(owner);
        }

        // =========================================================
        // Commercial Invoice aggregation
        // =========================================================

        /**
         * ردیف‌های تکراری Commercial Invoice را تجمیع می‌کند.
         *
         * Group Key:
         * - PartRef
         * - System Price / 1000
         * - Second Price / 1000
         *
         * بنابراین اگر یک کالا با دو قیمت متفاوت در یک فاکتور
         * وجود داشته باشد، عمداً در دو ردیف جدا چاپ می‌شود.
         */
        private List<SourceItem> aggregateCommercialInvoiceItems(
                List<SourceItem> items
        ) {

            if (items == null
                    || items.isEmpty()) {

                return List.of();
            }

            Map<CommercialGroupKey, CommercialAccumulator> grouped =
                    new LinkedHashMap<>();

            for (
                    SourceItem item
                    : items
            ) {

                CommercialGroupKey key =
                        new CommercialGroupKey(
                                item.partRef(),
                                decimalKey(
                                        item.systemPricePer1000()
                                ),
                                decimalKey(
                                        item.secondPricePer1000()
                                )
                        );

                grouped.computeIfAbsent(
                        key,
                        ignored ->
                                new CommercialAccumulator(
                                        item
                                )
                ).add(
                        item
                );
            }

            List<SourceItem> result =
                    new ArrayList<>(
                            grouped.size()
                    );

            int itemNo = 1;

            for (
                    CommercialAccumulator accumulator
                    : grouped.values()
            ) {

                result.add(
                        accumulator.toSourceItem(
                                itemNo++
                        )
                );
            }

            return List.copyOf(
                    result
            );
        }

        /**
         * کلید قیمت را به شکل Decimal پایدار می‌سازیم تا اختلاف‌های
         * نمایشی Double باعث جدا شدن اشتباه ردیف‌های یکسان نشود.
         */
        private String decimalKey(
                Double value
        ) {

            if (value == null) {
                return "<NULL>";
            }

            return BigDecimal
                    .valueOf(
                            value
                    )
                    .stripTrailingZeros()
                    .toPlainString();
        }

        private String decimalKey(
                double value
        ) {

            return BigDecimal
                    .valueOf(
                            value
                    )
                    .stripTrailingZeros()
                    .toPlainString();
        }

        private record CommercialGroupKey(
                long partRef,
                String systemPrice,
                String secondPrice
        ) {
        }

        private static final class CommercialAccumulator {

            private final long partRef;
            private final String partCode;
            private final String partName;
            private final String latinName;

            private final double systemPricePer1000;
            private final Double secondPricePer1000;

            private double qty;
            private double systemTotalPrice;
            private Double secondTotalPrice;

            private Double qtyOnePallet;
            private boolean qtyOnePalletConsistent = true;

            private double palletTotalSum;
            private boolean allPalletTotalsAvailable = true;

            private CommercialAccumulator(
                    SourceItem first
            ) {

                this.partRef =
                        first.partRef();

                this.partCode =
                        first.partCode();

                this.partName =
                        first.partName();

                this.latinName =
                        first.latinName();

                this.systemPricePer1000 =
                        first.systemPricePer1000();

                this.secondPricePer1000 =
                        first.secondPricePer1000();

                this.qtyOnePallet =
                        first.qtyOnePallet();

                this.secondTotalPrice =
                        first.secondTotalPrice() == null
                                ? null
                                : 0.0;
            }

            private void add(
                    SourceItem item
            ) {

                qty +=
                        item.qty();

                systemTotalPrice +=
                        item.systemTotalPrice();

                if (secondTotalPrice != null) {

                    if (item.secondTotalPrice() == null) {

                        secondTotalPrice =
                                null;

                    } else {

                        secondTotalPrice +=
                                item.secondTotalPrice();
                    }
                }

                if (!sameNullableNumber(
                        qtyOnePallet,
                        item.qtyOnePallet()
                )) {

                    qtyOnePalletConsistent =
                            false;
                }

                if (item.palletTotal() == null) {

                    allPalletTotalsAvailable =
                            false;

                } else {

                    palletTotalSum +=
                            item.palletTotal();
                }
            }

            private SourceItem toSourceItem(
                    int itemNo
            ) {

                Double resolvedQtyOnePallet =
                        qtyOnePalletConsistent
                                ? qtyOnePallet
                                : null;

                Double resolvedPalletTotal =
                        null;

                /*
                 * اگر نسبت پالت برای تمام ردیف‌ها یکسان باشد،
                 * تعداد پالت را از Qty تجمیعی محاسبه می‌کنیم.
                 */
                if (resolvedQtyOnePallet != null
                        && resolvedQtyOnePallet > 0) {

                    resolvedPalletTotal =
                            qty
                                    / resolvedQtyOnePallet;

                } else if (allPalletTotalsAvailable) {

                    resolvedPalletTotal =
                            palletTotalSum;
                }

                return new SourceItem(
                        partRef,
                        itemNo,
                        partCode,
                        partName,
                        latinName,
                        qty,
                        resolvedQtyOnePallet,
                        resolvedPalletTotal,

                        systemPricePer1000,
                        systemTotalPrice,

                        secondPricePer1000,
                        secondTotalPrice
                );
            }

            private static boolean sameNullableNumber(
                    Double a,
                    Double b
            ) {

                if (a == null
                        && b == null) {

                    return true;
                }

                if (a == null
                        || b == null) {

                    return false;
                }

                return BigDecimal
                        .valueOf(
                                a
                        )
                        .compareTo(
                                BigDecimal.valueOf(
                                        b
                                )
                        )
                        == 0;
            }
        }

        private void setDefaults() {

            if (kind
                    == ExportDocumentPrintService.DocumentKind.PROFORMA) {

                txtPayment.setText(
                        "90 days from proforma date"
                );

                txtDelivery.setText(
                        "10 days from proforma invoice"
                );

                txtFooter.setText(
                        "This is to certify that this proforma invoice is correct and true and the quoted prices are in accordance with our general sales conditions and the goods are of Iran origin."
                );

            } else {

                txtPayment.setText(
                        "As per agreed terms"
                );

                txtDelivery.setText(
                        "As per agreed shipment terms"
                );

                txtFooter.setText(
                        "We certify that this commercial invoice is true and correct and that the goods described above are of Iran origin."
                );
            }
        }

        private void initializeUi() {

            setDefaultCloseOperation(
                    DISPOSE_ON_CLOSE
            );

            JPanel root =
                    new JPanel(
                            new BorderLayout(
                                    0,
                                    12
                            )
                    );

            root.setBorder(
                    new EmptyBorder(
                            16,
                            16,
                            16,
                            16
                    )
            );

            root.add(
                    createHeader(),
                    BorderLayout.NORTH
            );

            root.add(
                    createBody(),
                    BorderLayout.CENTER
            );

            root.add(
                    createActions(),
                    BorderLayout.SOUTH
            );

            setContentPane(root);

            getContentPane()
                    .applyComponentOrientation(
                            ComponentOrientation.RIGHT_TO_LEFT
                    );
        }

        private JPanel createHeader() {

            JPanel panel =
                    new JPanel(
                            new BorderLayout()
                    );

            panel.setBorder(
                    new EmptyBorder(
                            0,
                            0,
                            4,
                            0
                    )
            );

            JLabel title =
                    new JLabel(
                            kind == ExportDocumentPrintService.DocumentKind.PROFORMA
                                    ? "تنظیمات چاپ پیش‌فاکتور صادراتی"
                                    : "تنظیمات چاپ فاکتور صادراتی"
                    );

            title.setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            20
                    )
            );

            JLabel doc =
                    new JLabel(
                            "شماره "
                                    + documentNo
                                    + "   |   تاریخ "
                                    + safe(documentDate)
                                    + "   |   مشتری "
                                    + safe(customerNameForHeader())
                    );

            doc.setForeground(
                    new Color(
                            100,
                            116,
                            139
                    )
            );

            JPanel text =
                    new JPanel();

            text.setOpaque(false);

            text.setLayout(
                    new BoxLayout(
                            text,
                            BoxLayout.Y_AXIS
                    )
            );

            title.setAlignmentX(
                    Component.RIGHT_ALIGNMENT
            );

            doc.setAlignmentX(
                    Component.RIGHT_ALIGNMENT
            );

            text.add(title);
            text.add(
                    Box.createVerticalStrut(5)
            );
            text.add(doc);

            panel.add(
                    text,
                    BorderLayout.EAST
            );

            return panel;
        }

        private JPanel createBody() {

            JPanel body =
                    new JPanel(
                            new BorderLayout(
                                    0,
                                    12
                            )
                    );

            body.add(
                    createInfoPanel(),
                    BorderLayout.NORTH
            );

            body.add(
                    createPalletPanel(),
                    BorderLayout.CENTER
            );

            return body;
        }

        private JPanel createInfoPanel() {

            JPanel panel =
                    new JPanel(
                            new GridBagLayout()
                    );

            panel.setBorder(
                    BorderFactory.createTitledBorder(
                            "اطلاعات تکمیلی چاپ"
                    )
            );

            GridBagConstraints gbc =
                    new GridBagConstraints();

            gbc.insets =
                    new Insets(
                            5,
                            6,
                            5,
                            6
                    );

            gbc.fill =
                    GridBagConstraints.HORIZONTAL;

            gbc.weightx =
                    0;

            int row =
                    0;

            addField(
                    panel,
                    gbc,
                    row++,
                    "کد فرم",
                    txtCode,
                    "Revision",
                    txtRevision
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "نام مشتری",
                    txtCustomerName,
                    "",
                    new JLabel("")
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "Payment",
                    txtPayment,
                    "Delivery",
                    txtDelivery
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "Packing",
                    txtPacking,
                    "وزن ناخالص",
                    txtGrossWeight
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "Sales Manager",
                    txtSalesManager,
                    "Managing Director",
                    txtManagingDirector
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "چاپ روی کاغذ سربرگ‌دار",
                    chkLetterhead,
                    "فاصله از بالای کاغذ (mm)",
                    spnLetterheadTopMm
            );

            addField(
                    panel,
                    gbc,
                    row++,
                    "اندازه فونت چاپ",
                    spnPrintFontSize,
                    "محدوده پیشنهادی",
                    new JLabel("9 تا 12")
            );


            addField(
                    panel,
                    gbc,
                    row++,
                    "مبنای قیمت چاپ",
                    cmbPriceSource,
                    "انتخاب",
                    new JLabel(
                            "قیمت سیستم یا قیمت دوم"
                    )
            );

            JLabel addressLabel =
                    createLabel(
                            "آدرس مشتری"
                    );

            gbc.gridx =
                    0;

            gbc.gridy =
                    row;

            gbc.weightx =
                    0;

            panel.add(
                    addressLabel,
                    gbc
            );

            txtCustomerAddress.setRows(3);
            txtCustomerAddress.setLineWrap(true);
            txtCustomerAddress.setWrapStyleWord(true);

            JScrollPane addressScroll =
                    new JScrollPane(
                            txtCustomerAddress
                    );

            gbc.gridx =
                    1;

            gbc.gridwidth =
                    3;

            gbc.weightx =
                    1;

            panel.add(
                    addressScroll,
                    gbc
            );

            gbc.gridwidth =
                    1;

            row++;

            JLabel footerLabel =
                    createLabel(
                            "متن پایین سند"
                    );

            gbc.gridx =
                    0;

            gbc.gridy =
                    row;

            gbc.weightx =
                    0;

            panel.add(
                    footerLabel,
                    gbc
            );

            txtFooter.setRows(2);
            txtFooter.setLineWrap(true);
            txtFooter.setWrapStyleWord(true);

            JScrollPane footerScroll =
                    new JScrollPane(
                            txtFooter
                    );

            gbc.gridx =
                    1;

            gbc.gridwidth =
                    3;

            gbc.weightx =
                    1;

            panel.add(
                    footerScroll,
                    gbc
            );

            return panel;
        }

        private void addField(
                JPanel panel,
                GridBagConstraints gbc,
                int row,
                String label1,
                JComponent component1,
                String label2,
                JComponent component2
        ) {

            gbc.gridwidth =
                    1;

            gbc.gridy =
                    row;

            gbc.gridx =
                    0;

            gbc.weightx =
                    0;

            panel.add(
                    createLabel(
                            label1
                    ),
                    gbc
            );

            gbc.gridx =
                    1;

            gbc.weightx =
                    0.5;

            panel.add(
                    component1,
                    gbc
            );

            gbc.gridx =
                    2;

            gbc.weightx =
                    0;

            panel.add(
                    createLabel(
                            label2
                    ),
                    gbc
            );

            gbc.gridx =
                    3;

            gbc.weightx =
                    0.5;

            panel.add(
                    component2,
                    gbc
            );
        }

        private JLabel createLabel(
                String text
        ) {

            JLabel label =
                    new JLabel(
                            text
                    );

            label.setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            11
                    )
            );

            return label;
        }

        private JPanel createPalletPanel() {

            JPanel panel =
                    new JPanel(
                            new BorderLayout(
                                    0,
                                    6
                            )
                    );

            panel.setBorder(
                    BorderFactory.createTitledBorder(
                            "اطلاعات پالت اقلام"
                    )
            );

            JLabel hint =
                    new JLabel(
                            "ستون‌های «نام لاتین کالا»، «تعداد در هر پالت» و «تعداد پالت» قابل ویرایش هستند."
                    );

            hint.setForeground(
                    new Color(
                            100,
                            116,
                            139
                    )
            );

            palletTable.setRowHeight(
                    30
            );

            palletTable.setFillsViewportHeight(
                    true
            );

            palletTable.setAutoCreateRowSorter(
                    false
            );

            palletTable.getTableHeader()
                    .setReorderingAllowed(
                            false
                    );

            JScrollPane scroll =
                    new JScrollPane(
                            palletTable
                    );

            panel.add(
                    hint,
                    BorderLayout.NORTH
            );

            panel.add(
                    scroll,
                    BorderLayout.CENTER
            );

            return panel;
        }

        private JPanel createActions() {

            JPanel panel =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.LEFT,
                                    8,
                                    0
                            )
                    );

            JButton btnSaveLatinName =
                    new JButton(
                            "ذخیره نام لاتین کالا"
                    );

            btnSaveLatinName.setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            12
                    )
            );

            btnSaveLatinName.addActionListener(
                    e -> saveLatinNames()
            );

            JButton btnPreview =
                    new JButton(
                            "پیش‌نمایش و چاپ"
                    );

            btnPreview.setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            12
                    )
            );

            btnPreview.setBackground(
                    new Color(
                            37,
                            99,
                            235
                    )
            );

            btnPreview.setForeground(
                    Color.WHITE
            );

            btnPreview.putClientProperty(
                    FlatClientProperties.STYLE,
                    "arc: 12; borderWidth: 0"
            );

            btnPreview.addActionListener(
                    e -> showPreview()
            );

            JButton btnClose =
                    new JButton(
                            "بستن"
                    );

            btnClose.addActionListener(
                    e -> dispose()
            );

            panel.add(btnSaveLatinName);
            panel.add(btnPreview);
            panel.add(btnClose);

            return panel;
        }

        private void loadItems() {

            palletModel.setRowCount(
                    0
            );

            for (
                    SourceItem item
                    : sourceItems
            ) {

                palletModel.addRow(
                        new Object[]{
                                item.itemNo(),
                                safe(item.partCode()),
                                safe(item.partName()),
                                safe(item.latinName()),
                                numberFormat.format(
                                        item.qty()
                                ),
                                formatNullableNumber(
                                        item.qtyOnePallet()
                                ),
                                formatNullableNumber(
                                        item.palletTotal()
                                ),
                                "",
                                ""
                        }
                );
            }

            refreshPriceColumns();
        }

        private void refreshPriceColumns() {

            PriceSource priceSource =
                    (PriceSource)
                            cmbPriceSource
                                    .getSelectedItem();

            if (priceSource == null) {

                priceSource =
                        PriceSource.SECOND;
            }

            for (
                    int row = 0;
                    row < sourceItems.size();
                    row++
            ) {

                SourceItem item =
                        sourceItems.get(
                                row
                        );

                if (priceSource
                        == PriceSource.SYSTEM) {

                    palletModel.setValueAt(
                            numberFormat.format(
                                    item.systemPricePer1000()
                            ),
                            row,
                            7
                    );

                    palletModel.setValueAt(
                            numberFormat.format(
                                    item.systemTotalPrice()
                            ),
                            row,
                            8
                    );

                } else {

                    palletModel.setValueAt(
                            item.secondPricePer1000()
                                    == null
                                    ? "-"
                                    : numberFormat.format(
                                            item.secondPricePer1000()
                                    ),
                            row,
                            7
                    );

                    palletModel.setValueAt(
                            item.secondTotalPrice()
                                    == null
                                    ? "-"
                                    : numberFormat.format(
                                            item.secondTotalPrice()
                                    ),
                            row,
                            8
                    );
                }
            }
        }

        private void showPreview() {

            if (palletTable.isEditing()) {

                if (!palletTable
                        .getCellEditor()
                        .stopCellEditing()) {

                    return;
                }
            }

            String customerName =
                    txtCustomerName
                            .getText()
                            .trim();

            if (customerName.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "نام مشتری را وارد کنید.",
                        "توجه",
                        JOptionPane.WARNING_MESSAGE
                );

                txtCustomerName.requestFocusInWindow();

                return;
            }

            PriceSource priceSource =
                    (PriceSource)
                            cmbPriceSource
                                    .getSelectedItem();

            if (priceSource == null) {

                priceSource =
                        PriceSource.SECOND;
            }

            if (priceSource
                    == PriceSource.SECOND) {

                for (
                        int row = 0;
                        row < sourceItems.size();
                        row++
                ) {

                    SourceItem source =
                            sourceItems.get(
                                    row
                            );

                    if (source.secondPricePer1000() == null
                            || source.secondPricePer1000() <= 0
                            || source.secondTotalPrice() == null) {

                        JOptionPane.showMessageDialog(
                                this,
                                "برای چاپ با قیمت دوم، قیمت دوم ردیف "
                                        + (row + 1)
                                        + " وارد نشده است.",
                                "قیمت دوم ناقص است",
                                JOptionPane.WARNING_MESSAGE
                        );

                        return;
                    }
                }
            }

            List<ExportDocumentPrintService.Item> items =
                    new ArrayList<>();

            for (
                    int row = 0;
                    row < sourceItems.size();
                    row++
            ) {

                SourceItem source =
                        sourceItems.get(
                                row
                        );

                items.add(
                        new ExportDocumentPrintService.Item(
                                source.itemNo(),
                                source.partCode(),
                                getText(
                                        palletModel.getValueAt(
                                                row,
                                                3
                                        )
                                ),
                                source.qty(),
                                getText(
                                        palletModel.getValueAt(
                                                row,
                                                5
                                        )
                                ),
                                getText(
                                        palletModel.getValueAt(
                                                row,
                                                6
                                        )
                                ),
                                priceSource
                                        == PriceSource.SYSTEM
                                        ? source.systemPricePer1000()
                                        : source.secondPricePer1000(),
                                priceSource
                                        == PriceSource.SYSTEM
                                        ? source.systemTotalPrice()
                                        : source.secondTotalPrice()
                        )
                );
            }

            ExportDocumentPrintService.PrintData data =
                    new ExportDocumentPrintService.PrintData(
                            kind,
                            txtCode
                                    .getText()
                                    .trim(),
                            txtRevision
                                    .getText()
                                    .trim(),
                            documentNo,
                            documentDate,
                            customerCode,
                            customerName,
                            txtCustomerAddress
                                    .getText()
                                    .trim(),
                            txtPayment
                                    .getText()
                                    .trim(),
                            txtDelivery
                                    .getText()
                                    .trim(),
                            txtPacking
                                    .getText()
                                    .trim(),
                            txtGrossWeight
                                    .getText()
                                    .trim(),
                            txtSalesManager
                                    .getText()
                                    .trim(),
                            txtManagingDirector
                                    .getText()
                                    .trim(),
                            txtFooter
                                    .getText()
                                    .trim(),
                            chkLetterhead.isSelected(),
                            ((Number)
                                    spnLetterheadTopMm
                                            .getValue())
                                            .doubleValue(),
                            ((Number)
                                    spnPrintFontSize
                                            .getValue())
                                            .intValue(),
                            priceSource
                                    == PriceSource.SYSTEM
                                    ? ExportDocumentPrintService.PriceType.SYSTEM
                                    : ExportDocumentPrintService.PriceType.SECOND,
                            items
                    );

            ExportDocumentPrintService
                    .showPreview(
                            this,
                            data
                    );
        }

        private void saveLatinNames() {

            if (palletTable.isEditing()) {

                if (!palletTable
                        .getCellEditor()
                        .stopCellEditing()) {

                    return;
                }
            }

            if (company == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "شرکت انتخاب نشده است.",
                        "خطا",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            List<DocumentRepository.PartLatinNameUpdate> updates =
                    new ArrayList<>();

            for (
                    int row = 0;
                    row < sourceItems.size();
                    row++
            ) {

                SourceItem source =
                        sourceItems.get(
                                row
                        );

                String latinName =
                        getText(
                                palletModel.getValueAt(
                                        row,
                                        3
                                )
                        );

                updates.add(
                        new DocumentRepository.PartLatinNameUpdate(
                                source.partRef(),
                                latinName
                        )
                );
            }

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.WAIT_CURSOR
                    )
            );

            SwingWorker<Void, Void> worker =
                    new SwingWorker<>() {

                        @Override
                        protected Void doInBackground()
                                throws Exception {

                            documentRepository
                                    .savePartLatinNames(
                                            company,
                                            updates
                                    );

                            return null;
                        }

                        @Override
                        protected void done() {

                            try {

                                get();

                                JOptionPane.showMessageDialog(
                                        PrintDialog.this,
                                        "نام لاتین کالاها با موفقیت در inv.Part ذخیره شد.",
                                        "ثبت موفق",
                                        JOptionPane.INFORMATION_MESSAGE
                                );

                            } catch (Exception ex) {

                                Throwable cause =
                                        ex.getCause() != null
                                                ? ex.getCause()
                                                : ex;

                                JOptionPane.showMessageDialog(
                                        PrintDialog.this,
                                        "خطا در ثبت نام لاتین کالا:\n\n"
                                                + (cause.getMessage() == null
                                                        ? cause.getClass().getSimpleName()
                                                        : cause.getMessage()),
                                        "خطای دیتابیس",
                                        JOptionPane.ERROR_MESSAGE
                                );

                            } finally {

                                setCursor(
                                        Cursor.getDefaultCursor()
                                );
                            }
                        }
                    };

            worker.execute();
        }

        private String formatNullableNumber(
                Double value
        ) {

            if (value == null) {

                return "";
            }

            return numberFormat.format(
                    value
            );
        }

        private String customerNameForHeader() {

            String value =
                    txtCustomerName
                            .getText()
                            .trim();

            if (!safe(customerCode)
                    .isBlank()) {

                value +=
                        " ["
                                + customerCode
                                + "]";
            }

            return value;
        }

        private String getText(
                Object value
        ) {

            return value == null
                    ? ""
                    : value
                            .toString()
                            .trim();
        }

        private String safe(
                String value
        ) {

            return value == null
                    ? ""
                    : value.trim();
        }
    }

    private static String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
