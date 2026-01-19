================================================================================
                              LIBSEARCH
                تطبيق البحث الأكاديمي الموحد
================================================================================

LibSearch هو أداة شاملة للبحث عن الأوراق العلمية تمكنك من البحث في عدة مصادر أكاديمية في نفس الوقت. فيها واجهة رسومية حديثة ب JavaFX وواجهة سطر الأوامر (REPL) تقدر تخدم بها.

================================================================================
                              الميزات
================================================================================

البحث الموحد
  - البحث في عدة مصادر أكاديمية في نفس الوقت:
    * arXiv، PubMed، CrossRef، Semantic Scholar، Google Scholar
  - مصادر جامعية: Primo، OBV Network (شبكة مكتبات النمسا)
  - مصادر المطورين: GitHub، StackOverflow، Reddit
  - محركات البحث العامة: DuckDuckGo، Bing، Brave، Wikipedia

صياغة الاستعلام المتقدمة
  - صياغة بحال grep مع تصفية المؤلفين، السنوات، أنواع الملفات
  - معاملات منطقية (AND، OR، NOT)
  - البحث في الحقول المحددة

الفهرسة النصية الكاملة
  - بحث offline على النتائج المخزنة ب Apache Lucene
  - فهرسة تلقائية للنتائج
  - فهرسة حسب الحقول (العنوان، المؤلفين، الملخص، السنة، الكلمات المفتاحية)

تحميل PDF
  - إدارة التحميلات بالصف
  - تتبع التقدم

ميزات البحث
  - مفضلات مع تاغات وملاحظات
  - تصدير الاستشهادات (BibTeX، RIS، EndNote)
  - مراقبة الكلمات المفتاحية والتنبيهات
  - إدارة الجلسات وتسجيل الأنشطة

واجهتين
  - واجهة رسومية حديثة ب JavaFX
  - CLI (REPL) ب Picocli/JLine3

================================================================================
                           المتطلبات
================================================================================

  - Java 21 أو أكثر (Amazon Corretto مفضل)
  - Gradle 8.5 أو أكثر

================================================================================
                         تعليمات البناء
================================================================================

1. نسخ المستودع:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. بناء المشروع:
   ./gradlew clean build

3. إنشاء JAR شامل (فيه جميع التبعيات):
   ./gradlew shadowJar

================================================================================
                          تعليمات التشغيل
================================================================================

وضعية الواجهة الرسومية
----------------------
الخيار 1 - باستعمال Gradle:
   ./gradlew run

الخيار 2 - سكريبت shell:
   ./libsearch.sh

الخيار 3 - JAR مباشر (بعد ./gradlew shadowJar):
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar


وضعية CLI
---------
أمر البحث:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar search "machine learning"

REPL تفاعلي:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar --interactive

إدارة المفضلات:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar bookmark list

إحصائيات الفهرس:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar index stats

تشخيص الشبكة:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar network diag


تشغيل الاختبارات
----------------
   ./gradlew test

================================================================================
                        دليل التجميع المفصل
================================================================================

المتطلبات
---------
1. تثبيت Java 21+ (Amazon Corretto مفضل):
   - macOS:    brew install --cask corretto
   - Windows:  تحميل من https://aws.amazon.com/corretto/
   - Linux:    sudo apt install openjdk-21-jdk

2. تحقق من تثبيت Java:
   java -version   (خاص يبان الإصدار 21 أو أكثر)

3. Gradle غادي يتحمل تلقائيا عبر gradlew

خطوات البناء
------------
1. نسخ ودخول مجلد المشروع:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. جعل gradlew قابل للتنفيذ (Unix/macOS):
   chmod +x gradlew

3. بناء المشروع:
   ./gradlew clean build

4. إنشاء JAR شامل بكل شيء:
   ./gradlew shadowJar

   الناتج: build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

5. تشغيل التطبيق:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

مشاكل البناء الشائعة
-------------------
- "JAVA_HOME غير محدد": حدد JAVA_HOME لمسار JDK 21
- Gradle ما تحملش: تحقق من الإنترنت والإعدادات
- JavaFX غير موجود: shadowJar فيه جميع تبعيات JavaFX

================================================================================
                          REPL (الوضع التفاعلي)
================================================================================

بدء REPL:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar repl

   أو ب --interactive:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar -i

