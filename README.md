# Dynamic Island Clone (Android)

এটা একটা Android প্রজেক্ট, যেটা:
1. ফোনের **সব নোটিফিকেশন পড়ার পারমিশন** নেয় (Notification Listener)
2. নোটিফিকেশন এলে স্ক্রিনের উপরে একটা **কালো পিল-শেপ ভাসমান আইল্যান্ড** দেখায় — iPhone-এর Dynamic Island-এর মতো — অ্যাপ আইকন, টাইটেল ও টেক্সট সহ, তারপর কয়েক সেকেন্ড পর আবার ছোট হয়ে যায়।

⚠️ **গুরুত্বপূর্ণ:** iPhone-এর আসল Dynamic Island হার্ডওয়্যার + iOS সিস্টেমের নিজস্ব জিনিস, থার্ড-পার্টি অ্যাপ সেটার জায়গা দখল করতে পারে না। এটা Android-এ ওভারলে উইন্ডো দিয়ে সেই লুক-অ্যান্ড-ফিল **নকল (replicate)** করে — আসল সিস্টেম ফিচার না।

## কীভাবে বিল্ড করবি (Android Studio দিয়ে)

1. Android Studio খুলে **New Project → Empty Views Activity** সিলেক্ট কর।
   - Package name দিবি: `com.example.dynamicisland`
   - Language: Kotlin
   - Minimum SDK: API 26

2. প্রজেক্ট তৈরি হয়ে গেলে, এই zip-এর ফাইলগুলো দিয়ে নিচের ফাইলগুলো **replace/add** কর (path মিলিয়ে):
   - `app/build.gradle`
   - `app/src/main/AndroidManifest.xml`
   - `app/src/main/java/com/example/dynamicisland/MainActivity.kt`
   - `app/src/main/java/com/example/dynamicisland/NotificationMonitorService.kt` (নতুন ফাইল)
   - `app/src/main/res/layout/activity_main.xml`
   - `app/src/main/res/layout/island_overlay.xml` (নতুন ফাইল)
   - `app/src/main/res/drawable/island_pill_bg.xml` (নতুন ফাইল)
   - `app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml`

   (`ic_launcher` আইকনগুলো Android Studio-র টেমপ্লেটেই থাকবে, ওগুলো ছুঁবি না।)

3. উপরে ডান দিকে **"Sync Now"** চাপ দিয়ে Gradle sync হতে দে (ইন্টারনেট লাগবে, dependency ডাউনলোড হবে)।

4. ফোন/এমুলেটরে **Run ▶️** চাপ দিয়ে ইনস্টল কর।

## অ্যাপ ব্যবহার করবি কীভাবে

1. অ্যাপ খুলে **"নোটিফিকেশন পারমিশন দাও"** বাটনে চাপ দিলে সেটিংস স্ক্রিন খুলবে — সেখানে গিয়ে এই অ্যাপের টগল **অন** কর।
2. **"ওভারলে পারমিশন দাও"** বাটনে চাপ দিয়ে "Display over other apps" পারমিশন **অন** কর।
3. এবার যেকোনো অ্যাপ থেকে নোটিফিকেশন (WhatsApp, Messenger, ইত্যাদি) আসলেই স্ক্রিনের উপরে কালো পিলটা বড় হয়ে অ্যাপের আইকন, টাইটেল ও মেসেজ দেখাবে, তারপর ৪ সেকেন্ড পর ছোট হয়ে যাবে।

## কোড কীভাবে কাজ করে (সংক্ষেপে)

- `NotificationMonitorService` — এটা `NotificationListenerService` ইনহেরিট করে, তাই সিস্টেম এটাকে সব নোটিফিকেশনের তথ্য পাঠায় (`onNotificationPosted`)।
- এই সার্ভিসই `WindowManager` দিয়ে একটা overlay view (`island_overlay.xml`) স্ক্রিনের উপরে বসিয়ে রাখে — সবসময় ছোট পিল হিসেবে, নোটিফিকেশন এলে অ্যানিমেশনের মাধ্যমে বড় হয়ে তথ্য দেখায়।
- `MainActivity` শুধু দুটো দরকারি পারমিশন (Notification Access, Overlay/Display over other apps) সহজে চালু করার শর্টকাট দেয়।

## নিজের মতো কাস্টমাইজ করতে চাইলে

- পিলের সাইজ/রঙ বদলাতে `NotificationMonitorService.kt`-তে `collapsedWidthDp/HeightDp`, `expandedWidthDp/HeightDp` আর `island_pill_bg.xml`-এর color/corner radius বদলা।
- কতক্ষণ পর ছোট হবে সেটা বদলাতে `mainHandler.postDelayed(runnable, 4000)`-এ 4000 (মিলিসেকেন্ড) বদলা।
- নির্দিষ্ট অ্যাপ (যেমন শুধু WhatsApp) থেকে নোটিফিকেশন দেখাতে চাইলে `onNotificationPosted`-এ `sbn.packageName` চেক করে ফিল্টার কর।
