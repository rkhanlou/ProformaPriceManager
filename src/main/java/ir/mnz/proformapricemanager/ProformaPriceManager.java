package ir.mnz.proformapricemanager;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class ProformaPriceManager {

    public static void main(String[] args) {

        // سازگاری با SQL Server قدیمی شرکت
        // توجه: در Java 23 ممکن است TLSv1 در java.security نیز نیاز به فعال‌سازی داشته باشد.
        System.setProperty(
                "jdk.tls.client.protocols",
                "TLSv1"
        );

        // ---------------------------------------------------------
        // فقط یک Instance از برنامه اجازه اجرا دارد
        // ---------------------------------------------------------
        if (!SingleInstanceManager.acquire()) {

            JOptionPane.showMessageDialog(
                    null,
                    "برنامه مدیریت قیمت اسناد صادراتی هم‌اکنون در حال اجراست.",
                    "برنامه در حال اجراست",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // اگر برنامه به هر دلیلی بسته شد، Lock حتماً آزاد شود.
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        SingleInstanceManager::release,
                        "proforma-price-manager-shutdown"
                )
        );

        // ---------------------------------------------------------
        // ظاهر برنامه
        // ---------------------------------------------------------
        FlatLightLaf.setup();

        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);

        // ---------------------------------------------------------
        // اجرای فرم اصلی
        // ---------------------------------------------------------
        SwingUtilities.invokeLater(() -> {

            ProformaPriceFrame frame =
                    new ProformaPriceFrame();

            /*
             * بستن برنامه را خودمان مدیریت می‌کنیم تا:
             * 1) تمام Windowهای Swing بسته شوند.
             * 2) Lock برنامه آزاد شود.
             * 3) Process جاوای همین برنامه کامل خاتمه پیدا کند.
             */
            frame.setDefaultCloseOperation(
                    WindowConstants.DO_NOTHING_ON_CLOSE
            );

            frame.addWindowListener(
                    new WindowAdapter() {

                        @Override
                        public void windowClosing(
                                WindowEvent e
                        ) {

                            shutdownApplication();
                        }
                    }
            );

            frame.setVisible(true);
        });
    }

    /**
     * خروج کامل و کنترل‌شده از برنامه.
     *
     * به هیچ java.exe دیگری دست نمی‌زند؛ فقط Process مربوط
     * به همین برنامه با System.exit خاتمه پیدا می‌کند.
     */
    private static void shutdownApplication() {

        try {

            for (Window window : Window.getWindows()) {

                if (window != null) {
                    window.dispose();
                }
            }

        } finally {

            SingleInstanceManager.release();

            // خاتمه کامل JVM مربوط به همین برنامه
            System.exit(0);
        }
    }
}
