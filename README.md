# ProformaPriceManager / ExportPriceManager

نسخه بازسازی‌شده با آخرین تغییرات پروژه.

## فناوری
- Java 23
- Swing + FlatLaf
- SQL Server 2008 / Microsoft JDBC
- Apache POI برای خروجی Excel
- Maven Shade برای ساخت Fat JAR

## قابلیت‌های موجود در این نسخه
- جستجوی پیش‌فاکتور و فاکتور با شماره سند + سال
- ثبت `CurUnitPrice2` و `CurPrice2`
- انتقال خودکار قیمت دوم از پیش‌فاکتور به فاکتور در زمان باز کردن فاکتور
- نام فارسی و لاتین کالا و امکان اصلاح `inv.Part.LatinName` از فرم چاپ
- آدرس مشتری از `gnr.Crspnd` / `gnr.Address`
- تعداد/پالت از `inv.UntPrt`
- لیست اسناد با فیلترهای کل سال، بازه تاریخ شمسی، فصل، بازه شماره سند، مشتری و وضعیت قیمت دوم
- خروجی Excel از همان فرم لیست اسناد (Sheet خلاصه + ریز اقلام)
- چاپ با قیمت سیستم یا قیمت دوم
- Proforma: قیمت سیستم آبی، قیمت دوم صورتی
- Commercial Invoice: تم نارنجی
- Commercial Invoice: تجمیع اقلام تکراری بر اساس کالا + قیمت
- تاریخ چاپ فقط میلادی
- دو باکس امضا: Sales Manager و Managing Director
- Preview همیشه جلوی فرم می‌ماند + دکمه بزرگ‌نمایی/بازگشت
- Single Instance؛ اجرای دوم برنامه باز نمی‌شود و هنگام خروج JVM همین برنامه خاتمه می‌یابد

## ساختار
```text
ProformaPriceManager_FULL_LATEST/
├─ pom.xml
├─ config/
│  └─ database.properties
├─ src/main/java/ir/mnz/proformapricemanager/
│  ├─ ProformaPriceManager.java
│  ├─ SingleInstanceManager.java
│  ├─ CompanyConfig.java
│  ├─ CompanyConfigLoader.java
│  ├─ DatabaseManager.java
│  ├─ DocumentRepository.java
│  ├─ ProformaPriceFrame.java
│  ├─ DocumentListDialog.java
│  ├─ ManagementReportExcelExporter.java
│  ├─ ExportDocumentPrintDialog.java
│  └─ ExportDocumentPrintService.java
├─ Build.bat
├─ Run-Java23.bat
└─ Run-Java23-Debug.bat
```

## قبل از اجرا
1. فایل `config/database.properties` را با اطلاعات واقعی سه شرکت پر کنید.
2. در NetBeans یا ترمینال `mvn clean package` اجرا کنید.
3. خروجی نهایی: `target/ExportPriceManager.jar`
4. برای اجبار اجرای Java 23 از `Run-Java23.bat` استفاده کنید.

## نکته SQL Server قدیمی / TLS
این پروژه برای سازگاری با SQL Server قدیمی از `TLSv1` استفاده می‌کند. روی Java 23 ممکن است علاوه بر گزینه اجرایی، لازم باشد TLSv1 در `java.security` همان JDK نیز مجاز شده باشد؛ همان تنظیمی که قبلاً روی سیستم انجام شده بود. راه‌حل اصولی بلندمدت، فعال‌سازی TLS 1.2 روی SQL Server است.
