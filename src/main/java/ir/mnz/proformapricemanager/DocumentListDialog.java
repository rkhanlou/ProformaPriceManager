package ir.mnz.proformapricemanager;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class DocumentListDialog extends JDialog {

    public enum DocumentKind {

        PRE_INVOICE("پیش‌فاکتورها", "پیش‌فاکتور", true),
        INVOICE("فاکتورهای فروش", "فاکتور فروش", false);

        private final String listTitle;
        private final String documentTitle;
        private final boolean preInvoice;

        DocumentKind(
                String listTitle,
                String documentTitle,
                boolean preInvoice
        ) {

            this.listTitle =
                    listTitle;

            this.documentTitle =
                    documentTitle;

            this.preInvoice =
                    preInvoice;
        }

        public boolean isPreInvoice() {

            return preInvoice;
        }

        public String documentTitle() {

            return documentTitle;
        }

        @Override
        public String toString() {

            return listTitle;
        }
    }

    private enum RangeMode {

        YEAR("کل سال"),
        SHAMSI_DATE("بازه تاریخ شمسی"),
        QUARTER("فصل به فصل"),
        DOCUMENT_NO("بازه شماره سند");

        private final String title;

        RangeMode(
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

    private static final Color APP_BG =
            new Color(241, 245, 249);

    private static final Color PRIMARY =
            new Color(37, 99, 235);

    private static final Color SUCCESS =
            new Color(22, 163, 74);

    private static final Color TEXT_MAIN =
            new Color(30, 41, 59);

    private static final Color TEXT_MUTED =
            new Color(100, 116, 139);

    private static final Color BORDER =
            new Color(226, 232, 240);

    private final DocumentRepository repository;
    private final CompanyConfig company;
    private final DocumentKind documentKind;

    private DocumentRepository.DocumentSummary selectedDocument;

    private DocumentRepository.ManagementReportData currentData;

    private final JComboBox<RangeMode> cmbRangeMode =
            new JComboBox<>(
                    RangeMode.values()
            );

    private final CardLayout rangeCardLayout =
            new CardLayout();

    private final JPanel pnlRangeCards =
            new JPanel(
                    rangeCardLayout
            );

    // کل سال
    private final JTextField txtYear =
            new JTextField();

    // تاریخ شمسی
    private final JTextField txtFromShamsiDate =
            new JTextField();

    private final JTextField txtToShamsiDate =
            new JTextField();

    // فصل
    private final JTextField txtFromQuarterYear =
            new JTextField();

    private final JComboBox<Integer> cmbFromQuarter =
            new JComboBox<>(
                    new Integer[]{
                            1,
                            2,
                            3,
                            4
                    }
            );

    private final JTextField txtToQuarterYear =
            new JTextField();

    private final JComboBox<Integer> cmbToQuarter =
            new JComboBox<>(
                    new Integer[]{
                            1,
                            2,
                            3,
                            4
                    }
            );

    // شماره سند
    private final JTextField txtDocumentYear =
            new JTextField();

    private final JTextField txtFromDocumentNo =
            new JTextField();

    private final JTextField txtToDocumentNo =
            new JTextField();

    // فیلترهای مشترک
    private final JTextField txtCustomer =
            new JTextField();

    private final JComboBox<DocumentRepository.PriceStatusFilter> cmbStatus =
            new JComboBox<>(
                    DocumentRepository.PriceStatusFilter.values()
            );

    private final JButton btnSearch =
            new JButton(
                    "جستجو"
            );

    private final JButton btnExcel =
            new JButton(
                    "خروجی Excel"
            );

    private final JButton btnSelect =
            new JButton(
                    "انتخاب سند"
            );

    private final JButton btnClose =
            new JButton(
                    "بستن"
            );

    private final JLabel lblSummary =
            new JLabel(
                    "0 سند"
            );

    private final DecimalFormat moneyFormat =
            new DecimalFormat(
                    "#,##0.00"
            );

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "VchHdrRef",
                            "شماره سند",
                            "سال",
                            "تاریخ شمسی",
                            "تاریخ میلادی",
                            "کد مشتری",
                            "نام مشتری",
                            "تعداد اقلام",
                            "وضعیت",
                            "مبلغ سیستم",
                            "مبلغ دوم",
                            "اختلاف"
                    },
                    0
            ) {

                @Override
                public boolean isCellEditable(
                        int row,
                        int column
                ) {

                    return false;
                }
            };

    private final JTable table =
            new JTable(
                    tableModel
            );

    private List<DocumentRepository.ManagementReportDocument> currentRows =
            List.of();

    private DocumentListDialog(
            Window owner,
            DocumentRepository repository,
            CompanyConfig company,
            DocumentKind documentKind,
            String initialYear
    ) {

        super(
                owner,
                "لیست "
                        + documentKind,
                ModalityType.APPLICATION_MODAL
        );

        this.repository =
                repository;

        this.company =
                company;

        this.documentKind =
                documentKind;

        initializeDefaults(
                initialYear
        );

        initializeUi();
        configureEvents();

        setSize(
                1180,
                700
        );

        setMinimumSize(
                new Dimension(
                        980,
                        600
                )
        );

        setLocationRelativeTo(
                owner
        );

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(
                        "ESCAPE"
                ),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        SwingUtilities.invokeLater(
                () -> {

                    if (!txtYear
                            .getText()
                            .isBlank()) {

                        searchDocuments();

                    } else {

                        txtYear.requestFocusInWindow();
                    }
                }
        );
    }

    public static DocumentRepository.DocumentSummary showDialog(
            Window owner,
            DocumentRepository repository,
            CompanyConfig company,
            DocumentKind documentKind,
            String initialYear
    ) {

        DocumentListDialog dialog =
                new DocumentListDialog(
                        owner,
                        repository,
                        company,
                        documentKind,
                        initialYear
                );

        dialog.setVisible(
                true
        );

        return dialog.selectedDocument;
    }

    private void initializeDefaults(
            String initialYear
    ) {

        String year =
                normalizeDigits(
                        initialYear == null
                                ? ""
                                : initialYear.trim()
                );

        txtYear.setText(
                year
        );

        txtFromQuarterYear.setText(
                year
        );

        txtToQuarterYear.setText(
                year
        );

        txtDocumentYear.setText(
                year
        );

        if (!year.isBlank()) {

            txtFromShamsiDate.setText(
                    year
                            + "/01/01"
            );

            txtToShamsiDate.setText(
                    year
                            + "/12/29"
            );
        }

        cmbRangeMode.setSelectedItem(
                RangeMode.YEAR
        );

        cmbStatus.setSelectedItem(
                DocumentRepository.PriceStatusFilter.ALL
        );

        btnExcel.setEnabled(
                false
        );
    }

    private void initializeUi() {

        JPanel root =
                new JPanel(
                        new BorderLayout(
                                0,
                                10
                        )
                );

        root.setBackground(
                APP_BG
        );

        root.setBorder(
                new EmptyBorder(
                        12,
                        14,
                        12,
                        14
                )
        );

        root.add(
                createFilterPanel(),
                BorderLayout.NORTH
        );

        root.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        root.add(
                createFooter(),
                BorderLayout.SOUTH
        );

        setContentPane(
                root
        );

        applyComponentOrientation(
                ComponentOrientation.RIGHT_TO_LEFT
        );
    }

    private JPanel createFilterPanel() {

        JPanel wrapper =
                new JPanel(
                        new BorderLayout(
                                0,
                                8
                        )
                );

        wrapper.setBackground(
                Color.WHITE
        );

        wrapper.setBorder(
                new EmptyBorder(
                        10,
                        12,
                        10,
                        12
                )
        );

        JPanel firstRow =
                new JPanel(
                        new GridBagLayout()
                );

        firstRow.setOpaque(
                false
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridy = 0;
        gbc.insets =
                new Insets(
                        4,
                        5,
                        4,
                        5
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        JLabel title =
                new JLabel(
                        documentKind
                                + " - "
                                + company.name()
                );

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        17
                )
        );

        title.setForeground(
                TEXT_MAIN
        );

        gbc.gridx = 0;
        gbc.weightx = 0.25;

        firstRow.add(
                title,
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 0;

        firstRow.add(
                label("نوع بازه"),
                gbc
        );

        gbc.gridx = 2;
        gbc.weightx = 0.18;

        firstRow.add(
                cmbRangeMode,
                gbc
        );

        gbc.gridx = 3;
        gbc.weightx = 0;

        firstRow.add(
                label("مشتری"),
                gbc
        );

        gbc.gridx = 4;
        gbc.weightx = 0.28;

        txtCustomer.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "کد یا نام مشتری"
        );

        firstRow.add(
                txtCustomer,
                gbc
        );

        gbc.gridx = 5;
        gbc.weightx = 0;

        firstRow.add(
                label("وضعیت قیمت دوم"),
                gbc
        );

        gbc.gridx = 6;
        gbc.weightx = 0.16;

        firstRow.add(
                cmbStatus,
                gbc
        );

        gbc.gridx = 7;
        gbc.weightx = 0;

        stylePrimaryButton(
                btnSearch
        );

        firstRow.add(
                btnSearch,
                gbc
        );

        // ---------------------------------------------
        // Range cards
        // ---------------------------------------------

        pnlRangeCards.setOpaque(
                false
        );

        pnlRangeCards.add(
                createYearPanel(),
                RangeMode.YEAR.name()
        );

        pnlRangeCards.add(
                createShamsiDatePanel(),
                RangeMode.SHAMSI_DATE.name()
        );

        pnlRangeCards.add(
                createQuarterPanel(),
                RangeMode.QUARTER.name()
        );

        pnlRangeCards.add(
                createDocumentNoPanel(),
                RangeMode.DOCUMENT_NO.name()
        );

        wrapper.add(
                firstRow,
                BorderLayout.NORTH
        );

        wrapper.add(
                pnlRangeCards,
                BorderLayout.CENTER
        );

        return wrapper;
    }

    private JPanel createYearPanel() {

        JPanel panel =
                rangePanel();

        addRangeField(
                panel,
                0,
                "سال",
                txtYear
        );

        JLabel hint =
                new JLabel(
                        "همه اسناد این سال نمایش داده می‌شوند."
                );

        hint.setForeground(
                TEXT_MUTED
        );

        GridBagConstraints gbc =
                rangeGbc();

        gbc.gridx = 2;
        gbc.weightx = 1;

        panel.add(
                hint,
                gbc
        );

        return panel;
    }

    private JPanel createShamsiDatePanel() {

        JPanel panel =
                rangePanel();

        addRangeField(
                panel,
                0,
                "از تاریخ شمسی",
                txtFromShamsiDate
        );

        addRangeField(
                panel,
                2,
                "تا تاریخ شمسی",
                txtToShamsiDate
        );

        JLabel hint =
                new JLabel(
                        "مثال: 1405/01/01"
                );

        hint.setForeground(
                TEXT_MUTED
        );

        GridBagConstraints gbc =
                rangeGbc();

        gbc.gridx = 4;
        gbc.weightx = 1;

        panel.add(
                hint,
                gbc
        );

        return panel;
    }

    private JPanel createQuarterPanel() {

        JPanel panel =
                rangePanel();

        addRangeField(
                panel,
                0,
                "سال شروع",
                txtFromQuarterYear
        );

        addRangeField(
                panel,
                2,
                "فصل شروع",
                cmbFromQuarter
        );

        addRangeField(
                panel,
                4,
                "سال پایان",
                txtToQuarterYear
        );

        addRangeField(
                panel,
                6,
                "فصل پایان",
                cmbToQuarter
        );

        return panel;
    }

    private JPanel createDocumentNoPanel() {

        JPanel panel =
                rangePanel();

        addRangeField(
                panel,
                0,
                "سال",
                txtDocumentYear
        );

        addRangeField(
                panel,
                2,
                "از شماره سند",
                txtFromDocumentNo
        );

        addRangeField(
                panel,
                4,
                "تا شماره سند",
                txtToDocumentNo
        );

        return panel;
    }

    private JPanel rangePanel() {

        JPanel panel =
                new JPanel(
                        new GridBagLayout()
                );

        panel.setOpaque(
                false
        );

        return panel;
    }

    private GridBagConstraints rangeGbc() {

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridy = 0;
        gbc.insets =
                new Insets(
                        4,
                        5,
                        4,
                        5
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        return gbc;
    }

    private void addRangeField(
            JPanel panel,
            int x,
            String title,
            JComponent component
    ) {

        GridBagConstraints gbc =
                rangeGbc();

        gbc.gridx = x;
        gbc.weightx = 0;

        panel.add(
                label(title),
                gbc
        );

        gbc.gridx =
                x + 1;

        gbc.weightx = 0.18;

        if (component instanceof JTextField field) {

            field.setPreferredSize(
                    new Dimension(
                            135,
                            34
                    )
            );

            field.setHorizontalAlignment(
                    JTextField.CENTER
            );
        }

        panel.add(
                component,
                gbc
        );
    }

    private JLabel label(
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

        label.setForeground(
                TEXT_MAIN
        );

        return label;
    }

    private JPanel createTablePanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                0,
                                8
                        )
                );

        panel.setBackground(
                Color.WHITE
        );

        panel.setBorder(
                new EmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        lblSummary.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        lblSummary.setForeground(
                TEXT_MUTED
        );

        table.setRowHeight(
                31
        );

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        table.setAutoCreateRowSorter(
                true
        );

        table.getTableHeader()
                .setReorderingAllowed(
                        false
                );

        // VchHdrRef مخفی
        table.removeColumn(
                table.getColumnModel()
                        .getColumn(0)
        );

        DefaultTableCellRenderer center =
                new DefaultTableCellRenderer();

        center.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        // Visible:
        // 0 No, 1 Year, 2 Shamsi, 3 Gregorian,
        // 4 customer code, 5 name, 6 items, 7 status,
        // 8 sys, 9 second, 10 diff
        for (
                int col : new int[]{
                        0,
                        1,
                        2,
                        3,
                        4,
                        6,
                        8,
                        9,
                        10
                }
        ) {

            table.getColumnModel()
                    .getColumn(col)
                    .setCellRenderer(
                            center
                    );
        }

        table.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(
                        240
                );

        table.getColumnModel()
                .getColumn(7)
                .setCellRenderer(
                        new StatusRenderer()
                );

        JScrollPane scrollPane =
                new JScrollPane(
                        table
                );

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        panel.add(
                lblSummary,
                BorderLayout.NORTH
        );

        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createFooter() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setOpaque(
                false
        );

        JPanel actions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        actions.setOpaque(
                false
        );

        styleSuccessButton(
                btnExcel
        );

        stylePrimaryButton(
                btnSelect
        );

        actions.add(
                btnClose
        );

        actions.add(
                btnExcel
        );

        actions.add(
                btnSelect
        );

        JLabel hint =
                new JLabel(
                        "برای باز کردن سند، روی ردیف دوبار کلیک کنید."
                );

        hint.setForeground(
                TEXT_MUTED
        );

        panel.add(
                hint,
                BorderLayout.EAST
        );

        panel.add(
                actions,
                BorderLayout.WEST
        );

        return panel;
    }

    private void stylePrimaryButton(
            JButton button
    ) {

        button.setBackground(
                PRIMARY
        );

        button.setForeground(
                Color.WHITE
        );

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        button.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 10; borderWidth: 0"
        );
    }

    private void styleSuccessButton(
            JButton button
    ) {

        button.setBackground(
                SUCCESS
        );

        button.setForeground(
                Color.WHITE
        );

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        button.putClientProperty(
                FlatClientProperties.STYLE,
                "arc: 10; borderWidth: 0"
        );
    }

    private void configureEvents() {

        cmbRangeMode.addActionListener(
                e -> updateRangeCard()
        );

        btnSearch.addActionListener(
                e -> searchDocuments()
        );

        btnExcel.addActionListener(
                e -> exportExcel()
        );

        btnSelect.addActionListener(
                e -> selectCurrentRow()
        );

        btnClose.addActionListener(
                e -> dispose()
        );

        txtCustomer.addActionListener(
                e -> searchDocuments()
        );

        table.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            MouseEvent e
                    ) {

                        if (e.getClickCount() == 2
                                && SwingUtilities.isLeftMouseButton(e)) {

                            selectCurrentRow();
                        }
                    }
                }
        );

        table.getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        ).put(
                KeyStroke.getKeyStroke(
                        "ENTER"
                ),
                "selectDocument"
        );

        table.getActionMap().put(
                "selectDocument",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent e
                    ) {

                        selectCurrentRow();
                    }
                }
        );

        updateRangeCard();
    }

    private void updateRangeCard() {

        RangeMode mode =
                selectedRangeMode();

        rangeCardLayout.show(
                pnlRangeCards,
                mode.name()
        );
    }

    private RangeMode selectedRangeMode() {

        RangeMode mode =
                (RangeMode)
                        cmbRangeMode
                                .getSelectedItem();

        return mode == null
                ? RangeMode.YEAR
                : mode;
    }

    private void searchDocuments() {

        final DocumentRepository.ManagementReportFilter filter;

        try {

            filter =
                    buildFilter();

        } catch (IllegalArgumentException ex) {

            showWarning(
                    ex.getMessage()
            );

            return;
        }

        setBusy(
                true
        );

        SwingWorker<
                DocumentRepository.ManagementReportData,
                Void
                > worker =
                new SwingWorker<>() {

                    @Override
                    protected DocumentRepository.ManagementReportData
                    doInBackground()
                            throws Exception {

                        return repository
                                .loadManagementReport(
                                        company,
                                        documentKind.isPreInvoice(),
                                        filter
                                );
                    }

                    @Override
                    protected void done() {

                        try {

                            currentData =
                                    get();

                            currentRows =
                                    currentData == null
                                            ? List.of()
                                            : currentData.documents();

                            fillTable();

                            btnExcel.setEnabled(
                                    currentData != null
                                            && currentData.documentCount() > 0
                            );

                        } catch (Exception ex) {

                            currentData =
                                    null;

                            currentRows =
                                    List.of();

                            tableModel.setRowCount(
                                    0
                            );

                            btnExcel.setEnabled(
                                    false
                            );

                            Throwable cause =
                                    unwrapException(
                                            ex
                                    );

                            cause.printStackTrace();

                            JOptionPane.showMessageDialog(
                                    DocumentListDialog.this,
                                    "خطا در خواندن لیست اسناد:\n\n"
                                            + safeMessage(
                                                    cause
                                            ),
                                    "خطای دیتابیس",
                                    JOptionPane.ERROR_MESSAGE
                            );

                        } finally {

                            setBusy(
                                    false
                            );
                        }
                    }
                };

        worker.execute();
    }

    private DocumentRepository.ManagementReportFilter buildFilter() {

        RangeMode mode =
                selectedRangeMode();

        String customer =
                txtCustomer
                        .getText()
                        .trim();

        DocumentRepository.PriceStatusFilter status =
                (DocumentRepository.PriceStatusFilter)
                        cmbStatus
                                .getSelectedItem();

        if (status == null) {

            status =
                    DocumentRepository.PriceStatusFilter.ALL;
        }

        switch (mode) {

            case YEAR -> {

                int year =
                        parsePositiveInt(
                                txtYear,
                                "سال"
                        );

                /*
                 * برای «کل سال» از همان موتور بازه شماره سند استفاده
                 * می‌کنیم، ولی بازه را از 1 تا Integer.MAX_VALUE می‌گذاریم.
                 */
                return new DocumentRepository.ManagementReportFilter(
                        DocumentRepository.ManagementReportRangeType.DOCUMENT_NO,

                        null,
                        null,

                        null,
                        null,
                        null,
                        null,

                        year,
                        1,
                        Integer.MAX_VALUE,

                        customer,
                        status
                );
            }

            case SHAMSI_DATE -> {

                String from =
                        normalizeShamsiDate(
                                txtFromShamsiDate.getText(),
                                "تاریخ شروع"
                        );

                String to =
                        normalizeShamsiDate(
                                txtToShamsiDate.getText(),
                                "تاریخ پایان"
                        );

                if (from.compareTo(to) > 0) {

                    throw new IllegalArgumentException(
                            "تاریخ شروع نباید بعد از تاریخ پایان باشد."
                    );
                }

                return new DocumentRepository.ManagementReportFilter(
                        DocumentRepository.ManagementReportRangeType.DATE,

                        from,
                        to,

                        null,
                        null,
                        null,
                        null,

                        null,
                        null,
                        null,

                        customer,
                        status
                );
            }

            case QUARTER -> {

                int fromYear =
                        parsePositiveInt(
                                txtFromQuarterYear,
                                "سال شروع"
                        );

                int toYear =
                        parsePositiveInt(
                                txtToQuarterYear,
                                "سال پایان"
                        );

                int fromQuarter =
                        (Integer)
                                cmbFromQuarter
                                        .getSelectedItem();

                int toQuarter =
                        (Integer)
                                cmbToQuarter
                                        .getSelectedItem();

                int fromKey =
                        fromYear * 4
                                + (fromQuarter - 1);

                int toKey =
                        toYear * 4
                                + (toQuarter - 1);

                if (fromKey > toKey) {

                    throw new IllegalArgumentException(
                            "فصل شروع نباید بعد از فصل پایان باشد."
                    );
                }

                return new DocumentRepository.ManagementReportFilter(
                        DocumentRepository.ManagementReportRangeType.QUARTER,

                        null,
                        null,

                        fromYear,
                        fromQuarter,
                        toYear,
                        toQuarter,

                        null,
                        null,
                        null,

                        customer,
                        status
                );
            }

            case DOCUMENT_NO -> {

                int year =
                        parsePositiveInt(
                                txtDocumentYear,
                                "سال"
                        );

                int fromNo =
                        parsePositiveInt(
                                txtFromDocumentNo,
                                "شماره سند شروع"
                        );

                int toNo =
                        parsePositiveInt(
                                txtToDocumentNo,
                                "شماره سند پایان"
                        );

                if (fromNo > toNo) {

                    throw new IllegalArgumentException(
                            "شماره سند شروع نباید بزرگ‌تر از شماره سند پایان باشد."
                    );
                }

                return new DocumentRepository.ManagementReportFilter(
                        DocumentRepository.ManagementReportRangeType.DOCUMENT_NO,

                        null,
                        null,

                        null,
                        null,
                        null,
                        null,

                        year,
                        fromNo,
                        toNo,

                        customer,
                        status
                );
            }
        }

        throw new IllegalArgumentException(
                "نوع بازه نامعتبر است."
        );
    }

    private void fillTable() {

        tableModel.setRowCount(
                0
        );

        for (
                DocumentRepository.ManagementReportDocument document
                : currentRows
        ) {

            tableModel.addRow(
                    new Object[]{
                            document.vchHdrRef(),
                            document.vchNo(),
                            document.year(),
                            blankToDash(
                                    document.shamsiDate()
                            ),
                            blankToDash(
                                    document.gregorianDate()
                            ),
                            blankToDash(
                                    document.customerCode()
                            ),
                            blankToDash(
                                    document.customerTitle()
                            ),
                            document.itemCount(),
                            document.priceStatus(),
                            moneyFormat.format(
                                    document.systemTotal()
                            ),
                            moneyFormat.format(
                                    document.secondTotal()
                            ),
                            moneyFormat.format(
                                    document.difference()
                            )
                    }
            );
        }

        if (currentData == null) {

            lblSummary.setText(
                    "0 سند"
            );

            return;
        }

        lblSummary.setText(
                "تعداد اسناد: "
                        + currentData.documentCount()
                        + "   |   ثبت شده: "
                        + currentData.completeCount()
                        + "   |   ناقص: "
                        + currentData.partialCount()
                        + "   |   ثبت نشده: "
                        + currentData.notPricedCount()
                        + "   |   مبلغ سیستم: "
                        + moneyFormat.format(
                                currentData.systemTotal()
                        )
                        + "   |   مبلغ دوم: "
                        + moneyFormat.format(
                                currentData.secondTotal()
                        )
                        + "   |   اختلاف: "
                        + moneyFormat.format(
                                currentData.differenceTotal()
                        )
        );

        if (!currentRows.isEmpty()) {

            table.setRowSelectionInterval(
                    0,
                    0
            );
        }
    }

    private void exportExcel() {

        if (currentData == null
                || currentData.documentCount() <= 0) {

            showWarning(
                    "ابتدا لیست اسناد را جستجو کنید."
            );

            return;
        }

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "ذخیره خروجی Excel"
        );

        chooser.setSelectedFile(
                new File(
                        buildDefaultExcelFileName()
                )
        );

        int result =
                chooser.showSaveDialog(
                        this
                );

        if (result
                != JFileChooser.APPROVE_OPTION) {

            return;
        }

        File target =
                chooser.getSelectedFile();

        if (!target.getName()
                .toLowerCase()
                .endsWith(".xlsx")) {

            target =
                    new File(
                            target.getParentFile(),
                            target.getName()
                                    + ".xlsx"
                    );
        }

        if (target.exists()) {

            int overwrite =
                    JOptionPane.showConfirmDialog(
                            this,
                            "فایل وجود دارد. جایگزین شود؟",
                            "تایید جایگزینی",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (overwrite
                    != JOptionPane.YES_OPTION) {

                return;
            }
        }

        try {

            ManagementReportExcelExporter.export(
                    target.toPath(),
                    new ManagementReportExcelExporter.ExportInfo(
                            company.name(),
                            documentKind.documentTitle(),
                            buildRangeTitle()
                    ),
                    currentData
            );

            int open =
                    JOptionPane.showConfirmDialog(
                            this,
                            "فایل Excel با موفقیت ساخته شد.\n\n"
                                    + target.getAbsolutePath()
                                    + "\n\nفایل باز شود؟",
                            "خروجی Excel",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE
                    );

            if (open == JOptionPane.YES_OPTION
                    && Desktop.isDesktopSupported()) {

                Desktop.getDesktop()
                        .open(
                                target
                        );
            }

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "خطا در ساخت فایل Excel:\n\n"
                            + safeMessage(
                                    ex
                            ),
                    "خطای خروجی",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String buildDefaultExcelFileName() {

        return (documentKind.isPreInvoice()
                ? "PreInvoice_"
                : "Invoice_")
                + "Report_"
                + System.currentTimeMillis()
                + ".xlsx";
    }

    private String buildRangeTitle() {

        return switch (selectedRangeMode()) {

            case YEAR ->
                    "سال "
                            + normalizeDigits(
                                    txtYear.getText()
                            );

            case SHAMSI_DATE ->
                    normalizeDigits(
                            txtFromShamsiDate.getText()
                    )
                            + " تا "
                            + normalizeDigits(
                                    txtToShamsiDate.getText()
                            );

            case QUARTER ->
                    "فصل "
                            + cmbFromQuarter.getSelectedItem()
                            + " سال "
                            + normalizeDigits(
                                    txtFromQuarterYear.getText()
                            )
                            + " تا فصل "
                            + cmbToQuarter.getSelectedItem()
                            + " سال "
                            + normalizeDigits(
                                    txtToQuarterYear.getText()
                            );

            case DOCUMENT_NO ->
                    "شماره "
                            + normalizeDigits(
                                    txtFromDocumentNo.getText()
                            )
                            + " تا "
                            + normalizeDigits(
                                    txtToDocumentNo.getText()
                            )
                            + " - سال "
                            + normalizeDigits(
                                    txtDocumentYear.getText()
                            );
        };
    }

    private void selectCurrentRow() {

        int viewRow =
                table.getSelectedRow();

        if (viewRow < 0) {

            showWarning(
                    "یک سند را انتخاب کنید."
            );

            return;
        }

        int modelRow =
                table.convertRowIndexToModel(
                        viewRow
                );

        long vchHdrRef =
                ((Number)
                        tableModel.getValueAt(
                                modelRow,
                                0
                        ))
                        .longValue();

        for (
                DocumentRepository.ManagementReportDocument document
                : currentRows
        ) {

            if (document.vchHdrRef()
                    == vchHdrRef) {

                selectedDocument =
                        new DocumentRepository.DocumentSummary(
                                document.vchHdrRef(),
                                document.vchNo(),
                                document.year(),
                                document.gregorianDate()
                                        + " ("
                                        + document.shamsiDate()
                                        + ")",
                                document.customerCode(),
                                document.customerTitle(),
                                document.itemCount(),
                                document.pricedItemCount()
                        );

                dispose();

                return;
            }
        }
    }

    private int parsePositiveInt(
            JTextField field,
            String title
    ) {

        String text =
                normalizeDigits(
                        field.getText()
                )
                        .trim();

        if (text.isEmpty()) {

            throw new IllegalArgumentException(
                    title
                            + " را وارد کنید."
            );
        }

        try {

            int value =
                    Integer.parseInt(
                            text
                    );

            if (value <= 0) {

                throw new NumberFormatException();
            }

            return value;

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    title
                            + " باید عدد معتبر باشد."
            );
        }
    }

    private String normalizeShamsiDate(
            String input,
            String title
    ) {

        String text =
                normalizeDigits(
                        input
                )
                        .trim()
                        .replace(
                                '-',
                                '/'
                        )
                        .replace(
                                '.',
                                '/'
                        );

        String[] parts =
                text.split(
                        "/"
                );

        if (parts.length != 3) {

            throw new IllegalArgumentException(
                    title
                            + " باید به شکل 1405/01/01 باشد."
            );
        }

        final int year;
        final int month;
        final int day;

        try {

            year =
                    Integer.parseInt(
                            parts[0]
                    );

            month =
                    Integer.parseInt(
                            parts[1]
                    );

            day =
                    Integer.parseInt(
                            parts[2]
                    );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    title
                            + " معتبر نیست."
            );
        }

        if (year < 1200
                || year > 1600) {

            throw new IllegalArgumentException(
                    "سال شمسی "
                            + title
                            + " معتبر نیست."
            );
        }

        if (month < 1
                || month > 12) {

            throw new IllegalArgumentException(
                    "ماه "
                            + title
                            + " معتبر نیست."
            );
        }

        int maxDay =
                month <= 6
                        ? 31
                        : 30;

        // اسفند 29 یا 30 روزه است؛ برای فیلتر اجازه 30 می‌دهیم.
        if (day < 1
                || day > maxDay) {

            throw new IllegalArgumentException(
                    "روز "
                            + title
                            + " معتبر نیست."
            );
        }

        return String.format(
                java.util.Locale.US,
                "%04d/%02d/%02d",
                year,
                month,
                day
        );
    }

    private static String normalizeDigits(
            String value
    ) {

        if (value == null) {
            return "";
        }

        StringBuilder out =
                new StringBuilder(
                        value.length()
                );

        for (
                int i = 0;
                i < value.length();
                i++
        ) {

            char ch =
                    value.charAt(i);

            if (ch >= '۰'
                    && ch <= '۹') {

                out.append(
                        (char)
                                ('0'
                                        + (ch - '۰'))
                );

            } else if (ch >= '٠'
                    && ch <= '٩') {

                out.append(
                        (char)
                                ('0'
                                        + (ch - '٠'))
                );

            } else {

                out.append(
                        ch
                );
            }
        }

        return out.toString();
    }

    private void setBusy(
            boolean busy
    ) {

        btnSearch.setEnabled(
                !busy
        );

        btnSelect.setEnabled(
                !busy
        );

        btnExcel.setEnabled(
                !busy
                        && currentData != null
                        && currentData.documentCount() > 0
        );

        cmbRangeMode.setEnabled(
                !busy
        );

        cmbStatus.setEnabled(
                !busy
        );

        txtCustomer.setEnabled(
                !busy
        );

        setCursor(
                busy
                        ? Cursor.getPredefinedCursor(
                                Cursor.WAIT_CURSOR
                        )
                        : Cursor.getDefaultCursor()
        );
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

    private String blankToDash(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }

        return value.trim();
    }

    private Throwable unwrapException(
            Throwable ex
    ) {

        Throwable current =
                ex;

        while (current.getCause() != null
                && current != current.getCause()) {

            current =
                    current.getCause();
        }

        return current;
    }

    private String safeMessage(
            Throwable ex
    ) {

        if (ex == null) {
            return "خطای نامشخص";
        }

        String message =
                ex.getMessage();

        if (message == null
                || message.isBlank()) {

            return ex.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private static class StatusRenderer
            extends DefaultTableCellRenderer {

        StatusRenderer() {

            setHorizontalAlignment(
                    SwingConstants.CENTER
            );
        }

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

            if (!isSelected
                    && value instanceof DocumentRepository.PriceStatus status) {

                switch (status) {

                    case COMPLETE -> {

                        component.setBackground(
                                new Color(
                                        220,
                                        252,
                                        231
                                )
                        );

                        component.setForeground(
                                new Color(
                                        22,
                                        101,
                                        52
                                )
                        );
                    }

                    case PARTIAL -> {

                        component.setBackground(
                                new Color(
                                        254,
                                        249,
                                        195
                                )
                        );

                        component.setForeground(
                                new Color(
                                        133,
                                        77,
                                        14
                                )
                        );
                    }

                    case NOT_PRICED -> {

                        component.setBackground(
                                new Color(
                                        254,
                                        226,
                                        226
                                )
                        );

                        component.setForeground(
                                new Color(
                                        153,
                                        27,
                                        27
                                )
                        );
                    }
                }

            } else if (!isSelected) {

                component.setBackground(
                        Color.WHITE
                );

                component.setForeground(
                        TEXT_MAIN
                );
            }

            return component;
        }
    }
}
