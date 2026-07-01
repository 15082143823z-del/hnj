# ============================================
# VideoCrawler ProGuard/R8 混淆规则
# ============================================

# ---- Kotlin & Coroutines ----
-keepattributes *Annotation*, Signature, SourceFile, LineNumberTable
-keep class kotlinx.coroutines.** { *; }

# ---- Gson 序列化 ----
-keepattributes SerializedName
-keep class com.coder.videocrawler.model.** { *; }
-keepclassmembers class com.coder.videocrawler.model.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- OkHttp (反射使用较多) ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- ViewModel 反射创建 ----
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ---- Compose UI 组件（避免重组异常） ----
-keep class com.coder.videocrawler.ui.screen.** { *; }

# ---- Release 移除 Log ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