أوامر REPL
----------
في REPL تقدر تدير أي أمر بلا "libsearch":

  libsearch> search "neural networks"
  libsearch> search "machine learning" -s arxiv,pubmed -m 20
  libsearch> bookmark list
  libsearch> bookmark list -v              (يبان الروابط والملاحظات والتاغات)
  libsearch> bookmark find ai              (لقا المفضلات بتاغ "ai")
  libsearch> bookmark delete <id>
  libsearch> session list
  libsearch> session create "بحثي"
  libsearch> index build
  libsearch> index status
  libsearch> network diag
  libsearch> clear                         (مسح الشاشة)
  libsearch> exit                          (خروج من REPL)

خيارات البحث في REPL
--------------------
  -s, --sources     المصادر مفصولة بفاصلة (arxiv,pubmed,crossref,scholar)
  -m, --max-results أقصى عدد نتائج لكل مصدر (افتراضي: 50)
  -o, --offline     البحث في الفهرس المحلي فقط
  -a, --author      تصفية حسب اسم المؤلف
  -y, --year        تصفية حسب السنة أو النطاق (2023 أو 2020-2024)
  -f, --format      صيغة الإخراج: table، json، simple

أمثلة:
  search "deep learning" -s arxiv -m 10 -f json
  search "CRISPR" -s pubmed,crossref -y 2020-2024
  search "machine learning" -o                    (بحث offline)

================================================================================
                             دليل استخدام الواجهة
================================================================================

تشغيل الواجهة بلا أي وسيطات:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

   أو ب Gradle:
   ./gradlew run

أقسام الواجهة الرئيسية
----------------------
1. لوحة البحث (فوق)
   - كتب الاستعلام
   - اختار المصادر: arXiv، PubMed، CrossRef، Semantic Scholar
   - حدد نطاق السنوات
   - اختار أقصى عدد نتائج لكل مصدر
   - فعل "وضع offline" للبحث في الفهرس المحلي

2. جدول النتائج (وسط)
   - عرض النتائج بأعمدة: العنوان، المؤلفين، السنة، المصدر، الوصول
   - ضغط على رؤوس الأعمدة للترتيب
   - ضغط مزدوج على صف لفتح الرابط
   - ضغط يمين لقائمة السياق: فتح الرابط، تحميل PDF، مفضلات، ...

3. الشريط الجانبي (يسار)
   - عرض حالة الاتصال بالمصادر (أخضر = متصل، أحمر = غير متصل)
   - عرض اسم الجلسة وعدد النتائج

4. شريط الحالة (تحت)
   - عرض الحالة الحالية، مؤشر التقدم، ووقت البحث

خيارات القائمة
-------------
قائمة الملفات:
  - جلسة جديدة       إنشاء جلسة بحث جديدة
  - فتح جلسة         التبديل لجلسة سابقة
  - تصدير النتائج    تصدير النتائج الحالية لملف CSV
  - خروج             إغلاق التطبيق

قائمة البحث:
  - بحث جديد         تركيز على حقل البحث
  - بحث متقدم        فتح خيارات البحث المتقدمة
  - مسح النتائج      مسح النتائج الحالية
  - سجل البحث        عرض وإعادة تشغيل عمليات البحث السابقة

قائمة المفضلات:
  - إضافة للمفضلات   إضافة النتائج المحددة للمفضلات
  - إدارة المفضلات   عرض وتصفية وحذف المفضلات

قائمة الأدوات:
  - مدير التحميلات   عرض وإدارة تحميلات PDF
  - مدير الفهرس      معلومات عن الفهرس المحلي
  - الإعدادات        تفضيلات التطبيق

قائمة المساعدة:
  - دليل المستخدم    شرح صياغة البحث والاختصارات
  - حول              معلومات الإصدار والتطبيق

اختصارات لوحة المفاتيح
---------------------
  Ctrl+N    بحث جديد (تركيز على حقل البحث)
  Ctrl+F    تركيز على حقل البحث
  Ctrl+B    إضافة للنتائج المحددة للمفضلات
  Ctrl+E    تصدير النتائج
  Enter     تنفيذ البحث (في حقل البحث)

صياغة الاستعلام
----------------
  بسيط:       machine learning
  عبارة:      "deep learning"
  مؤلف:       author:Smith  أو  author:"John Smith"
  سنة:        year:2023     أو  year:2020-2024
  منطقي:      neural AND networks
                AI OR ML
                cancer NOT treatment

إضافة للمفضلات
--------------
1. ضغط على زر ★ في عمود الإجراءات
2. أو ضغط يمين واختيار "مفضلة"
3. أو تحديد عدة نتائج واستخدام قائمة المفضلات
4. إدارة المفضلات من القائمة

