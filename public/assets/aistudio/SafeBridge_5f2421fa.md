أنت مهندس Android محترف وخبير في Kotlin وAndroid SDK وBinder IPC.

أنشئ تطبيق Android تعليميًا مستقلًا باسم:
SafeBridge

هدف التطبيق:
إنشاء نظام آمن للتواصل بين تطبيق Manager وتطبيقات Client عبر Android Binder، مع خدمة محلية محدودة لتنفيذ عمليات مسموحة داخل Sandbox. التطبيق مستوحى من المفاهيم العامة لخدمات Binder، لكنه ليس نسخة من Shizuku ولا يستخدم كوده أو مفاتيح توقيعه أو أسماء حزم Rikka/Shizuku.

المتطلبات العامة:
- اللغة: Kotlin
- الواجهة: Jetpack Compose
- بنية المشروع: Gradle Kotlin DSL
- Minimum SDK: 26
- Target SDK: أحدث إصدار مستقر متاح
- Architecture: MVVM أو Clean Architecture
- استخدم Coroutines وStateFlow
- استخدم Material 3
- فعّل R8/ProGuard في نسخة Release
- اكتب كودًا واضحًا وقابلًا للصيانة
- أضف تعليقات توضيحية للأجزاء المتعلقة بـBinder وIPC

أنشئ المكونات التالية:

1. تطبيق Manager:
- شاشة رئيسية تعرض حالة الخدمة:
  - متوقفة
  - قيد التشغيل
  - متوقفة بسبب خطأ
- زر Start Service
- زر Stop Service
- زر Restart Service
- سجل Logs داخل التطبيق
- قائمة بالتطبيقات العميلة التي طلبت الاتصال
- شاشة لإدارة الموافقات
- إمكانية منح أو رفض صلاحية Client
- دعم صلاحية مؤقتة تنتهي بعد مدة محددة
- شاشة Settings

2. Binder Service:
أنشئ Android Service باسم:
SafeBridgeService

يجب أن:
- يعمل داخل عملية التطبيق العادية فقط.
- لا يستخدم root.
- لا يستخدم ADB لتنفيذ أوامر على الجهاز.
- لا يستعمل WRITE_SECURE_SETTINGS أو صلاحيات نظام غير متاحة للتطبيق العادي.
- يعرّف واجهة AIDL أو Binder واضحة، مثل:
  - getServiceVersion()
  - ping()
  - requestClientAccess()
  - revokeClientAccess()
  - getClientStatus()
  - executeSandboxTask()
  - registerCallback()
  - unregisterCallback()
- يستخدم Parcelable لنقل البيانات.
- يعالج Binder death باستخدام IBinder.DeathRecipient.
- يرفض الطلبات غير المصرح بها.
- يتحقق من UID واسم الحزمة حيثما أمكن.
- لا يثق بالبيانات القادمة من Intent أو Binder دون تحقق.

3. تطبيق Client تجريبي:
أنشئ تطبيق Client منفصل باسم:
SafeBridgeDemoClient

يجب أن:
- يتصل بـ SafeBridgeService.
- يعرض حالة الاتصال.
- يرسل طلب Authorization إلى Manager.
- يعرض نتيجة الموافقة أو الرفض.
- يستدعي ping().
- يستدعي executeSandboxTask() بمهمة آمنة.
- يتعامل مع انقطاع Binder وإعادة الاتصال.
- لا يحاول الوصول إلى ملفات تطبيقات أخرى أو خدمات النظام.

4. Sandbox للعمليات:
أنشئ Sandbox داخليًا في:
context.filesDir/sandbox/

اسمح فقط بالعمليات التالية:
- إنشاء ملف داخل Sandbox
- قراءة ملف داخل Sandbox
- كتابة ملف داخل Sandbox
- حذف ملف داخل Sandbox
- إنشاء مجلد داخل Sandbox
- عرض قائمة الملفات

قواعد الأمان:
- امنع Path Traversal مثل ../.
- امنع المسارات المطلقة.
- امنع الوصول إلى /data أو /system أو /sdcard أو أي مسار خارج Sandbox.
- طبّق canonical path validation.
- لا تسمح بتنفيذ أوامر shell حقيقية.
- إذا احتجت إلى محاكاة التنفيذ، استخدم قائمة مهام وهمية مثل:
  - echo
  - get_status
  - list_sandbox
- ارفض أي أمر غير موجود في allowlist.

5. نظام الصلاحيات:
أنشئ PermissionManager داخليًا يدعم:
- packageName
- uid
- granted
- grantedAt
- expiresAt
- oneTime
- reason

