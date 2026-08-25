package ir.mnz.proformapricemanager;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProformaPriceFrame extends JFrame {

    // =========================================================
    // Theme
    // =========================================================

    private static final Color APP_BG = new Color(241, 245, 249);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color HEADER_BG = new Color(15, 23, 42);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color WARNING = new Color(234, 88, 12);
    private static final Color TEXT_MAIN = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color EDIT_BG = new Color(254, 249, 195);

    // =========================================================
    // Repository
    // =========================================================

    private final DocumentRepository documentRepository =
            new DocumentRepository();

    private DocumentRepository.DocumentData currentDocumentData;

    // =========================================================
    // Document Type
    // =========================================================

    private enum DocumentType {

        PRE_INVOICE(
                "پیش‌فاکتور",
                "sle.SLEPreFactItm"
        ),

        INVOICE(
                "فاکتور فروش",
                "sle.SLEFactItm"
        );

        private final String title;
        private final String tableName;

        DocumentType(
                String title,
                String tableName
        ) {
            this.title = title;
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    // =========================================================
    // Search Controls
    // =========================================================

    private final JComboBox<CompanyConfig> cmbCompany =
            new JComboBox<>();

    private final JComboBox<DocumentType> cmbDocumentType =
            new JComboBox<>(DocumentType.values());

    private final JTextField txtYear =
            new JTextField();

    private final JTextField txtVchNo =
            new JTextField();

    private final JButton btnSearch =
            new JButton("جستجو");

    private final JButton btnDocumentList =
            new JButton("لیست اسناد");

    private final JButton btnSave =
            new JButton("ثبت قیمت دوم");

    private final JButton btnPrint =
            new JButton("چاپ سند");

    // =========================================================
    // Document Info
    // =========================================================

    private final JLabel lblCustomerValue =
            new JLabel("-");

    private final JLabel lblCustomerCodeValue =
            new JLabel("-");

    private final JLabel lblDateValue =
            new JLabel("-");

    private final JLabel lblDocumentNoValue =
            new JLabel("-");

    private final JLabel lblDocumentTypeValue =
            new JLabel("-");

    // =========================================================
    // Totals
    // =========================================================

    private final JLabel lblOldTotal =
            new JLabel("-");

    private final JLabel lblNewTotal =
            new JLabel("-");

    private final JLabel lblDifference =
            new JLabel("-");

    // =========================================================
    // Status
    // =========================================================

    private final JLabel lblStatus =
            new JLabel("آماده");

    // =========================================================
    // Formats
    // =========================================================

    private final DecimalFormat numberFormat =
            new DecimalFormat("#,##0.###");

    // =========================================================
    // Table Model
    //
    // Model columns:
    // 0 = VchItmId (hidden)
    // 1 = Seq
    // 2 = PartCode
    // 3 = PartName (Persian)
    // 4 = LatinName
    // 5 = Qty
    // 6 = Current Price / 1000
    // 7 = Second Price / 1000  <-- editable
    // 8 = Second Amount
    // =========================================================

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "VchItmId",
                            "ردیف",
                            "کد کالا",
                            "نام کالا",
                            "نام لاتین کالا",
                            "تعداد",
                            "قیمت سیستم / 1000",
                            "قیمت دوم / 1000",
                            "مبلغ دوم"
                    },
                    0
            ) {

                @Override
                public boolean isCellEditable(
                        int row,
                        int column
                ) {
                    return column == 7;
                }

                @Override
                public Class<?> getColumnClass(
                        int columnIndex
                ) {
                    return switch (columnIndex) {
                        case 0 -> Long.class;
                        case 1 -> Integer.class;
                        case 5, 6, 7, 8 -> Double.class;
                        default -> String.class;
                    };
                }
            };

    private final JTable tblItems =
            new JTable(tableModel);

    // =========================================================
    // Constructor
    // =========================================================

    public ProformaPriceFrame() {

        initializeFrame();

        setContentPane(
                createMainPanel()
        );

        configureTable();
        configureEvents();
        loadCompanies();

        getContentPane()
                .applyComponentOrientation(
                        ComponentOrientation.RIGHT_TO_LEFT
                );

        SwingUtilities.invokeLater(
                () -> txtYear.requestFocusInWindow()
        );
    }

    // =========================================================
    // Frame
    // =========================================================

    private void initializeFrame() {

        setTitle(
                "مدیریت قیمت اسناد صادراتی"
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setSize(
                1180,
                760
        );

        setMinimumSize(
                new Dimension(
                        1050,
                        680
                )
        );

        setLocationRelativeTo(null);
    }

    // =========================================================
    // Main Panel
    // =========================================================

    private JPanel createMainPanel() {

        JPanel root =
                new JPanel(
                        new BorderLayout(
                                0,
                                14
                        )
                );

        root.setBackground(APP_BG);

        root.setBorder(
                new EmptyBorder(
                        18,
                        22,
                        18,
                        22
                )
        );

        root.add(
                createHeader(),
                BorderLayout.NORTH
        );

        root.add(
                createContent(),
                BorderLayout.CENTER
        );

        root.add(
                createFooter(),
                BorderLayout.SOUTH
        );

        return root;
    }

    // =========================================================
    // Header
    // =========================================================

    private JPanel createHeader() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                20,
                                0
                        )
                );

        panel.setOpaque(true);
        panel.setBackground(HEADER_BG);

        panel.setBorder(
                new EmptyBorder(
                        18,
                        22,
                        18,
                        22
                )
        );

        panel.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 20"
        );

        JPanel titlePanel =
                new JPanel();

        titlePanel.setOpaque(false);

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel lblTitle =
                new JLabel(
                        "مدیریت قیمت اسناد صادراتی"
                );

        lblTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        25
                )
        );

        lblTitle.setForeground(Color.WHITE);

        lblTitle.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        JLabel lblSubtitle =
                new JLabel(
                        "ثبت و کنترل قیمت دوم پیش‌فاکتور و فاکتور فروش"
                );

        lblSubtitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        lblSubtitle.setForeground(
                new Color(
                        203,
                        213,
                        225
                )
        );

        lblSubtitle.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(lblSubtitle);

        JLabel badge =
                new JLabel(
                        "EXPORT PRICE",
                        SwingConstants.CENTER
                );

        badge.setOpaque(true);
        badge.setBackground(
                new Color(
                        30,
                        41,
                        59
                )
        );

        badge.setForeground(
                new Color(
                        147,
                        197,
                        253
                )
        );

        badge.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        badge.setBorder(
                new EmptyBorder(
                        8,
                        14,
                        8,
                        14
                )
        );

        badge.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 999"
        );

        panel.add(
                titlePanel,
                BorderLayout.EAST
        );

        panel.add(
                badge,
                BorderLayout.WEST
        );

        return panel;
    }

    // =========================================================
    // Content
    // =========================================================

    private JPanel createContent() {

        JPanel panel =
                new JPanel();

        panel.setOpaque(false);

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JPanel searchCard =
                createSearchCard();

        // فرم جستجو دوباره یک ردیف است.
        searchCard.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        92
                )
        );

        JPanel infoCard =
                createDocumentInfoCard();

        infoCard.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        100
                )
        );

        JPanel totalsCard =
                createTotalsCard();

        totalsCard.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        105
                )
        );

        panel.add(searchCard);
        panel.add(Box.createVerticalStrut(12));

        panel.add(infoCard);
        panel.add(Box.createVerticalStrut(12));

        panel.add(createItemsCard());
        panel.add(Box.createVerticalStrut(12));

        panel.add(totalsCard);

        return panel;
    }

    // =========================================================
    // Search Card
    // =========================================================

    private JPanel createSearchCard() {

        JPanel card =
                createCard();

        card.setLayout(
                new GridBagLayout()
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        5,
                        7,
                        5,
                        7
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        // Company
        JLabel lblCompany =
                createFieldLabel("شرکت");

        cmbCompany.setPreferredSize(
                new Dimension(
                        185,
                        40
                )
        );

        cmbCompany.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        // Document Type
        JLabel lblDocumentType =
                createFieldLabel("نوع سند");

        cmbDocumentType.setPreferredSize(
                new Dimension(
                        135,
                        40
                )
        );

        cmbDocumentType.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        // Year
        JLabel lblYear =
                createFieldLabel("سال");

        txtYear.setPreferredSize(
                new Dimension(
                        85,
                        40
                )
        );

        txtYear.setHorizontalAlignment(
                JTextField.CENTER
        );

        txtYear.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "مثلاً 2020"
        );

        txtYear.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        // Document No
        JLabel lblVchNo =
                createFieldLabel("شماره سند");

        txtVchNo.setPreferredSize(
                new Dimension(
                        110,
                        40
                )
        );

        txtVchNo.setHorizontalAlignment(
                JTextField.CENTER
        );

        txtVchNo.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "مثلاً 17"
        );

        txtVchNo.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        // Search
        btnSearch.setPreferredSize(
                new Dimension(
                        115,
                        40
                )
        );

        btnSearch.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        btnSearch.setBackground(PRIMARY);
        btnSearch.setForeground(Color.WHITE);

        btnSearch.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0"
        );

        btnDocumentList.setPreferredSize(
                new Dimension(
                        110,
                        40
                )
        );

        btnDocumentList.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        btnDocumentList.setForeground(
                PRIMARY
        );

        btnDocumentList.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0;
        card.add(lblCompany, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.30;
        card.add(cmbCompany, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        card.add(lblDocumentType, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.22;
        card.add(cmbDocumentType, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        card.add(lblYear, gbc);

        gbc.gridx = 5;
        gbc.weightx = 0.12;
        card.add(txtYear, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0;
        card.add(lblVchNo, gbc);

        gbc.gridx = 7;
        gbc.weightx = 0.16;
        card.add(txtVchNo, gbc);

        /*
         * هر دکمه در یک ستون مستقل GridBag قرار می‌گیرد.
         * قبلاً هر دو دکمه داخل FlowLayout یک Cell بودند و در عرض
         * فعلی فرم، Cell فقط به اندازه یک دکمه جا می‌گرفت و دکمه
         * جستجو Clip می‌شد.
         */

        btnDocumentList.setPreferredSize(
                new Dimension(
                        105,
                        40
                )
        );

        btnSearch.setPreferredSize(
                new Dimension(
                        82,
                        40
                )
        );

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        // لیست اسناد
        gbc.gridx = 8;
        gbc.insets =
                new Insets(
                        5,
                        4,
                        5,
                        4
                );

        card.add(
                btnDocumentList,
                gbc
        );

        // جستجوی مستقیم
        gbc.gridx = 9;

        card.add(
                btnSearch,
                gbc
        );

        return card;
    }

    private JLabel createFieldLabel(
            String text
    ) {

        JLabel label =
                new JLabel(text);

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        label.setForeground(TEXT_MAIN);

        return label;
    }

    // =========================================================
    // Document Info
    // =========================================================

    private JPanel createDocumentInfoCard() {

        JPanel card =
                createCard();

        card.setLayout(
                new GridLayout(
                        1,
                        5,
                        10,
                        0
                )
        );

        card.add(
                createInfoBox(
                        "نام مشتری",
                        lblCustomerValue
                )
        );

        card.add(
                createInfoBox(
                        "کد مشتری",
                        lblCustomerCodeValue
                )
        );

        card.add(
                createInfoBox(
                        "تاریخ سند",
                        lblDateValue
                )
        );

        card.add(
                createInfoBox(
                        "شماره سند",
                        lblDocumentNoValue
                )
        );

        card.add(
                createInfoBox(
                        "نوع سند",
                        lblDocumentTypeValue
                )
        );

        return card;
    }

    private JPanel createInfoBox(
            String title,
            JLabel valueLabel
    ) {

        JPanel panel =
                new JPanel();

        panel.setOpaque(true);

        panel.setBackground(
                new Color(
                        248,
                        250,
                        252
                )
        );

        panel.setBorder(
                new EmptyBorder(
                        10,
                        12,
                        10,
                        12
                )
        );

        panel.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12"
        );

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel lblTitle =
                new JLabel(title);

        lblTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        11
                )
        );

        lblTitle.setForeground(TEXT_MUTED);

        valueLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        14
                )
        );

        valueLabel.setForeground(TEXT_MAIN);

        valueLabel.setBorder(
                new EmptyBorder(
                        5,
                        0,
                        0,
                        0
                )
        );

        lblTitle.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        valueLabel.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        panel.add(lblTitle);
        panel.add(valueLabel);

        return panel;
    }

    // =========================================================
    // Items Card
    // =========================================================

    private JPanel createItemsCard() {

        JPanel card =
                createCard();

        card.setLayout(
                new BorderLayout(
                        0,
                        10
                )
        );

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setOpaque(false);

        JLabel lblTitle =
                new JLabel(
                        "اقلام سند"
                );

        lblTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        15
                )
        );

        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblHint =
                new JLabel(
                        "ستون زرد محل ورود قیمت دوم به ازای هر 1000 واحد است"
                );

        lblHint.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        11
                )
        );

        lblHint.setForeground(TEXT_MUTED);

        top.add(
                lblTitle,
                BorderLayout.EAST
        );

        top.add(
                lblHint,
                BorderLayout.WEST
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        tblItems
                );

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        scrollPane.setPreferredSize(
                new Dimension(
                        980,
                        270
                )
        );

        card.add(
                top,
                BorderLayout.NORTH
        );

        card.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // Totals
    // =========================================================

    private JPanel createTotalsCard() {

        JPanel card =
                createCard();

        card.setLayout(
                new GridLayout(
                        1,
                        3,
                        12,
                        0
                )
        );

        card.add(
                createAmountBox(
                        "مبلغ سیستم",
                        lblOldTotal,
                        new Color(239, 246, 255),
                        PRIMARY
                )
        );

        card.add(
                createAmountBox(
                        "مبلغ دوم",
                        lblNewTotal,
                        new Color(240, 253, 244),
                        SUCCESS
                )
        );

        card.add(
                createAmountBox(
                        "اختلاف",
                        lblDifference,
                        new Color(255, 247, 237),
                        WARNING
                )
        );

        return card;
    }

    private JPanel createAmountBox(
            String title,
            JLabel valueLabel,
            Color background,
            Color foreground
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setOpaque(true);
        panel.setBackground(background);

        panel.setBorder(
                new EmptyBorder(
                        11,
                        12,
                        11,
                        12
                )
        );

        panel.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 14"
        );

        JLabel lblTitle =
                new JLabel(
                        title,
                        SwingConstants.CENTER
                );

        lblTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        lblTitle.setForeground(TEXT_MUTED);

        valueLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        valueLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        21
                )
        );

        valueLabel.setForeground(foreground);

        valueLabel.setBorder(
                new EmptyBorder(
                        7,
                        0,
                        0,
                        0
                )
        );

        panel.add(
                lblTitle,
                BorderLayout.NORTH
        );

        panel.add(
                valueLabel,
                BorderLayout.CENTER
        );

        return panel;
    }

    // =========================================================
    // Footer
    // =========================================================

    private JPanel createFooter() {

        JPanel footer =
                new JPanel(
                        new BorderLayout()
                );

        footer.setOpaque(false);

        lblStatus.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        lblStatus.setForeground(TEXT_MUTED);

        btnSave.setPreferredSize(
                new Dimension(
                        180,
                        44
                )
        );

        btnSave.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        btnSave.setBackground(SUCCESS);
        btnSave.setForeground(Color.WHITE);

        btnSave.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0"
        );

        btnSave.setEnabled(false);

        btnPrint.setPreferredSize(
                new Dimension(
                        150,
                        44
                )
        );

        btnPrint.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        btnPrint.setBackground(PRIMARY);
        btnPrint.setForeground(Color.WHITE);

        btnPrint.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0"
        );

        btnPrint.setEnabled(false);

        JPanel footerActions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        footerActions.setOpaque(false);

        footerActions.add(
                btnPrint
        );

        footerActions.add(
                btnSave
        );

        footer.add(
                lblStatus,
                BorderLayout.EAST
        );

        footer.add(
                footerActions,
                BorderLayout.WEST
        );

        return footer;
    }

    // =========================================================
    // Card
    // =========================================================

    private JPanel createCard() {

        JPanel panel =
                new JPanel();

        panel.setOpaque(true);
        panel.setBackground(CARD_BG);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                14,
                                16,
                                14,
                                16
                        )
                )
        );

        panel.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 16"
        );

        return panel;
    }

    // =========================================================
    // Table
    // =========================================================

    private void configureTable() {

        tblItems.setRowHeight(42);

        tblItems.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblItems.setFillsViewportHeight(true);

        tblItems.setShowHorizontalLines(true);
        tblItems.setShowVerticalLines(false);

        tblItems.setGridColor(BORDER);

        tblItems.setSelectionBackground(
                new Color(
                        219,
                        234,
                        254
                )
        );

        tblItems.setSelectionForeground(TEXT_MAIN);

        tblItems.getTableHeader()
                .setReorderingAllowed(false);

        tblItems.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                0,
                                40
                        )
                );

        tblItems.getTableHeader()
                .setFont(
                        new Font(
                                "Segoe UI",
                                Font.BOLD,
                                12
                        )
                );

        tblItems.getTableHeader()
                .setBackground(
                        new Color(
                                226,
                                232,
                                240
                        )
                );

        tblItems.getTableHeader()
                .setForeground(TEXT_MAIN);

        tblItems.setComponentOrientation(
                ComponentOrientation.RIGHT_TO_LEFT
        );

        // Hide VchItmId
        tblItems.removeColumn(
                tblItems
                        .getColumnModel()
                        .getColumn(0)
        );

        /*
         * View columns after hiding ID:
         *
         * 0 = ردیف
         * 1 = کد کالا
         * 2 = نام کالا
         * 3 = نام لاتین کالا
         * 4 = تعداد
         * 5 = قیمت سیستم / 1000
         * 6 = قیمت دوم / 1000
         * 7 = مبلغ دوم
         */

        tblItems.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(50);

        tblItems.getColumnModel()
                .getColumn(0)
                .setMaxWidth(65);

        tblItems.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(120);

        tblItems.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(230);

        tblItems.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(230);

        tblItems.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(100);

        tblItems.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(125);

        tblItems.getColumnModel()
                .getColumn(6)
                .setPreferredWidth(125);

        tblItems.getColumnModel()
                .getColumn(7)
                .setPreferredWidth(135);

        DefaultTableCellRenderer center =
                new DefaultTableCellRenderer();

        center.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        DefaultTableCellRenderer right =
                new DefaultTableCellRenderer();

        right.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        DefaultTableCellRenderer numberRenderer =
                new DefaultTableCellRenderer() {

                    private final DecimalFormat format =
                            new DecimalFormat("#,##0.###");

                    @Override
                    protected void setValue(
                            Object value
                    ) {

                        if (value instanceof Number number) {

                            setText(
                                    format.format(
                                            number.doubleValue()
                                    )
                            );

                        } else {

                            setText(
                                    value == null
                                            ? ""
                                            : value.toString()
                            );
                        }
                    }
                };

        numberRenderer.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        DefaultTableCellRenderer editablePriceRenderer =
                new DefaultTableCellRenderer() {

                    private final DecimalFormat format =
                            new DecimalFormat("#,##0.###");

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column
                    ) {

                        Component component =
                                super.getTableCellRendererComponent(
                                        table,
                                        value,
                                        isSelected,
                                        hasFocus,
                                        row,
                                        column
                                );

                        setHorizontalAlignment(
                                SwingConstants.CENTER
                        );

                        setFont(
                                getFont()
                                        .deriveFont(
                                                Font.BOLD
                                        )
                        );

                        if (value instanceof Number number) {

                            setText(
                                    format.format(
                                            number.doubleValue()
                                    )
                            );
                        }

                        if (!isSelected) {
                            component.setBackground(EDIT_BG);
                            component.setForeground(
                                    new Color(
                                            133,
                                            77,
                                            14
                                    )
                            );
                        }

                        return component;
                    }
                };

        // Seq
        tblItems.getColumnModel()
                .getColumn(0)
                .setCellRenderer(center);

        // Part Code
        tblItems.getColumnModel()
                .getColumn(1)
                .setCellRenderer(center);

        // Persian Part Name
        tblItems.getColumnModel()
                .getColumn(2)
                .setCellRenderer(right);

        // Latin Part Name
        tblItems.getColumnModel()
                .getColumn(3)
                .setCellRenderer(right);

        // Qty
        tblItems.getColumnModel()
                .getColumn(4)
                .setCellRenderer(numberRenderer);

        // System Price
        tblItems.getColumnModel()
                .getColumn(5)
                .setCellRenderer(numberRenderer);

        // Second Price
        tblItems.getColumnModel()
                .getColumn(6)
                .setCellRenderer(editablePriceRenderer);

        // Second Amount
        tblItems.getColumnModel()
                .getColumn(7)
                .setCellRenderer(numberRenderer);
    }

    // =========================================================
    // Events
    // =========================================================

    private void configureEvents() {

        txtYear.addActionListener(
                e -> searchDocument()
        );

        txtVchNo.addActionListener(
                e -> searchDocument()
        );

        btnSearch.addActionListener(
                e -> searchDocument()
        );

        btnDocumentList.addActionListener(
                e -> openDocumentList()
        );

        btnPrint.addActionListener(
                e -> printDocument()
        );

        btnSave.addActionListener(
                e -> savePrices()
        );

        cmbCompany.addActionListener(
                e -> selectionChanged()
        );

        cmbDocumentType.addActionListener(
                e -> selectionChanged()
        );

        tableModel.addTableModelListener(
                e -> {

                    // Model column 7 = Second Price
                    if (e.getColumn() == 7) {

                        calculateTotals();
                    }
                }
        );
    }

    // =========================================================
    // Companies
    // =========================================================

    private void loadCompanies() {

        cmbCompany.removeAllItems();

        try {

            var companies =
                    CompanyConfigLoader.load();

            for (
                    CompanyConfig company
                    : companies
            ) {

                cmbCompany.addItem(company);
            }

            if (cmbCompany.getItemCount() > 0) {

                cmbCompany.setSelectedIndex(0);
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "خطا در خواندن تنظیمات شرکت‌ها:\n\n"
                            + ex.getMessage(),
                    "خطای تنظیمات",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =========================================================
    // Selection Changed
    // =========================================================

    private void selectionChanged() {

        clearDocument(false);

        CompanyConfig company =
                (CompanyConfig)
                        cmbCompany
                                .getSelectedItem();

        DocumentType documentType =
                getSelectedDocumentType();

        if (company != null
                && documentType != null) {

            lblStatus.setText(
                    "شرکت: "
                            + company.name()
                            + "   |   نوع سند: "
                            + documentType
            );
        }
    }

    // =========================================================
    // Document List
    // =========================================================

    private void openDocumentList() {

        CompanyConfig company =
                (CompanyConfig)
                        cmbCompany
                                .getSelectedItem();

        if (company == null) {

            showError(
                    "شرکت انتخاب نشده است."
            );

            return;
        }

        DocumentType documentType =
                getSelectedDocumentType();

        if (documentType == null) {

            showError(
                    "نوع سند انتخاب نشده است."
            );

            return;
        }

        DocumentListDialog.DocumentKind kind =
                documentType
                        == DocumentType.PRE_INVOICE
                        ? DocumentListDialog.DocumentKind.PRE_INVOICE
                        : DocumentListDialog.DocumentKind.INVOICE;

        DocumentRepository.DocumentSummary selected =
                DocumentListDialog.showDialog(
                        this,
                        documentRepository,
                        company,
                        kind,
                        txtYear
                                .getText()
                                .trim()
                );

        if (selected == null) {

            return;
        }

        txtYear.setText(
                String.valueOf(
                        selected.year()
                )
        );

        txtVchNo.setText(
                String.valueOf(
                        selected.vchNo()
                )
        );

        /*
         * همان منطق جستجوی تک‌سند استفاده می‌شود؛ بنابراین
         * برای فاکتور، Auto Sync قیمت دوم از پیش‌فاکتور نیز
         * دقیقاً مثل جستجوی مستقیم اجرا خواهد شد.
         */
        searchDocument();
    }

    // =========================================================
    // Search
    // =========================================================

    private void searchDocument() {

        if (tblItems.isEditing()) {

            if (!tblItems
                    .getCellEditor()
                    .stopCellEditing()) {

                return;
            }
        }

        // =====================================================
        // Document Number
        // =====================================================

        String vchNoText =
                txtVchNo
                        .getText()
                        .trim();

        if (vchNoText.isBlank()) {

            showWarning(
                    "شماره سند را وارد کنید."
            );

            txtVchNo.requestFocusInWindow();

            return;
        }

        final int vchNo;

        try {

            vchNo =
                    Integer.parseInt(
                            vchNoText
                    );

        } catch (NumberFormatException ex) {

            showError(
                    "شماره سند باید عدد باشد."
            );

            txtVchNo.requestFocusInWindow();

            return;
        }

        // =====================================================
        // Year
        // =====================================================

        String yearText =
                txtYear
                        .getText()
                        .trim();

        if (yearText.isBlank()) {

            showWarning(
                    "سال سند را وارد کنید."
            );

            txtYear.requestFocusInWindow();

            return;
        }

        final int year;

        try {

            year =
                    Integer.parseInt(
                            yearText
                    );

        } catch (NumberFormatException ex) {

            showError(
                    "سال سند باید عدد باشد."
            );

            txtYear.requestFocusInWindow();

            return;
        }

        // =====================================================
        // Company / Type
        // =====================================================

        CompanyConfig company =
                (CompanyConfig)
                        cmbCompany
                                .getSelectedItem();

        if (company == null) {

            showError(
                    "شرکت انتخاب نشده است."
            );

            return;
        }

        DocumentType documentType =
                getSelectedDocumentType();

        if (documentType == null) {

            showError(
                    "نوع سند انتخاب نشده است."
            );

            return;
        }

        // =====================================================
        // Reset Result
        // =====================================================

        clearDocument(false);

        setBusy(true);

        lblStatus.setText(
                "در حال خواندن "
                        + documentType
                        + " شماره "
                        + vchNo
                        + " سال "
                        + year
                        + " از "
                        + company.name()
                        + " ..."
        );

        // =====================================================
        // Background Load
        // =====================================================

        SwingWorker<
                DocumentRepository.DocumentData,
                Void
                > worker =
                new SwingWorker<>() {

                    @Override
                    protected DocumentRepository.DocumentData
                    doInBackground()
                            throws Exception {

                        if (documentType
                                == DocumentType.PRE_INVOICE) {

                            return documentRepository
                                    .loadPreInvoice(
                                            company,
                                            vchNo,
                                            year
                                    );
                        }

                        return documentRepository
                                .loadInvoice(
                                        company,
                                        vchNo,
                                        year
                                );
                    }

                    @Override
                    protected void done() {

                        try {

                            DocumentRepository.DocumentData data =
                                    get();

                            if (data == null) {

                                JOptionPane.showMessageDialog(
                                        ProformaPriceFrame.this,
                                        """
                                        سند مورد نظر پیدا نشد.

                                        شرکت: %s
                                        نوع سند: %s
                                        سال: %d
                                        شماره سند: %d
                                        """
                                                .formatted(
                                                        company.name(),
                                                        documentType,
                                                        year,
                                                        vchNo
                                                ),
                                        "سند پیدا نشد",
                                        JOptionPane.WARNING_MESSAGE
                                );

                                lblStatus.setText(
                                        "سند مورد نظر پیدا نشد."
                                );

                                return;
                            }

                            currentDocumentData =
                                    data;

                            // Header Info
                            lblDocumentNoValue.setText(
                                    String.valueOf(
                                            data.vchNo()
                                    )
                            );

                            lblDocumentTypeValue.setText(
                                    documentType.toString()
                            );

                            lblCustomerValue.setText(
                                    isBlank(
                                            data.customerTitle()
                                    )
                                            ? "-"
                                            : data.customerTitle()
                            );

                            lblCustomerCodeValue.setText(
                                    isBlank(
                                            data.customerCode()
                                    )
                                            ? "-"
                                            : data.customerCode()
                            );

                            lblDateValue.setText(
                                    formatDate(
                                            data.vchDate()
                                    )
                            );

                            // Items
                            tableModel.setRowCount(0);

                            for (
                                    DocumentRepository.DocumentItem item
                                    : data.items()
                            ) {

                                addItem(
                                        item.vchItmId(),
                                        item.seq(),
                                        item.partCode(),
                                        item.partName(),
                                        item.latinName(),
                                        item.qty(),
                                        item.currentPricePer1000(),
                                        item.secondPricePer1000()
                                );
                            }

                            calculateTotals();

                            btnSave.setEnabled(
                                    !data.items().isEmpty()
                            );

                            btnPrint.setEnabled(
                                    !data.items().isEmpty()
                            );

                            lblStatus.setText(
                                    "✓ "
                                            + company.name()
                                            + "  |  "
                                            + documentType
                                            + " "
                                            + data.vchNo()
                                            + "  |  سال "
                                            + data.year()
                                            + "  |  "
                                            + data.items().size()
                                            + " ردیف"
                            );

                        } catch (Exception ex) {

                            Throwable cause =
                                    unwrapException(ex);

                            cause.printStackTrace();

                            JOptionPane.showMessageDialog(
                                    ProformaPriceFrame.this,
                                    """
                                    خطا در خواندن اطلاعات سند.

                                    شرکت: %s
                                    نوع سند: %s
                                    سال: %d
                                    شماره سند: %d

                                    خطا:
                                    %s
                                    """
                                            .formatted(
                                                    company.name(),
                                                    documentType,
                                                    year,
                                                    vchNo,
                                                    safeMessage(cause)
                                            ),
                                    "خطای دیتابیس",
                                    JOptionPane.ERROR_MESSAGE
                            );

                            lblStatus.setText(
                                    "خطا در خواندن سند."
                            );

                        } finally {

                            setBusy(false);
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // Add Item
    // =========================================================

    private void addItem(
            long vchItmId,
            int seq,
            String partCode,
            String partName,
            String latinName,
            double qty,
            double currentPricePer1000,
            Double secondPricePer1000
    ) {

        Double secondAmount = null;

        if (secondPricePer1000 != null) {

            secondAmount =
                    qty
                            * secondPricePer1000
                            / 1000.0;
        }

        tableModel.addRow(
                new Object[]{
                        vchItmId,
                        seq,
                        partCode,
                        partName,
                        latinName,
                        qty,
                        currentPricePer1000,
                        secondPricePer1000,
                        secondAmount
                }
        );
    }

    // =========================================================
    // Calculate
    // =========================================================

    private void calculateTotals() {

        double oldTotal = 0.0;
        double newTotal = 0.0;

        boolean allRowsHaveSecondPrice =
                tableModel.getRowCount() > 0;

        for (
                int row = 0;
                row < tableModel.getRowCount();
                row++
        ) {

            // Model column 5 = Qty
            double qty =
                    getDouble(
                            tableModel.getValueAt(
                                    row,
                                    5
                            )
                    );

            // Model column 6 = Current Price / 1000
            double oldPricePer1000 =
                    getDouble(
                            tableModel.getValueAt(
                                    row,
                                    6
                            )
                    );

            double oldAmount =
                    qty
                            * oldPricePer1000
                            / 1000.0;

            oldTotal += oldAmount;

            // Model column 7 = Second Price / 1000
            Object secondPriceObject =
                    tableModel.getValueAt(
                            row,
                            7
                    );

            Double secondPrice =
                    tryGetDouble(
                            secondPriceObject
                    );

            if (secondPrice != null) {

                double secondAmount =
                        qty
                                * secondPrice
                                / 1000.0;

                // Model column 8 = Second Amount
                tableModel.setValueAt(
                        secondAmount,
                        row,
                        8
                );

                newTotal += secondAmount;

            } else {

                tableModel.setValueAt(
                        null,
                        row,
                        8
                );

                allRowsHaveSecondPrice = false;
            }
        }

        lblOldTotal.setText(
                formatMoney(
                        oldTotal
                )
        );

        if (allRowsHaveSecondPrice) {

            lblNewTotal.setText(
                    formatMoney(
                            newTotal
                    )
            );

            lblDifference.setText(
                    formatMoney(
                            newTotal
                                    - oldTotal
                    )
            );

        } else {

            lblNewTotal.setText("-");
            lblDifference.setText("-");
        }
    }

    // =========================================================
    // Print
    // =========================================================

    private void printDocument() {

        if (tblItems.isEditing()) {

            if (!tblItems
                    .getCellEditor()
                    .stopCellEditing()) {

                return;
            }
        }

        if (tableModel.getRowCount() == 0
                || currentDocumentData == null) {

            showWarning(
                    "ابتدا یک سند را جستجو کنید."
            );

            return;
        }

        DocumentType documentType =
                getSelectedDocumentType();

        CompanyConfig company =
                (CompanyConfig)
                        cmbCompany
                                .getSelectedItem();

        if (documentType == null
                || company == null) {

            showError(
                    "شرکت یا نوع سند انتخاب نشده است."
            );

            return;
        }

        List<ExportDocumentPrintDialog.SourceItem> printItems =
                new ArrayList<>();

        for (
                int row = 0;
                row < tableModel.getRowCount();
                row++
        ) {

            DocumentRepository.DocumentItem item =
                    currentDocumentData
                            .items()
                            .get(row);

            // قیمت دوم از خود جدول گرفته می‌شود تا اگر کاربر هنوز
            // ذخیره نکرده ولی مقدار را تغییر داده، همان مقدار چاپ شود.
            Double secondPricePer1000 =
                    tryGetDouble(
                            tableModel.getValueAt(
                                    row,
                                    7
                            )
                    );

            Double secondTotalPrice =
                    null;

            if (secondPricePer1000 != null
                    && secondPricePer1000 > 0) {

                secondTotalPrice =
                        item.qty()
                                * secondPricePer1000
                                / 1000.0;
            }

            printItems.add(
                    new ExportDocumentPrintDialog.SourceItem(
                            item.partRef(),
                            item.seq(),
                            item.partCode(),
                            item.partName(),
                            item.latinName(),
                            item.qty(),
                            item.shrRatio(),
                            item.qtyPallet(),

                            // قیمت سیستم
                            item.currentPricePer1000(),
                            item.curPrice(),

                            // قیمت دوم
                            secondPricePer1000,
                            secondTotalPrice
                    )
            );
        }

        ExportDocumentPrintService.DocumentKind kind =
                documentType
                        == DocumentType.PRE_INVOICE
                        ? ExportDocumentPrintService.DocumentKind.PROFORMA
                        : ExportDocumentPrintService.DocumentKind.INVOICE;

        ExportDocumentPrintDialog.showDialog(
                this,
                company,
                kind,
                lblDocumentNoValue.getText(),
                getGregorianDateForPrint(
                        lblDateValue.getText()
                ),
                lblCustomerCodeValue.getText(),
                lblCustomerValue.getText(),
                currentDocumentData.customerAddress(),
                printItems
        );
    }

    // =========================================================
    // Save
    // =========================================================

    private void savePrices() {

        if (tblItems.isEditing()) {

            if (!tblItems
                    .getCellEditor()
                    .stopCellEditing()) {

                return;
            }
        }

        if (tableModel.getRowCount() == 0) {

            showWarning(
                    "ابتدا یک سند را جستجو کنید."
            );

            return;
        }

        CompanyConfig company =
                (CompanyConfig)
                        cmbCompany
                                .getSelectedItem();

        DocumentType documentType =
                getSelectedDocumentType();

        if (company == null
                || documentType == null) {

            showError(
                    "شرکت یا نوع سند انتخاب نشده است."
            );

            return;
        }

        final int year;
        final int vchNo;

        try {

            year =
                    Integer.parseInt(
                            txtYear
                                    .getText()
                                    .trim()
                    );

            vchNo =
                    Integer.parseInt(
                            txtVchNo
                                    .getText()
                                    .trim()
                    );

        } catch (NumberFormatException ex) {

            showError(
                    "سال یا شماره سند معتبر نیست."
            );

            return;
        }

        List<DocumentRepository.PriceUpdate> updates =
                new ArrayList<>();

        for (
                int row = 0;
                row < tableModel.getRowCount();
                row++
        ) {

            long vchItmId =
                    ((Number)
                            tableModel.getValueAt(
                                    row,
                                    0
                            ))
                            .longValue();

            double qty =
                    getDouble(
                            tableModel.getValueAt(
                                    row,
                                    5
                            )
                    );

            Double displayPrice =
                    tryGetDouble(
                            tableModel.getValueAt(
                                    row,
                                    7
                            )
                    );

            if (displayPrice == null) {

                showWarning(
                        "قیمت دوم ردیف "
                                + (row + 1)
                                + " وارد نشده است."
                );

                tblItems.changeSelection(
                        row,
                        6,
                        false,
                        false
                );

                return;
            }

            if (displayPrice <= 0) {

                showError(
                        "قیمت دوم ردیف "
                                + (row + 1)
                                + " باید بزرگ‌تر از صفر باشد."
                );

                return;
            }

            double curUnitPrice2 =
                    displayPrice
                            / 1000.0;

            double curPrice2 =
                    qty
                            * curUnitPrice2;

            updates.add(
                    new DocumentRepository.PriceUpdate(
                            vchItmId,
                            curUnitPrice2,
                            curPrice2
                    )
            );
        }

        calculateTotals();

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        """
                        قیمت دوم ثبت شود؟

                        شرکت: %s
                        نوع سند: %s
                        سال: %d
                        شماره سند: %d
                        تعداد ردیف: %d

                        مبلغ سیستم: %s
                        مبلغ دوم: %s
                        اختلاف: %s
                        """
                                .formatted(
                                        company.name(),
                                        documentType,
                                        year,
                                        vchNo,
                                        updates.size(),
                                        lblOldTotal.getText(),
                                        lblNewTotal.getText(),
                                        lblDifference.getText()
                                ),
                        "تأیید ثبت قیمت",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (result != JOptionPane.YES_OPTION) {

            return;
        }

        setBusy(true);

        lblStatus.setText(
                "در حال ثبت قیمت دوم..."
        );

        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Void doInBackground()
                            throws Exception {

                        if (documentType
                                == DocumentType.PRE_INVOICE) {

                            documentRepository
                                    .savePreInvoicePrices(
                                            company,
                                            updates
                                    );

                        } else {

                            documentRepository
                                    .saveInvoicePrices(
                                            company,
                                            updates
                                    );
                        }

                        return null;
                    }

                    @Override
                    protected void done() {

                        boolean success =
                                false;

                        try {

                            get();

                            success = true;

                            JOptionPane.showMessageDialog(
                                    ProformaPriceFrame.this,
                                    """
                                    قیمت دوم با موفقیت ثبت شد.

                                    شرکت: %s
                                    نوع سند: %s
                                    سال: %d
                                    شماره سند: %d
                                    """
                                            .formatted(
                                                    company.name(),
                                                    documentType,
                                                    year,
                                                    vchNo
                                            ),
                                    "ثبت موفق",
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                            lblStatus.setText(
                                    "✓ اطلاعات با موفقیت ثبت شد."
                            );

                        } catch (Exception ex) {

                            Throwable cause =
                                    unwrapException(ex);

                            cause.printStackTrace();

                            JOptionPane.showMessageDialog(
                                    ProformaPriceFrame.this,
                                    "خطا در ثبت اطلاعات:\n\n"
                                            + safeMessage(cause),
                                    "خطای دیتابیس",
                                    JOptionPane.ERROR_MESSAGE
                            );

                            lblStatus.setText(
                                    "ثبت اطلاعات ناموفق بود."
                            );

                        } finally {

                            setBusy(false);

                            if (success) {

                                SwingUtilities.invokeLater(
                                        ProformaPriceFrame.this
                                                ::searchDocument
                                );
                            }
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // Clear
    // =========================================================

    private void clearDocument(
            boolean clearSearchFields
    ) {

        currentDocumentData = null;

        tableModel.setRowCount(0);

        if (clearSearchFields) {

            txtYear.setText("");
            txtVchNo.setText("");
        }

        lblDocumentNoValue.setText("-");
        lblDocumentTypeValue.setText("-");
        lblCustomerValue.setText("-");
        lblCustomerCodeValue.setText("-");
        lblDateValue.setText("-");

        lblOldTotal.setText("-");
        lblNewTotal.setText("-");
        lblDifference.setText("-");

        btnSave.setEnabled(false);
        btnPrint.setEnabled(false);
    }

    // =========================================================
    // Busy
    // =========================================================

    private void setBusy(
            boolean busy
    ) {

        btnSearch.setEnabled(!busy);
        btnDocumentList.setEnabled(!busy);

        btnSave.setEnabled(
                !busy
                        && tableModel.getRowCount() > 0
        );

        btnPrint.setEnabled(
                !busy
                        && tableModel.getRowCount() > 0
        );

        cmbCompany.setEnabled(!busy);
        cmbDocumentType.setEnabled(!busy);
        txtYear.setEnabled(!busy);
        txtVchNo.setEnabled(!busy);

        setCursor(
                busy
                        ? Cursor.getPredefinedCursor(
                                Cursor.WAIT_CURSOR
                        )
                        : Cursor.getDefaultCursor()
        );
    }

    // =========================================================
    // Document Type
    // =========================================================

    private DocumentType getSelectedDocumentType() {

        return (DocumentType)
                cmbDocumentType
                        .getSelectedItem();
    }

    // =========================================================
    // Helpers
    // =========================================================

    private double getDouble(
            Object value
    ) {

        Double result =
                tryGetDouble(value);

        return result == null
                ? 0.0
                : result;
    }

    private Double tryGetDouble(
            Object value
    ) {

        if (value == null) {

            return null;
        }

        if (value instanceof Number number) {

            return number.doubleValue();
        }

        String text =
                value
                        .toString()
                        .trim()
                        .replace(",", "");

        if (text.isEmpty()) {

            return null;
        }

        try {

            return Double.valueOf(text);

        } catch (NumberFormatException ex) {

            return null;
        }
    }

    private String formatMoney(
            double value
    ) {

        return "$ "
                + numberFormat.format(
                        value
                );
    }

    private String formatDate(
                Object value
        ) {

            if (value == null) {
                return "-";
            }

            String text =
                    value
                            .toString()
                            .trim();

            if (text.isEmpty()) {
                return "-";
            }

            return text;
        }

    /**
     * تاریخ مورد استفاده در چاپ.
     *
     * مقدار نمایشی فرم می‌تواند به صورت زیر باشد:
     * 2026-06-09 (1405/03/19)
     *
     * ولی روی Proforma / Invoice فقط تاریخ میلادی چاپ می‌شود:
     * 2026-06-09
     */
    private String getGregorianDateForPrint(
            String value
    ) {

        if (value == null) {
            return "-";
        }

        String text =
                value.trim();

        if (text.isEmpty()
                || "-".equals(text)) {

            return "-";
        }

        // حالت استاندارد فعلی برنامه: yyyy-MM-dd (...)
        if (text.length() >= 10
                && text.charAt(4) == '-'
                && text.charAt(7) == '-') {

            return text.substring(
                    0,
                    10
            );
        }

        // Fallback:
        // اگر فرمت ورودی کمی تغییر کرد، هر چیزی قبل از پرانتز شمسی را نگه می‌داریم.
        int parenthesisIndex =
                text.indexOf('(');

        if (parenthesisIndex > 0) {

            String gregorian =
                    text.substring(
                            0,
                            parenthesisIndex
                    ).trim();

            if (!gregorian.isEmpty()) {
                return gregorian;
            }
        }

        return text;
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private Throwable unwrapException(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        while (
                current.getCause() != null
                        && current.getCause() != current
        ) {

            current =
                    current.getCause();
        }

        return current;
    }

    private String safeMessage(
            Throwable throwable
    ) {

        if (throwable == null) {

            return "خطای نامشخص";
        }

        String message =
                throwable.getMessage();

        if (message == null
                || message.isBlank()) {

            return throwable
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    private void showWarning(
            String message
    ) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "توجه",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(
            String message
    ) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "خطا",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
