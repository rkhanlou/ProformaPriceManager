package ir.mnz.proformapricemanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {

    private DatabaseManager() {
    }

    public static Connection getConnection(
            CompanyConfig company
    ) throws SQLException {

        if (company == null) {
            throw new IllegalArgumentException(
                    "اطلاعات شرکت مشخص نشده است."
            );
        }

        DriverManager.setLoginTimeout(5);

        return DriverManager.getConnection(
                company.jdbcUrl(),
                company.username(),
                company.password()
        );
    }
}
