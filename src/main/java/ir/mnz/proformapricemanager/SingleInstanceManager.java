package ir.mnz.proformapricemanager;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SingleInstanceManager {

    private static final String LOCK_FILE_NAME =
            "ProformaPriceManager.lock";

    private static FileChannel lockChannel;
    private static FileLock fileLock;

    private SingleInstanceManager() {
    }

    /**
     * تلاش برای گرفتن Lock اختصاصی برنامه.
     *
     * @return true اگر این اولین Instance برنامه باشد.
     */
    public static synchronized boolean acquire() {

        if (fileLock != null
                && fileLock.isValid()) {

            return true;
        }

        Path lockFile =
                Path.of(
                        System.getProperty(
                                "java.io.tmpdir"
                        ),
                        LOCK_FILE_NAME
                );

        try {

            lockChannel =
                    FileChannel.open(
                            lockFile,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE
                    );

            try {

                fileLock =
                        lockChannel.tryLock();

            } catch (OverlappingFileLockException ex) {

                fileLock = null;
            }

            if (fileLock == null) {

                closeChannelQuietly();

                return false;
            }

            return true;

        } catch (IOException ex) {

            closeChannelQuietly();

            /*
             * اگر به هر دلیل Lock قابل ایجاد نباشد، بهتر است
             * برنامه اجرا نشود تا احتمال اجرای همزمان چند نسخه
             * به وجود نیاید.
             */
            return false;
        }
    }

    /**
     * آزاد کردن Lock برنامه.
     *
     * این متد idempotent است؛ چند بار صدا زده شود مشکلی ندارد.
     */
    public static synchronized void release() {

        if (fileLock != null) {

            try {

                if (fileLock.isValid()) {
                    fileLock.release();
                }

            } catch (IOException ignored) {
            }

            fileLock = null;
        }

        closeChannelQuietly();
    }

    private static void closeChannelQuietly() {

        if (lockChannel != null) {

            try {

                if (lockChannel.isOpen()) {
                    lockChannel.close();
                }

            } catch (IOException ignored) {
            }

            lockChannel = null;
        }
    }
}
