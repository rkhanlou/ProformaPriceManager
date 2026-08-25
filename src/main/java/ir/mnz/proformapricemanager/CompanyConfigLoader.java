package ir.mnz.proformapricemanager;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class CompanyConfigLoader {

    private static final String CONFIG_DIR =
            "config";

    private static final String CONFIG_FILE =
            "database.properties";

    private CompanyConfigLoader() {
    }

    public static List<CompanyConfig> load()
            throws IOException {

        Path path =
                resolveConfigPath();

        Properties properties =
                new Properties();

        try (
                Reader reader =
                        Files.newBufferedReader(
                                path,
                                StandardCharsets.UTF_8
                        )
        ) {
            properties.load(reader);
        }

        List<CompanyConfig> companies =
                new ArrayList<>();

        for (int i = 1; i <= 3; i++) {

            String prefix =
                    "company"
                            + i
                            + ".";

            String name =
                    getRequired(properties, prefix + "name");

            String server =
                    getRequired(properties, prefix + "server");

            int port =
                    Integer.parseInt(
                            properties.getProperty(
                                    prefix + "port",
                                    "1433"
                            ).trim()
                    );

            String database =
                    getRequired(properties, prefix + "database");

            String username =
                    getRequired(properties, prefix + "username");

            String password =
                    getRequired(properties, prefix + "password");

            companies.add(
                    new CompanyConfig(
                            name,
                            server,
                            port,
                            database,
                            username,
                            password
                    )
            );
        }

        return companies;
    }

    private static Path resolveConfigPath()
            throws IOException {

        // NetBeans / Maven / اجرای JAR از ریشه پروژه
        Path developmentPath =
                Path.of(
                        System.getProperty("user.dir"),
                        CONFIG_DIR,
                        CONFIG_FILE
                )
                .toAbsolutePath()
                .normalize();

        if (Files.isRegularFile(developmentPath)) {
            return developmentPath;
        }

        // نسخه jpackage: config کنار EXE و runtime
        Path javaHome =
                Path.of(System.getProperty("java.home"))
                        .toAbsolutePath()
                        .normalize();

        Path appRoot =
                javaHome.getParent();

        if (appRoot != null) {

            Path packagedPath =
                    appRoot
                            .resolve(CONFIG_DIR)
                            .resolve(CONFIG_FILE)
                            .normalize();

            if (Files.isRegularFile(packagedPath)) {
                return packagedPath;
            }
        }

        throw new IOException(
                "فایل database.properties پیدا نشد.\n\n"
                        + "مسیر بررسی‌شده در حالت توسعه:\n"
                        + developmentPath
                        + "\n\njava.home:\n"
                        + javaHome
                        + "\n\nپوشه config باید کنار پروژه/JAR یا کنار EXE باشد."
        );
    }

    private static String getRequired(
            Properties properties,
            String key
    ) {

        String value =
                properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "تنظیم " + key
                            + " در database.properties وجود ندارد."
            );
        }

        return value.trim();
    }
}