يجب أن:
- يخزن البيانات محليًا باستخدام DataStore أو Room.
- يطلب موافقة المستخدم قبل منح الوصول.
- يدعم إلغاء الصلاحية.
- يلغي الصلاحيات المنتهية تلقائيًا.
- لا يمنح أي صلاحيات Android نظامية.
- لا يستخدم صلاحيات مخفية أو APIs غير موثقة.

6. Watchdog تعليمي:
أنشئ Watchdog داخليًا لمراقبة حالة SafeBridgeService:
- استخدم lifecycle وCoroutine بدلاً من آليات النظام الحساسة.
- سجّل حالة الخدمة.
- أعد الاتصال من Client عند انقطاع Binder.
- لا تنشئ آلية تشغيل تلقائي بعد إقلاع الجهاز إلا إذا كانت ضرورية وبموافقة واضحة من المستخدم.
- لا تستخدم Alarm لتجاوز قيود Android الحديثة.

7. واجهة المستخدم:
أنشئ شاشات Compose التالية:
- DashboardScreen
- ServiceControlScreen
- ClientRequestsScreen
- PermissionDetailsScreen
- LogsScreen
- SettingsScreen
- AboutScreen

اجعل التصميم:
- بسيطًا وحديثًا.
- يدعم الوضع الفاتح والداكن.
- يدعم اللغة العربية والإنجليزية.
- يعرض رسائل خطأ مفهومة.
- لا يعرض أو يخزن بيانات حساسة.

8. السجل Logging:
أنشئ Logger داخليًا:
- مستويات DEBUG وINFO وWARNING وERROR.
- لا تسجل كلمات المرور أو الرموز السرية أو البيانات الخاصة.
- خزّن آخر 500 سجل فقط.
- أضف خيار مسح السجلات.
- أضف خيار تصدير السجل إلى ملف نصي داخل مشاركة Android الرسمية.

9. الأمان:
نفّذ مراجعة أمنية للكود تشمل:
- عدم تصدير Components دون ضرورة.
- حماية Service من الطلبات غير المصرح بها.
- التحقق من UID وpackage.
- التحقق من كل المدخلات.
- منع Intent spoofing.
- منع Path Traversal.
- منع Arbitrary Code Execution.
- منع Shell Injection.
- منع تسريب Binder.
- منع تخزين بيانات حساسة في Logs.
- استخدام explicit intents.
- استخدام FLAG_IMMUTABLE عند الحاجة.
- إضافة android:exported بشكل صريح لكل Component.

10. الاختبارات:
اكتب اختبارات Unit وInstrumented تشمل:
- اختبار منح الصلاحية.
- اختبار رفض الصلاحية.
- اختبار انتهاء الصلاحية.
- اختبار one-time permission.
- اختبار Binder disconnect.
- اختبار UID غير صحيح.
- اختبار Path Traversal.
- اختبار الوصول إلى ملف خارج Sandbox.
- اختبار أوامر غير موجودة في allowlist.
- اختبار إعادة الاتصال بالخدمة.
- اختبار Room/DataStore.

11. الملفات المطلوبة:
أنشئ مشروعًا كاملًا يتضمن:
- settings.gradle.kts
- build.gradle.kts
- app/build.gradle.kts
- AndroidManifest.xml
- ملفات Kotlin منظمة حسب الحزم:
  - service
  - ipc
  - permission
  - sandbox
  - logging
  - ui
  - data
  - security
- ملفات AIDL أو Binder interfaces.
- README.md باللغة العربية والإنجليزية.
- SECURITY.md يشرح حدود الأمان.
- ملف توثيق يشرح طريقة التشغيل والاختبار.
- مخطط Mermaid يوضح:
  Manager -> SafeBridgeService -> Client
  PermissionManager
  Sandbox
  Binder callbacks

طريقة التنفيذ:
- ابدأ بإنشاء هيكل المشروع.
- ثم أنشئ طبقة البيانات.
- ثم Binder Service.
- ثم Manager UI.
- ثم Client Demo.
- ثم Sandbox.
- ثم الاختبارات.
- بعد كل مرحلة تحقق من عدم وجود أخطاء compile.
- لا تضع أي كود تخميني غير قابل للبناء.
- إذا كانت هناك API غير متاحة، استخدم البديل الرسمي الموثق.
- اعرض في النهاية قائمة بالملفات التي تم إنشاؤها وطريقة البناء والتشغيل.