تصدير البيانات
--------------
1. ملف > تصدير النتائج يحفظ النتائج في CSV
2. ضغط يمين > تصدير الاستشهاد ل BibTeX/RIS/EndNote
3. نسخ DOI من قائمة السياق

================================================================================
                         بنية المشروع
================================================================================

src/main/java/com/example/teamse1csdchcw/
|
+-- domain/                    نماذج البيانات (POJOs، enums)
|   +-- search/               SearchResult، SearchQuery، AcademicPaper
|   +-- source/               SourceType enum
|   +-- user/                 نموذج Bookmark
|   +-- monitoring/           إعدادات التنبيه
|   +-- export/               نوع التصدير ExportFormat
|
+-- service/                  طبقة المنطق
|   +-- search/               FederatedSearchService، QueryParserService
|   +-- connector/            كونكتورات المصادر + factory
|   +-- index/                IndexService (Lucene)، LocalSearchService
|   +-- download/             DownloadService (الصف)
|   +-- bookmark/             BookmarkService
|   +-- export/               CitationExportService
|   +-- alert/                AlertService
|   +-- monitoring/           MonitoringService، NotificationService
|   +-- session/              SessionService، JournalService
|   +-- network/              ProxyConfiguration، NetworkDiagnostics
|
+-- repository/               الوصول للبيانات (SQLite)
|   +-- sqlite/               SQLiteConnection، DatabaseInitializer
|
+-- ui/                       واجهة JavaFX
|   +-- controller/           MainController، SearchController
|   +-- component/            مكونات الواجهة
|   +-- model/                نماذج JavaFX
|
+-- cli/                      CLI
|   +-- LibSearchCLI.java     أمر CLI الرئيسي
|   +-- SearchCommand.java
|   +-- BookmarkCommand.java
|   +-- IndexCommand.java
|   +-- NetworkCommand.java
|   +-- MonitorCommand.java
|   +-- SessionCommand.java
|   +-- ReplCommand.java
|
+-- config/                   إدارة الإعدادات
+-- exception/                استثناءات مخصصة
+-- util/                     أدوات (HTTP، ملفات، تحليل، تحقق)

src/main/resources/
|
+-- config/application.yaml   الإعدادات الافتراضية
+-- db/schema.sql             مخطط SQLite
+-- com/example/teamse1csdchcw/
    +-- fxml/                 واجهات FXML
    +-- css/main.css          تنسيق

================================================================================
                            التقنيات
================================================================================

اللغة والبناء:
  - Java 21 (Amazon Corretto)
  - Gradle 8.11.1

إطار الواجهة:
  - JavaFX 21.0.6
  - ControlsFX، FormsFX، ValidatorFX، TilesFX، BootstrapFX

مكتبات الخلفية:
  - OkHttp3 4.12.0         HTTP Client
  - Jsoup 1.17.2           تحليل HTML
  - Apache Lucene 9.9.1    بحث نصي كامل
  - SQLite JDBC 3.45.0     قاعدة بيانات
  - Jackson 2.16.1         معالجة JSON/YAML
  - JBibTeX 1.0.18         تنسيق الاستشهادات

إطار CLI:
  - Picocli 4.7.5          تحليل الأوامر
  - JLine3 3.25.1          واجهة التيرمينال

التسجيل:
  - SLF4J 2.0.11
  - Logback 1.4.14

الاختبار:
  - JUnit 5.12.1
  - Mockito 5.8.0

================================================================================
                           الإعدادات
================================================================================

ملف الإعدادات: ~/.libsearch/config.yaml

الإعدادات الرئيسية:
  - تفعيل/تعطيل المصادر ومفاتيح API
  - إعدادات البحث (أقصى مصادر متزامنة، المهلة، تحديد المعدل)
  - مجلد التحميل وحد أقصى للتحميلات
  - مجلد الفهرس وتفعيل الفهرسة التلقائية
  - مسار قاعدة البيانات ووضع WAL

قاعدة البيانات: ~/.libsearch/data/libsearch.db

الجداول:
  - sessions          تتبع الجلسات
  - search_history    سجل الاستعلامات
  - search_results    نتائج مخزنة
  - bookmarks         نتائج محفوظة مع تاغات/ملاحظات
  - alerts            إعدادات مراقبة الكلمات
  - alert_matches     سجلات التنبيهات
  - downloads         قائمة التحميلات
  - journal           سجل الأنشطة
  - config            إعدادات رئيسية

================================================================================
                              المؤلفون
================================================================================

  - qwitch13 (nebulai13)
  - zahieddo

================================================================================
                              الإصدار
================================================================================

الإصدار: 1.0.0

================================================================================
