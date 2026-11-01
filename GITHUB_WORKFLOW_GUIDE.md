# دليل بناء تطبيق SafeBridge عبر GitHub Actions 🚀
# SafeBridge GitHub Actions Workflow Guide

[العربية](#العربية) | [English](#english)

---

## العربية

تم إعداد المشروع بالكامل ليعمل تلقائياً مع **GitHub Actions (Workflows)** لتحويل الكود المصدري إلى ملفات تثبيت أندرويد جاهزة (`.apk` و `.aab`) عند كل تحديث أو إصدار جديد.

### 📁 ملفات الـ Workflows المضافة
1. **`.github/workflows/android-build.yml`**:
   - يعمل تلقائياً عند أي `push` أو `pull_request` على فرعي `main` و `master`، أو يدوياً عبر زر `Run workflow`.
   - يقوم بتجهيز بيئة Java 17 و Gradle تلقائياً.
   - ينفذ اختبارات الـ Unit و الـ Robolectric للتأكد من سلامة الأمان.
   - يقوم بتجميع تطبيق أندرويد بصيغة **Debug APK** و **Release APK** و **Release App Bundle (AAB)**.
   - يرفع الملفات الناتجة كـ **Artifacts** قابلة للتنزيل المباشر لمدة 14 يوماً.

2. **`.github/workflows/release.yml`**:
   - يعمل عند إنشاء Tag جديد (مثل `v1.0.0`) أو تشغيله يدوياً.
   - يقوم بإنشاء **GitHub Release** رسمي ويرفق ملفات الـ `.apk` و `.aab` لتنزيلها مباشرة إلى الهاتف.

---

### 📲 خطوات التشغيل والتنزيل:

#### 1. رفع المشروع إلى GitHub:
- من خلال قائمة إعدادات AI Studio: اختر **Push to GitHub** أو صدّر المشروع كملف ZIP وادفعه إلى مستودعك:
```bash
git init
git add .
git commit -m "Initial commit for SafeBridge Android app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

#### 2. متابعة البناء التلقائي:
1. ادخل إلى مستودعك على **GitHub**.
2. اضغط على تبويب **Actions** في الشريط العلوي.
3. ستشاهد سير العمل **Build Android APK & Bundle** يعمل تلقائياً.
4. انتظر حتى يكتمل البناء (تظهر علامة صح خضراء `✓`).

#### 3. تنزيل ملف الـ APK:
- اضغط على تشغيل الـ Workflow المكتمل.
- انزل لأسفل الصفحة إلى قسم **Artifacts**.
- ستجد الملفات التالية جاهزة للتنزيل بضغطة زر:
  - `SafeBridge-debug-apk`: يحتوي على ملف الـ APK التجريبي الجاهز للتثبيت الفوري على أي هاتف أندرويد.
  - `SafeBridge-release-apk`: نسخة الإنتاج الموقعة والمحسنة.
  - `SafeBridge-release-bundle`: ملف حزمة التطبيق `.aab` الخاص بمتجر Google Play.
  - `SafeBridge-test-report`: تقرير نتائج الاختبارات.

---

## English

The project is fully pre-configured with **GitHub Actions CI/CD workflows** to build, test, and package your Android app into installable `.apk` and `.aab` binaries.

### 📁 Workflow Files Included
1. **`.github/workflows/android-build.yml`**:
   - Triggered on `push` or `pull_request` to `main` / `master`, or manually via `workflow_dispatch`.
   - Sets up Temurin JDK 17 and caches Gradle dependencies.
   - Runs unit and Robolectric test suites.
   - Compiles **Debug APK**, **Release APK**, and **Google Play App Bundle (AAB)**.
   - Uploads all artifacts for direct download.

2. **`.github/workflows/release.yml`**:
   - Triggered when pushing a version tag (e.g., `git push origin v1.0.0`) or manually.
   - Creates a formal GitHub Release and attaches the installable `.apk` and `.aab` assets.

### 🚀 Quick Start:
1. Push your repository to GitHub.
2. Navigate to the **Actions** tab on your GitHub repository.
3. Select **Build Android APK & Bundle** to view the live build logs.
4. Once completed, scroll to **Artifacts** to download `SafeBridge-debug-apk` or `SafeBridge-release-apk`.