قيود قانونية وأمنية إلزامية:
- لا تنسخ كود Shizuku أو Rish أو أي مشروع آخر.
- لا تستخدم package names مثل moe.shizuku أو rikka.shizuku.
- لا تستخدم مفاتيح توقيع خارجية.
- لا تنشئ root service.
-يسمح باستخدام ADB فقط من خلال SafeAdbExecutor، وبأوامر محددة داخل Allowlist ثابتة، وبعد مصادقة الجهاز وموافقة المستخدم. يمنع تنفيذ shell عام أو أوامر غير معروفة أو أوامر تتطلب root.
لا تنفذ أوامر shell عامة. يسمح فقط بتنفيذ أوامر ADB محددة مسبقًا، بعد تحويلها إلى معاملات منفصلة والتحقق منها، مع حدود للمهلة والمخرجات والصلاحيات.
- لا تتجاوز صلاحيات Android.
- لا تصل إلى بيانات التطبيقات الأخرى.
- لا تنشئ آلية تحايل على قيود النظام.
- اجعل كل العمليات داخل Sandbox الخاص بالتطبيق.
12. دعم ADB الآمن والمقيّد:

أضف طبقة مستقلة باسم:
SafeAdbExecutor

هدفها:
تنفيذ مجموعة محدودة ومعلنة مسبقًا من أوامر ADB على جهاز Android يملكه المستخدم أو لديه تفويض صريح لإدارته.

قيود أساسية:
- لا تسمح بتنفيذ أوامر shell عشوائية من المستخدم.
- لا تستخدم root.
- لا تحاول تجاوز قفل الجهاز أو حماية Android.
- لا تنفذ أوامر على أجهزة غير مقترنة أو غير موثوقة.
- لا تحفظ مفاتيح ADB أو بيانات الاقتران في Logs.
- لا تنفذ أوامر في الخلفية دون إظهار الحالة للمستخدم.
- يجب أن يوافق المستخدم صراحة قبل أول اتصال وقبل كل عملية عالية الخطورة.
- يجب عرض الجهاز المستهدف ورقمه التعريفي واسم العملية قبل التنفيذ.
- يجب توفير زر واضح لإيقاف التنفيذ وإلغاء جلسة ADB.

طريقة الاتصال:
- استخدم مكتبة ADB موثوقة ومراجعة، أو Companion Service منفصل يعمل على جهاز المستخدم.
- لا تعتمد على استدعاء أوامر adb من خلال Runtime.exec() بشكل مباشر.
- إذا كان التنفيذ من جهاز كمبيوتر، استخدم قناة محلية آمنة ومصادقًا عليها بين التطبيق وCompanion Service.
- إذا كان التنفيذ من Android نفسه، استخدم فقط API رسميًا أو مكتبة موثوقة تدعم ADB/Wireless Debugging.
- لا تحاول الحصول على صلاحيات ADB بطرق غير رسمية.
- افصل طبقة ADB عن واجهة المستخدم وعن Binder Service.

نموذج الجلسة:
أنشئ AdbSession يحتوي على:
- deviceId
- transportType
- connectionState
- authenticated
- createdAt
- lastActivityAt
- userApproved
- expiresAt

حالات الاتصال المسموحة:
- DISCONNECTED
- DISCOVERING
- PAIRING_REQUIRED
- PAIRING
- CONNECTED
- AUTHENTICATED
- EXPIRED
- FAILED

قائمة الأوامر المسموحة فقط:

قراءة معلومات غير حساسة:
- get-state
- get-serialno
- shell getprop ro.build.version.sdk
- shell getprop ro.product.model
- shell pm list packages -3

عمليات ملفات داخل مجلد التطبيق المخصص فقط:
- shell mkdir -p <approved_directory>
- push <approved_local_file> <approved_remote_directory>
- pull <approved_remote_file> <approved_local_directory>
- shell ls <approved_directory>

عمليات تشخيص محدودة:
- shell dumpsys activity activities
- shell dumpsys package <approved_package>
- shell logcat -d -t <limited_number_of_lines>

يجب تعريف كل أمر ككائن AdbCommand يحتوي على:
- commandId
- displayName
- arguments
- riskLevel
- requiresConfirmation
- timeout
- outputLimit
- allowedDevices
- allowedPackages
- auditDescription

