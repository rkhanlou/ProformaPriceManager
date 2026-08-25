package ir.mnz.proformapricemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocumentRepository {

    // =========================================================
    // PRE INVOICE
    // SQL Server 2008 compatible
    // =========================================================

    private static final String SQL_PRE_INVOICE = """
        SELECT
            i.VchHdrRef,
            i.VchItmId,
            i.VchNo,
            i.Year,
            CONVERT(VARCHAR(10), i.VchDate, 120)
            + ' ('
            + ISNULL(CONVERT(VARCHAR(20), dbo.miladitoshamsi(i.VchDate)), '')
            + ')' AS VchDate,

            CONVERT(NVARCHAR(50), d.AccNum) AS CustomerCode,
            d.Title AS CustomerTitle,

            (
                SELECT TOP 1
                    a.Address
                FROM gnr.Crspnd c
                LEFT JOIN gnr.Address a
                       ON a.CrspndRef = c.Serial
                WHERE c.DLRef = d.AccNum
            ) AS CustomerAddress,

            i.Seq,
            i.PartRef,

            CONVERT(NVARCHAR(100), part.PartCode) AS PartCode,
            part.PartName AS PartName,
            part.LatinName AS LatinName,

            i.Qty,
            i.CurUnitPrice,
            i.CurPrice,

            i.CurUnitPrice2 AS CurUnitPrice2Resolved,

            CASE
                WHEN i.CurPrice2 IS NOT NULL
                    THEN i.CurPrice2

                WHEN i.CurUnitPrice2 IS NOT NULL
                    THEN i.Qty * i.CurUnitPrice2

                ELSE NULL
            END AS CurPrice2Resolved,

            CAST(NULL AS BIGINT) AS PreFactVchItmRef,

            i.Descr,
            up.Shratio AS ShrRatio,
            i.Qty / up.Shratio AS QtyPallet

        FROM sle.SLEPreFactItm i

        LEFT JOIN acc.DL d
               ON d.AccNum = i.CstmrRef

        LEFT JOIN inv.Part part
               ON part.Serial = i.PartRef

        LEFT JOIN inv.UntPrt up
               ON up.PartRef = part.Serial

        WHERE i.VchHdrRef = (

            SELECT TOP 1
                x.VchHdrRef

            FROM sle.SLEPreFactItm x

            WHERE x.VchNo = ?
              AND x.Year = ?
              AND x.CurType = 1

            ORDER BY
                x.VchHdrRef DESC
        )

          AND i.CurType = 1

        ORDER BY
            i.Seq,
            i.VchItmId
        """;

    // =========================================================
    // INVOICE
    // SQL Server 2008 compatible
    // =========================================================

    private static final String SQL_INVOICE = """
        SELECT
            f.VchHdrRef,
            f.VchItmId,
            f.VchNo,
            f.Year,
            CONVERT(VARCHAR(10), f.VchDate, 120)
            + ' ('
            + ISNULL(CONVERT(VARCHAR(20), dbo.miladitoshamsi(f.VchDate)), '')
            + ')' AS VchDate,

            CONVERT(NVARCHAR(50), d.AccNum) AS CustomerCode,
            d.Title AS CustomerTitle,

            (
                SELECT TOP 1
                    a.Address
                FROM gnr.Crspnd c
                LEFT JOIN gnr.Address a
                       ON a.CrspndRef = c.Serial
                WHERE c.DLRef = d.AccNum
            ) AS CustomerAddress,

            f.Seq,
            f.PartRef,

            CONVERT(NVARCHAR(100), part.PartCode) AS PartCode,
            part.PartName AS PartName,
            part.LatinName AS LatinName,

            f.Qty,
            f.CurUnitPrice,
            f.CurPrice,

            CASE
                WHEN f.CurUnitPrice2 IS NOT NULL
                    THEN f.CurUnitPrice2

                ELSE pre.CurUnitPrice2
            END AS CurUnitPrice2Resolved,

            CASE
                WHEN f.CurPrice2 IS NOT NULL
                    THEN f.CurPrice2

                WHEN f.CurUnitPrice2 IS NOT NULL
                    THEN f.Qty * f.CurUnitPrice2

                WHEN pre.CurUnitPrice2 IS NOT NULL
                    THEN f.Qty * pre.CurUnitPrice2

                ELSE NULL
            END AS CurPrice2Resolved,

            f.PreFactVchItmRef,

            f.Descr,
            up.Shratio AS ShrRatio,
            f.Qty / up.Shratio AS QtyPallet

        FROM sle.SLEFactItm f

        LEFT JOIN sle.SLEPreFactItm pre
               ON pre.VchItmId = f.PreFactVchItmRef

        LEFT JOIN inv.Part part
               ON part.Serial = f.PartRef

        LEFT JOIN inv.UntPrt up
               ON up.PartRef = part.Serial

        LEFT JOIN acc.DL d
               ON d.AccNum = f.CstmrRef

        WHERE f.VchHdrRef = (

            SELECT TOP 1
                x.VchHdrRef

            FROM sle.SLEFactItm x

            WHERE x.VchNo = ?
              AND x.Year = ?
              AND x.CurType = 1

            ORDER BY
                x.VchHdrRef DESC
        )

          AND f.CurType = 1

        ORDER BY
            f.Seq,
            f.VchItmId
        """;

    // =========================================================
    // UPDATE
    // =========================================================

    private static final String SQL_UPDATE_PRE_INVOICE = """
        UPDATE sle.SLEPreFactItm
        SET
            CurUnitPrice2 = ?,
            CurPrice2 = ?
        WHERE VchItmId = ?
        """;

    private static final String SQL_UPDATE_INVOICE = """
        UPDATE sle.SLEFactItm
        SET
            CurUnitPrice2 = ?,
            CurPrice2 = ?
        WHERE VchItmId = ?
        """;

    /*
     * هنگام باز کردن فاکتور:
     * اگر قیمت دوم روی خود فاکتور هنوز خالی باشد،
     * ولی ردیف فاکتور به پیش‌فاکتور متصل باشد و
     * پیش‌فاکتور قیمت دوم داشته باشد،
     * فقط قیمت واحد دوم را از پیش‌فاکتور می‌گیریم
     * و مبلغ دوم را بر اساس Qty خود فاکتور محاسبه می‌کنیم.
     *
     * نکته مهم:
     * اگر CurUnitPrice2 روی فاکتور قبلاً مقدار داشته باشد،
     * هیچ مقداری overwrite نمی‌شود.
     */
    private static final String SQL_SYNC_INVOICE_SECOND_PRICE_FROM_PRE_INVOICE = """
        UPDATE f
        SET
            f.CurUnitPrice2 = pre.CurUnitPrice2,
            f.CurPrice2 = f.Qty * pre.CurUnitPrice2

        FROM sle.SLEFactItm f

        INNER JOIN sle.SLEPreFactItm pre
                ON pre.VchItmId = f.PreFactVchItmRef

        WHERE f.VchHdrRef = (

            SELECT TOP 1
                x.VchHdrRef

            FROM sle.SLEFactItm x

            WHERE x.VchNo = ?
              AND x.Year = ?
              AND x.CurType = 1

            ORDER BY
                x.VchHdrRef DESC
        )

          AND f.CurType = 1
          AND f.CurUnitPrice2 IS NULL
          AND pre.CurUnitPrice2 IS NOT NULL
        """;

    private static final String SQL_UPDATE_PART_LATIN_NAME = """
        UPDATE inv.Part
        SET LatinName = ?
        WHERE Serial = ?
        """;

    // =========================================================
    // Load
    // =========================================================

    public DocumentData loadPreInvoice(
            CompanyConfig company,
            int vchNo,
            int year
    ) throws SQLException {

        return loadDocument(
                company,
                vchNo,
                year,
                SQL_PRE_INVOICE
        );
    }

    public DocumentData loadInvoice(
            CompanyConfig company,
            int vchNo,
            int year
    ) throws SQLException {

        /*
         * قبل از خواندن فاکتور، قیمت دوم ردیف‌هایی که هنوز
         * روی فاکتور مقدار ندارند از پیش‌فاکتور مبنا منتقل می‌شود.
         */
        syncInvoiceSecondPricesFromPreInvoice(
                company,
                vchNo,
                year
        );

        return loadDocument(
                company,
                vchNo,
                year,
                SQL_INVOICE
        );
    }

    /**
     * قیمت دوم را از پیش‌فاکتور به فاکتور منتقل می‌کند.
     *
     * فقط ردیف‌هایی به‌روزرسانی می‌شوند که:
     * 1) CurUnitPrice2 فاکتور NULL باشد.
     * 2) PreFactVchItmRef معتبر داشته باشند.
     * 3) CurUnitPrice2 پیش‌فاکتور مقدار داشته باشد.
     *
     * CurPrice2 از روی Qty خود فاکتور محاسبه می‌شود.
     */
    private int syncInvoiceSecondPricesFromPreInvoice(
            CompanyConfig company,
            int vchNo,
            int year
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseManager.getConnection(company);

                PreparedStatement statement =
                        connection.prepareStatement(
                                SQL_SYNC_INVOICE_SECOND_PRICE_FROM_PRE_INVOICE
                        )
        ) {

            statement.setInt(
                    1,
                    vchNo
            );

            statement.setInt(
                    2,
                    year
            );

            return statement.executeUpdate();
        }
    }

    private DocumentData loadDocument(
            CompanyConfig company,
            int vchNo,
            int year,
            String sql
    ) throws SQLException {

        List<DocumentItem> items =
                new ArrayList<>();

        long vchHdrRef = 0;
        int actualVchNo = 0;
        int actualYear = 0;

        Object vchDate = null;

        String customerCode = null;
        String customerTitle = null;
        String customerAddress = null;

        try (
                Connection connection =
                        DatabaseManager.getConnection(company);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    vchNo
            );

            statement.setInt(
                    2,
                    year
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                boolean first =
                        true;

                while (rs.next()) {

                    if (first) {

                        vchHdrRef =
                                rs.getLong(
                                        "VchHdrRef"
                                );

                        actualVchNo =
                                rs.getInt(
                                        "VchNo"
                                );

                        actualYear =
                                rs.getInt(
                                        "Year"
                                );

                        vchDate =
                                rs.getObject(
                                        "VchDate"
                                );

                        customerCode =
                                rs.getString(
                                        "CustomerCode"
                                );

                        customerTitle =
                                rs.getString(
                                        "CustomerTitle"
                                );

                        customerAddress =
                                rs.getString(
                                        "CustomerAddress"
                                );

                        if (customerAddress != null) {
                            customerAddress =
                                    customerAddress.trim();
                        }

                        first = false;
                    }

                    long vchItmId =
                            rs.getLong(
                                    "VchItmId"
                            );

                    int seq =
                            rs.getInt(
                                    "Seq"
                            );

                    long partRef =
                            rs.getLong(
                                    "PartRef"
                            );

                    String partCode =
                            rs.getString(
                                    "PartCode"
                            );

                    String partName =
                            rs.getString(
                                    "PartName"
                            );

                    String latinName =
                            rs.getString(
                                    "LatinName"
                            );

                    String itemDescr =
                            rs.getString(
                                    "Descr"
                            );

                    if (partCode == null
                            || partCode.isBlank()) {

                        partCode =
                                String.valueOf(
                                        partRef
                                );
                    }

                    if (partName == null
                            || partName.isBlank()) {

                        if (itemDescr != null
                                && !itemDescr.isBlank()) {

                            partName =
                                    itemDescr;

                        } else {

                            partName =
                                    "کالای "
                                            + partRef;
                        }
                    }

                    if (latinName != null) {
                        latinName = latinName.trim();
                    }

                    double qty =
                            rs.getDouble(
                                    "Qty"
                            );

                    double curUnitPrice =
                            rs.getDouble(
                                    "CurUnitPrice"
                            );

                    double curPrice =
                            rs.getDouble(
                                    "CurPrice"
                            );

                    Double curUnitPrice2 =
                            getNullableDouble(
                                    rs,
                                    "CurUnitPrice2Resolved"
                            );

                    Double curPrice2 =
                            getNullableDouble(
                                    rs,
                                    "CurPrice2Resolved"
                            );

                    Double shrRatio =
                            getNullableDouble(
                                    rs,
                                    "ShrRatio"
                            );

                    Double qtyPallet =
                            getNullableDouble(
                                    rs,
                                    "QtyPallet"
                            );

                    Long preFactVchItmRef =
                            getNullableLong(
                                    rs,
                                    "PreFactVchItmRef"
                            );

                    items.add(
                            new DocumentItem(
                                    vchItmId,
                                    seq,
                                    partRef,
                                    partCode,
                                    partName,
                                    latinName,
                                    itemDescr,
                                    qty,
                                    shrRatio,
                                    qtyPallet,
                                    curUnitPrice,
                                    curPrice,
                                    curUnitPrice2,
                                    curPrice2,
                                    preFactVchItmRef
                            )
                    );
                }
            }
        }

        if (items.isEmpty()) {

            return null;
        }

        return new DocumentData(
                vchHdrRef,
                actualVchNo,
                actualYear,
                vchDate,
                customerCode,
                customerTitle,
                customerAddress,
                items
        );
    }


    // =========================================================
    // Document List
    // =========================================================

    public List<DocumentSummary> listPreInvoices(
            CompanyConfig company,
            int year,
            Integer vchNo,
            String customerSearch,
            PriceStatusFilter priceStatus
    ) throws SQLException {

        return listDocuments(
                company,
                year,
                vchNo,
                customerSearch,
                priceStatus,
                true
        );
    }

    public List<DocumentSummary> listInvoices(
            CompanyConfig company,
            int year,
            Integer vchNo,
            String customerSearch,
            PriceStatusFilter priceStatus
    ) throws SQLException {

        return listDocuments(
                company,
                year,
                vchNo,
                customerSearch,
                priceStatus,
                false
        );
    }

    /**
     * حداکثر 200 سند آخر را برمی‌گرداند.
     *
     * برای فاکتور فروش، وضعیت قیمت دوم بر اساس مقدار مؤثر محاسبه
     * می‌شود؛ یعنی اگر قیمت روی خود فاکتور خالی باشد ولی از
     * پیش‌فاکتور قابل دریافت باشد، آن ردیف قیمت‌دار محسوب می‌شود.
     */
    private List<DocumentSummary> listDocuments(
            CompanyConfig company,
            int year,
            Integer vchNo,
            String customerSearch,
            PriceStatusFilter priceStatus,
            boolean preInvoice
    ) throws SQLException {

        String alias =
                preInvoice
                        ? "i"
                        : "f";

        String tableName =
                preInvoice
                        ? "sle.SLEPreFactItm"
                        : "sle.SLEFactItm";

        String effectiveSecondPrice =
                preInvoice
                        ? "i.CurUnitPrice2"
                        : "COALESCE(f.CurUnitPrice2, pre.CurUnitPrice2)";

        StringBuilder sql =
                new StringBuilder();

        sql.append("""
            SELECT TOP 200
                %1$s.VchHdrRef,
                %1$s.VchNo,
                %1$s.Year,

                CONVERT(VARCHAR(10), %1$s.VchDate, 120)
                + ' ('
                + ISNULL(
                    CONVERT(
                        VARCHAR(20),
                        dbo.miladitoshamsi(%1$s.VchDate)
                    ),
                    ''
                )
                + ')' AS VchDate,

                CONVERT(NVARCHAR(50), d.AccNum) AS CustomerCode,
                d.Title AS CustomerTitle,

                COUNT(*) AS ItemCount,

                SUM(
                    CASE
                        WHEN %2$s IS NOT NULL
                            THEN 1
                        ELSE 0
                    END
                ) AS PricedItemCount

            FROM %3$s %1$s
            """.formatted(
                        alias,
                        effectiveSecondPrice,
                        tableName
                )
        );

        if (!preInvoice) {

            sql.append("""

                LEFT JOIN sle.SLEPreFactItm pre
                       ON pre.VchItmId = f.PreFactVchItmRef
                """
            );
        }

        sql.append("""

            LEFT JOIN acc.DL d
                   ON d.AccNum = %1$s.CstmrRef

            WHERE %1$s.Year = ?
              AND %1$s.CurType = 1

              AND %1$s.VchHdrRef = (

                    SELECT TOP 1
                        x.VchHdrRef

                    FROM %2$s x

                    WHERE x.VchNo = %1$s.VchNo
                      AND x.Year = %1$s.Year
                      AND x.CurType = 1

                    ORDER BY
                        x.VchHdrRef DESC
              )
            """.formatted(
                        alias,
                        tableName
                )
        );

        if (vchNo != null) {

            sql.append(
                    "\n  AND "
                            + alias
                            + ".VchNo = ?\n"
            );
        }

        String normalizedCustomerSearch =
                customerSearch == null
                        ? ""
                        : customerSearch.trim();

        if (!normalizedCustomerSearch.isEmpty()) {

            sql.append("""

                  AND (
                        CONVERT(NVARCHAR(50), d.AccNum) LIKE ?
                        OR ISNULL(d.Title, N'') LIKE ?
                  )
                """
            );
        }

        sql.append("""

            GROUP BY
                %1$s.VchHdrRef,
                %1$s.VchNo,
                %1$s.Year,
                %1$s.VchDate,
                d.AccNum,
                d.Title
            """.formatted(alias)
        );

        PriceStatusFilter normalizedStatus =
                priceStatus == null
                        ? PriceStatusFilter.ALL
                        : priceStatus;

        switch (normalizedStatus) {

            case NOT_PRICED ->
                    sql.append(
                            "\nHAVING SUM(CASE WHEN "
                                    + effectiveSecondPrice
                                    + " IS NOT NULL THEN 1 ELSE 0 END) = 0\n"
                    );

            case PARTIAL ->
                    sql.append(
                            "\nHAVING SUM(CASE WHEN "
                                    + effectiveSecondPrice
                                    + " IS NOT NULL THEN 1 ELSE 0 END) > 0"
                                    + "\n   AND SUM(CASE WHEN "
                                    + effectiveSecondPrice
                                    + " IS NOT NULL THEN 1 ELSE 0 END) < COUNT(*)\n"
                    );

            case COMPLETE ->
                    sql.append(
                            "\nHAVING SUM(CASE WHEN "
                                    + effectiveSecondPrice
                                    + " IS NOT NULL THEN 1 ELSE 0 END) = COUNT(*)\n"
                    );

            case ALL -> {
                // بدون HAVING
            }
        }

        sql.append(
                "\nORDER BY "
                        + alias
                        + ".VchHdrRef DESC"
        );

        List<DocumentSummary> result =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection(company);

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int paramIndex = 1;

            statement.setInt(
                    paramIndex++,
                    year
            );

            if (vchNo != null) {

                statement.setInt(
                        paramIndex++,
                        vchNo
                );
            }

            if (!normalizedCustomerSearch.isEmpty()) {

                String likeValue =
                        "%"
                                + normalizedCustomerSearch
                                + "%";

                statement.setString(
                        paramIndex++,
                        likeValue
                );

                statement.setString(
                        paramIndex++,
                        likeValue
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    int itemCount =
                            rs.getInt(
                                    "ItemCount"
                            );

                    int pricedItemCount =
                            rs.getInt(
                                    "PricedItemCount"
                            );

                    result.add(
                            new DocumentSummary(
                                    rs.getLong(
                                            "VchHdrRef"
                                    ),
                                    rs.getInt(
                                            "VchNo"
                                    ),
                                    rs.getInt(
                                            "Year"
                                    ),
                                    rs.getObject(
                                            "VchDate"
                                    ),
                                    rs.getString(
                                            "CustomerCode"
                                    ),
                                    rs.getString(
                                            "CustomerTitle"
                                    ),
                                    itemCount,
                                    pricedItemCount
                            )
                    );
                }
            }
        }

        return result;
    }


    // =========================================================
    // Management Report
    // =========================================================

    /**
     * گزارش مدیریتی اسناد صادراتی.
     *
     * گزارش از اقلام واقعی سند ساخته می‌شود و در Java به سطح سند
     * تجمیع می‌شود تا هم Sheet خلاصه و هم Sheet ریز اقلام قابل تولید باشد.
     *
     * این متد هیچ داده‌ای را Update نمی‌کند.
     */
    public ManagementReportData loadManagementReport(
            CompanyConfig company,
            boolean preInvoice,
            ManagementReportFilter filter
    ) throws SQLException {

        if (filter == null) {

            throw new IllegalArgumentException(
                    "فیلتر گزارش مشخص نشده است."
            );
        }

        String alias =
                preInvoice
                        ? "i"
                        : "f";

        String tableName =
                preInvoice
                        ? "sle.SLEPreFactItm"
                        : "sle.SLEFactItm";

        String secondUnitPrice =
                preInvoice
                        ? "i.CurUnitPrice2"
                        : "COALESCE(f.CurUnitPrice2, pre.CurUnitPrice2)";

        String secondAmount =
                preInvoice
                        ? """
                          CASE
                              WHEN i.CurPrice2 IS NOT NULL
                                  THEN i.CurPrice2
                              WHEN i.CurUnitPrice2 IS NOT NULL
                                  THEN i.Qty * i.CurUnitPrice2
                              ELSE NULL
                          END
                          """
                        : """
                          CASE
                              WHEN f.CurPrice2 IS NOT NULL
                                  THEN f.CurPrice2
                              WHEN f.CurUnitPrice2 IS NOT NULL
                                  THEN f.Qty * f.CurUnitPrice2
                              WHEN pre.CurUnitPrice2 IS NOT NULL
                                  THEN f.Qty * pre.CurUnitPrice2
                              ELSE NULL
                          END
                          """;

        StringBuilder sql =
                new StringBuilder();

        sql.append("""
            SELECT
                %1$s.VchHdrRef,
                %1$s.VchItmId,
                %1$s.VchNo,
                %1$s.Year,
                %1$s.Month,

                CONVERT(VARCHAR(10), %1$s.VchDate, 120)
                    AS GregorianDate,

                ISNULL(
                    CONVERT(
                        VARCHAR(20),
                        dbo.miladitoshamsi(%1$s.VchDate)
                    ),
                    ''
                ) AS ShamsiDate,

                CONVERT(NVARCHAR(50), d.AccNum)
                    AS CustomerCode,

                d.Title
                    AS CustomerTitle,

                %1$s.Seq,
                %1$s.PartRef,

                CONVERT(
                    NVARCHAR(100),
                    part.PartCode
                ) AS PartCode,

                part.PartName
                    AS PartName,

                part.LatinName
                    AS LatinName,

                %1$s.Qty,

                %1$s.CurUnitPrice * 1000.0
                    AS SystemPricePer1000,

                CASE
                    WHEN %1$s.CurPrice IS NOT NULL
                        THEN %1$s.CurPrice
                    ELSE %1$s.Qty * %1$s.CurUnitPrice
                END AS SystemAmount,

                %2$s * 1000.0
                    AS SecondPricePer1000,

                %3$s
                    AS SecondAmount

            FROM %4$s %1$s
            """.formatted(
                        alias,
                        secondUnitPrice,
                        secondAmount,
                        tableName
                )
        );

        if (!preInvoice) {

            sql.append("""

                LEFT JOIN sle.SLEPreFactItm pre
                       ON pre.VchItmId = f.PreFactVchItmRef
                """
            );
        }

        sql.append("""

            LEFT JOIN acc.DL d
                   ON d.AccNum = %1$s.CstmrRef

            LEFT JOIN inv.Part part
                   ON part.Serial = %1$s.PartRef

            WHERE %1$s.CurType = 1

              AND %1$s.VchHdrRef = (

                    SELECT TOP 1
                        x.VchHdrRef

                    FROM %2$s x

                    WHERE x.VchNo = %1$s.VchNo
                      AND x.Year = %1$s.Year
                      AND x.CurType = 1

                    ORDER BY
                        x.VchHdrRef DESC
              )
            """.formatted(
                        alias,
                        tableName
                )
        );

        // -----------------------------------------------------
        // Range Filter
        // -----------------------------------------------------

        switch (filter.rangeType()) {

            case DATE -> sql.append(
                    "\n  AND ISNULL("
                            + "CONVERT(VARCHAR(20), dbo.miladitoshamsi("
                            + alias
                            + ".VchDate)), '') >= ?"
                            + "\n  AND ISNULL("
                            + "CONVERT(VARCHAR(20), dbo.miladitoshamsi("
                            + alias
                            + ".VchDate)), '') <= ?\n"
            );

            case QUARTER -> sql.append(
                    "\n  AND ("
                            + alias
                            + ".Year * 4 + (("
                            + alias
                            + ".Month - 1) / 3)) BETWEEN ? AND ?\n"
            );

            case DOCUMENT_NO -> sql.append(
                    "\n  AND "
                            + alias
                            + ".Year = ?"
                            + "\n  AND "
                            + alias
                            + ".VchNo BETWEEN ? AND ?\n"
            );
        }

        String customerSearch =
                filter.customerSearch() == null
                        ? ""
                        : filter.customerSearch().trim();

        if (!customerSearch.isEmpty()) {

            sql.append("""

                  AND (
                        CONVERT(NVARCHAR(50), d.AccNum) LIKE ?
                        OR ISNULL(d.Title, N'') LIKE ?
                  )
                """
            );
        }

        sql.append(
                "\nORDER BY "
                        + alias
                        + ".VchDate, "
                        + alias
                        + ".VchNo, "
                        + alias
                        + ".Seq, "
                        + alias
                        + ".VchItmId"
        );

        List<ManagementReportItem> allItems =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection(company);

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int p = 1;

            switch (filter.rangeType()) {

                case DATE -> {

                    statement.setString(
                            p++,
                            filter.fromShamsiDate()
                    );

                    statement.setString(
                            p++,
                            filter.toShamsiDate()
                    );
                }

                case QUARTER -> {

                    int fromKey =
                            filter.fromYear() * 4
                                    + (filter.fromQuarter() - 1);

                    int toKey =
                            filter.toYear() * 4
                                    + (filter.toQuarter() - 1);

                    statement.setInt(
                            p++,
                            fromKey
                    );

                    statement.setInt(
                            p++,
                            toKey
                    );
                }

                case DOCUMENT_NO -> {

                    statement.setInt(
                            p++,
                            filter.documentYear()
                    );

                    statement.setInt(
                            p++,
                            filter.fromDocumentNo()
                    );

                    statement.setInt(
                            p++,
                            filter.toDocumentNo()
                    );
                }
            }

            if (!customerSearch.isEmpty()) {

                String like =
                        "%"
                                + customerSearch
                                + "%";

                statement.setString(
                        p++,
                        like
                );

                statement.setString(
                        p++,
                        like
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    Double secondPricePer1000 =
                            getNullableDouble(
                                    rs,
                                    "SecondPricePer1000"
                            );

                    Double secondAmountValue =
                            getNullableDouble(
                                    rs,
                                    "SecondAmount"
                            );

                    double systemAmountValue =
                            rs.getDouble(
                                    "SystemAmount"
                            );

                    allItems.add(
                            new ManagementReportItem(
                                    rs.getLong(
                                            "VchHdrRef"
                                    ),
                                    rs.getLong(
                                            "VchItmId"
                                    ),
                                    rs.getInt(
                                            "VchNo"
                                    ),
                                    rs.getInt(
                                            "Year"
                                    ),
                                    rs.getInt(
                                            "Month"
                                    ),
                                    rs.getString(
                                            "GregorianDate"
                                    ),
                                    rs.getString(
                                            "ShamsiDate"
                                    ),
                                    rs.getString(
                                            "CustomerCode"
                                    ),
                                    rs.getString(
                                            "CustomerTitle"
                                    ),
                                    rs.getInt(
                                            "Seq"
                                    ),
                                    rs.getLong(
                                            "PartRef"
                                    ),
                                    rs.getString(
                                            "PartCode"
                                    ),
                                    rs.getString(
                                            "PartName"
                                    ),
                                    rs.getString(
                                            "LatinName"
                                    ),
                                    rs.getDouble(
                                            "Qty"
                                    ),
                                    rs.getDouble(
                                            "SystemPricePer1000"
                                    ),
                                    systemAmountValue,
                                    secondPricePer1000,
                                    secondAmountValue
                            )
                    );
                }
            }
        }

        // -----------------------------------------------------
        // Remove duplicate SQL rows by the real item key
        // -----------------------------------------------------
        /*
         * تعداد اقلام باید تعداد واقعی ردیف‌های سند باشد، نه تعداد
         * ردیف‌هایی که به علت JOINها از SQL برمی‌گردند.
         *
         * کلید واقعی هر ردیف کالا VchItmId است؛ بنابراین قبل از
         * محاسبه تعداد اقلام، وضعیت قیمت دوم و جمع مبالغ، خروجی
         * SQL را بر اساس VchItmId یکتا می‌کنیم.
         */
        Map<Long, ManagementReportItem> uniqueItemsByVchItmId =
                new LinkedHashMap<>();

        for (
                ManagementReportItem item
                : allItems
        ) {

            uniqueItemsByVchItmId.putIfAbsent(
                    item.vchItmId(),
                    item
            );
        }

        // -----------------------------------------------------
        // Group unique items by document
        // -----------------------------------------------------

        Map<Long, List<ManagementReportItem>> grouped =
                new LinkedHashMap<>();

        for (
                ManagementReportItem item
                : uniqueItemsByVchItmId.values()
        ) {

            grouped.computeIfAbsent(
                    item.vchHdrRef(),
                    key -> new ArrayList<>()
            ).add(
                    item
            );
        }

        List<ManagementReportDocument> documents =
                new ArrayList<>();

        List<ManagementReportItem> filteredItems =
                new ArrayList<>();

        PriceStatusFilter requestedStatus =
                filter.priceStatus() == null
                        ? PriceStatusFilter.ALL
                        : filter.priceStatus();

        for (
                Map.Entry<
                        Long,
                        List<ManagementReportItem>
                        > entry
                : grouped.entrySet()
        ) {

            List<ManagementReportItem> items =
                    entry.getValue();

            if (items.isEmpty()) {
                continue;
            }

            ManagementReportItem first =
                    items.get(0);

            int pricedItemCount = 0;

            double systemTotal = 0.0;
            double secondTotal = 0.0;

            for (
                    ManagementReportItem item
                    : items
            ) {

                systemTotal +=
                        item.systemAmount();

                if (item.secondAmount() != null) {

                    pricedItemCount++;

                    secondTotal +=
                            item.secondAmount();
                }
            }

            PriceStatus status;

            if (pricedItemCount <= 0) {

                status =
                        PriceStatus.NOT_PRICED;

            } else if (pricedItemCount
                    >= items.size()) {

                status =
                        PriceStatus.COMPLETE;

            } else {

                status =
                        PriceStatus.PARTIAL;
            }

            if (!matchesPriceStatus(
                    requestedStatus,
                    status
            )) {

                continue;
            }

            double difference =
                    secondTotal
                            - systemTotal;

            documents.add(
                    new ManagementReportDocument(
                            first.vchHdrRef(),
                            first.vchNo(),
                            first.year(),
                            first.gregorianDate(),
                            first.shamsiDate(),
                            first.customerCode(),
                            first.customerTitle(),
                            items.size(),
                            pricedItemCount,
                            status,
                            systemTotal,
                            secondTotal,
                            difference
                    )
            );

            filteredItems.addAll(
                    items
            );
        }

        return new ManagementReportData(
                documents,
                filteredItems
        );
    }

    private boolean matchesPriceStatus(
            PriceStatusFilter filter,
            PriceStatus status
    ) {

        return switch (filter) {

            case ALL ->
                    true;

            case NOT_PRICED ->
                    status
                            == PriceStatus.NOT_PRICED;

            case PARTIAL ->
                    status
                            == PriceStatus.PARTIAL;

            case COMPLETE ->
                    status
                            == PriceStatus.COMPLETE;
        };
    }

    // =========================================================
    // Save
    // =========================================================

    public void savePreInvoicePrices(
            CompanyConfig company,
            List<PriceUpdate> updates
    ) throws SQLException {

        savePrices(
                company,
                updates,
                SQL_UPDATE_PRE_INVOICE
        );
    }

    public void saveInvoicePrices(
            CompanyConfig company,
            List<PriceUpdate> updates
    ) throws SQLException {

        savePrices(
                company,
                updates,
                SQL_UPDATE_INVOICE
        );
    }

    private void savePrices(
            CompanyConfig company,
            List<PriceUpdate> updates,
            String sql
    ) throws SQLException {

        if (updates == null
                || updates.isEmpty()) {

            return;
        }

        try (
                Connection connection =
                        DatabaseManager.getConnection(company)
        ) {

            boolean oldAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                for (
                        PriceUpdate item
                        : updates
                ) {

                    statement.setDouble(
                            1,
                            item.curUnitPrice2()
                    );

                    statement.setDouble(
                            2,
                            item.curPrice2()
                    );

                    statement.setLong(
                            3,
                            item.vchItmId()
                    );

                    statement.addBatch();
                }

                int[] results =
                        statement.executeBatch();

                for (
                        int result
                        : results
                ) {

                    if (result
                            == Statement.EXECUTE_FAILED) {

                        throw new SQLException(
                                "حداقل یکی از ردیف‌ها به‌روزرسانی نشد."
                        );
                    }
                }

                connection.commit();

            } catch (Exception ex) {

                connection.rollback();

                if (ex instanceof SQLException sqlException) {

                    throw sqlException;
                }

                throw new SQLException(
                        "خطا در ثبت قیمت دوم.",
                        ex
                );

            } finally {

                connection.setAutoCommit(
                        oldAutoCommit
                );
            }
        }
    }

    // =========================================================
    // Save Latin Names
    // =========================================================

    public void savePartLatinNames(
            CompanyConfig company,
            List<PartLatinNameUpdate> updates
    ) throws SQLException {

        if (updates == null
                || updates.isEmpty()) {

            return;
        }

        try (
                Connection connection =
                        DatabaseManager.getConnection(company)
        ) {

            boolean oldAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    SQL_UPDATE_PART_LATIN_NAME
                            )
            ) {

                for (
                        PartLatinNameUpdate item
                        : updates
                ) {

                    statement.setString(
                            1,
                            item.latinName()
                    );

                    statement.setLong(
                            2,
                            item.partRef()
                    );

                    statement.addBatch();
                }

                int[] results =
                        statement.executeBatch();

                for (
                        int result
                        : results
                ) {

                    if (result
                            == Statement.EXECUTE_FAILED) {

                        throw new SQLException(
                                "حداقل نام لاتین یکی از کالاها به‌روزرسانی نشد."
                        );
                    }
                }

                connection.commit();

            } catch (Exception ex) {

                connection.rollback();

                if (ex instanceof SQLException sqlException) {

                    throw sqlException;
                }

                throw new SQLException(
                        "خطا در ثبت نام لاتین کالا.",
                        ex
                );

            } finally {

                connection.setAutoCommit(
                        oldAutoCommit
                );
            }
        }
    }

    // =========================================================
    // SQL Helpers
    // =========================================================

    private Double getNullableDouble(
            ResultSet rs,
            String column
    ) throws SQLException {

        double value =
                rs.getDouble(
                        column
                );

        return rs.wasNull()
                ? null
                : value;
    }

    private Long getNullableLong(
            ResultSet rs,
            String column
    ) throws SQLException {

        long value =
                rs.getLong(
                        column
                );

        return rs.wasNull()
                ? null
                : value;
    }


    public enum PriceStatusFilter {

        ALL("همه"),
        NOT_PRICED("ثبت نشده"),
        PARTIAL("ناقص"),
        COMPLETE("ثبت شده");

        private final String title;

        PriceStatusFilter(
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

    public enum PriceStatus {

        NOT_PRICED("ثبت نشده"),
        PARTIAL("ناقص"),
        COMPLETE("ثبت شده");

        private final String title;

        PriceStatus(
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

    public record DocumentSummary(
            long vchHdrRef,
            int vchNo,
            int year,
            Object vchDate,
            String customerCode,
            String customerTitle,
            int itemCount,
            int pricedItemCount
    ) {

        public PriceStatus priceStatus() {

            if (itemCount <= 0
                    || pricedItemCount <= 0) {

                return PriceStatus.NOT_PRICED;
            }

            if (pricedItemCount >= itemCount) {

                return PriceStatus.COMPLETE;
            }

            return PriceStatus.PARTIAL;
        }
    }


    public enum ManagementReportRangeType {

        DATE("از تاریخ شمسی تا تاریخ شمسی"),
        QUARTER("فصل به فصل"),
        DOCUMENT_NO("از شماره سند تا شماره سند");

        private final String title;

        ManagementReportRangeType(
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

    public record ManagementReportFilter(
            ManagementReportRangeType rangeType,

            String fromShamsiDate,
            String toShamsiDate,

            Integer fromYear,
            Integer fromQuarter,
            Integer toYear,
            Integer toQuarter,

            Integer documentYear,
            Integer fromDocumentNo,
            Integer toDocumentNo,

            String customerSearch,
            PriceStatusFilter priceStatus
    ) {
    }

    public record ManagementReportDocument(
            long vchHdrRef,
            int vchNo,
            int year,
            String gregorianDate,
            String shamsiDate,
            String customerCode,
            String customerTitle,
            int itemCount,
            int pricedItemCount,
            PriceStatus priceStatus,
            double systemTotal,
            double secondTotal,
            double difference
    ) {
    }

    public record ManagementReportItem(
            long vchHdrRef,
            long vchItmId,
            int vchNo,
            int year,
            int month,
            String gregorianDate,
            String shamsiDate,
            String customerCode,
            String customerTitle,
            int seq,
            long partRef,
            String partCode,
            String partName,
            String latinName,
            double qty,
            double systemPricePer1000,
            double systemAmount,
            Double secondPricePer1000,
            Double secondAmount
    ) {

        public Double difference() {

            if (secondAmount == null) {
                return null;
            }

            return secondAmount
                    - systemAmount;
        }
    }

    public record ManagementReportData(
            List<ManagementReportDocument> documents,
            List<ManagementReportItem> items
    ) {

        public int documentCount() {

            return documents == null
                    ? 0
                    : documents.size();
        }

        public int completeCount() {

            if (documents == null) {
                return 0;
            }

            int count = 0;

            for (
                    ManagementReportDocument document
                    : documents
            ) {

                if (document.priceStatus()
                        == PriceStatus.COMPLETE) {

                    count++;
                }
            }

            return count;
        }

        public int partialCount() {

            if (documents == null) {
                return 0;
            }

            int count = 0;

            for (
                    ManagementReportDocument document
                    : documents
            ) {

                if (document.priceStatus()
                        == PriceStatus.PARTIAL) {

                    count++;
                }
            }

            return count;
        }

        public int notPricedCount() {

            if (documents == null) {
                return 0;
            }

            int count = 0;

            for (
                    ManagementReportDocument document
                    : documents
            ) {

                if (document.priceStatus()
                        == PriceStatus.NOT_PRICED) {

                    count++;
                }
            }

            return count;
        }

        public double systemTotal() {

            if (documents == null) {
                return 0.0;
            }

            double total = 0.0;

            for (
                    ManagementReportDocument document
                    : documents
            ) {

                total +=
                        document.systemTotal();
            }

            return total;
        }

        public double secondTotal() {

            if (documents == null) {
                return 0.0;
            }

            double total = 0.0;

            for (
                    ManagementReportDocument document
                    : documents
            ) {

                total +=
                        document.secondTotal();
            }

            return total;
        }

        public double differenceTotal() {

            return secondTotal()
                    - systemTotal();
        }
    }

    // =========================================================
    // Records
    // =========================================================

    public record DocumentData(
            long vchHdrRef,
            int vchNo,
            int year,
            Object vchDate,
            String customerCode,
            String customerTitle,
            String customerAddress,
            List<DocumentItem> items
    ) {
    }

    public record DocumentItem(
            long vchItmId,
            int seq,
            long partRef,
            String partCode,
            String partName,
            String latinName,
            String itemDescr,
            double qty,
            Double shrRatio,
            Double qtyPallet,
            double curUnitPrice,
            double curPrice,
            Double curUnitPrice2,
            Double curPrice2,
            Long preFactVchItmRef
    ) {

        public double currentPricePer1000() {

            return curUnitPrice
                    * 1000.0;
        }

        public Double secondPricePer1000() {

            if (curUnitPrice2 == null) {

                return null;
            }

            return curUnitPrice2
                    * 1000.0;
        }
    }

    public record PartLatinNameUpdate(
            long partRef,
            String latinName
    ) {
    }

    public record PriceUpdate(
            long vchItmId,
            double curUnitPrice2,
            double curPrice2
    ) {
    }
}
