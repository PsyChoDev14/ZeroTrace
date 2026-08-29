# ZeroTrace ProGuard & R8 Optimization Rules

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Native JNI & Core Engines (libXray, hev-socks5-tunnel)
-keep class libXray.** { *; }
-keep class hev.sockstun.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Data Models & Repositories
-keep class lk.novalink.zerotrace.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Keep VPN Service & Core Managers
-keep class lk.novalink.zerotrace.service.** { *; }
-keep class lk.novalink.zerotrace.core.** { *; }
-keep class lk.novalink.zerotrace.util.** { *; }

# Keep ZXing & Camera QR Scanner
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