قواعد التحقق:
- استخدم Allowlist ثابتة داخل التطبيق.
- لا تسمح بتمرير نص shell كامل.
- لا تقبل command string من المستخدم.
- حوّل كل أمر إلى arguments منفصلة ومتحقق منها.
- ارفض الرموز:
  ; & | && || ` $ ( ) { } < > \n \r
- ارفض المسارات المطلقة غير المسموحة.
- ارفض:
  ../
  ..\
  /system
  /data
  /vendor
  /proc
  /sys
  /dev
  /sdcard
- طبّق canonical path validation على كل مسار.
- امنع wildcard غير الضروري مثل * و?.
- امنع إعادة التوجيه إلى ملفات أو pipes.
- حدّد طول كل argument وعدد arguments.
- افحص أسماء الحزم باستخدام Android PackageManager أو صيغة package name صحيحة.
- افحص deviceId مقابل الأجهزة التي وافق عليها المستخدم.

تصنيف المخاطر:

LOW:
- get-state
- get-serialno
- قراءة model وSDK
- قراءة قائمة تطبيقات المستخدم

MEDIUM:
- pull
- push
- قراءة logcat
- قراءة dumpsys

HIGH:
- حذف ملفات
- تثبيت أو إزالة تطبيق
- تغيير إعدادات الجهاز
- إعادة تشغيل الجهاز
- تنفيذ أوامر تتطلب صلاحيات خاصة

قاعدة إلزامية:
- لا تنفذ أي أمر HIGH.
- يمكن تنفيذ LOW وMEDIUM فقط بعد التحقق والموافقة.
- يجب أن تظهر نافذة تأكيد للعمليات MEDIUM.
- اعرض الأمر بصيغة مفهومة للمستخدم قبل التنفيذ.
- لا تعرض أو تسمح بإخفاء الأمر الفعلي عن المستخدم.

المهل والحدود:
- مهلة اتصال محددة.
- مهلة تنفيذ لكل أمر.
- حد أقصى لحجم stdout وstderr.
- حد أقصى لعدد الأسطر.
- إيقاف العملية عند تجاوز الحد.
- إلغاء الجلسة عند عدم النشاط.
- منع أكثر من جلسة ADB نشطة لكل جهاز.
- منع تنفيذ أوامر متوازية على نفس الجهاز.

التسجيل والمراجعة:
- أنشئ AuditLog لكل عملية يحتوي على:
  - timestamp
  - deviceId بعد إخفاء جزء منه
  - commandId
  - result
  - duration
  - exitCode
  - userApproval
- لا تسجل كلمات المرور أو مفاتيح ADB أو محتوى الملفات أو الرموز السرية.
- لا تسجل stdout كاملًا إذا كان قد يحتوي على بيانات حساسة.
- احتفظ بآخر 500 عملية فقط.
- أضف زرًا لمسح سجل التدقيق.
- أضف خيار تصدير السجل بعد تنقيح البيانات الحساسة.

الأمان الشبكي:
- بالنسبة لـWireless ADB، اعرض عنوان الجهاز والمنفذ للمستخدم قبل الاقتران.
- لا تقبل الاقتران تلقائيًا.
- لا تحفظ pairing code.
- استخدم TLS أو آلية المصادقة التي توفرها مكتبة ADB الرسمية.
- لا تتصل بعناوين IP عشوائية.
- لا تسمح بالاتصال من الإنترنت العام.
- اسمح فقط بالشبكة المحلية أو localhost.
- أغلق الجلسة عند تغيير الشبكة أو انتهاء مهلة الجلسة.
- لا تعمل كـADB server مفتوح على جميع الواجهات.

واجهة المستخدم:
أنشئ شاشة AdbDevicesScreen تعرض:
- الأجهزة المكتشفة.
- حالة كل جهاز.
- deviceId بعد إخفاء جزء منه.
- نوع الاتصال.
- وقت آخر اتصال.
- زر Pair.
- زر Disconnect.
- زر Forget Device.

أنشئ شاشة AdbCommandApprovalScreen تعرض:
- اسم الجهاز.
- العملية التي ستنفذ.
- سبب طلبها.
- مستوى الخطورة.
- المسارات والملفات المتأثرة.
- المهلة.
- حد المخرجات.
- أزرار Allow Once وCancel.

الاختبارات الإلزامية:
- رفض الأوامر غير الموجودة في Allowlist.
- رفض command injection.
- رفض path traversal.
- رفض المسارات الحساسة.
- رفض deviceId غير المعروف.
- رفض التنفيذ بعد انتهاء الجلسة.
- رفض التنفيذ دون موافقة المستخدم.
- اختبار timeout.
- اختبار output limit.
- اختبار إلغاء العملية.
- اختبار انقطاع الشبكة.
- اختبار تغيير الجهاز أثناء الجلسة.
- اختبار عدم تخزين مفاتيح ADB في السجل.
- اختبار عدم وجود ADB listener مفتوح على 0.0.0.0.
الأفضل أن يكون التصميم بهذا الشكل:
Manager UI
   |
   v
PermissionManager
   |
   v
SafeAdbExecutor
   |
   +--> Command Allowlist
   +--> Argument Validator
   +--> Device Authenticator
   +--> Timeout Controller
   +--> Audit Logger
   |
   v
ADB Companion / Official ADB Transport
   |
   v
Authorized Android Device

:
adb shell <نص يكتبه المستخدم>
:
AdbCommand.GetDeviceState
AdbCommand.GetAndroidVersion
AdbCommand.ListUserPackages
AdbCommand.PullFile(
    remotePath = approvedRemotePath,
    localDirectory = approvedLocalDirectory
)
