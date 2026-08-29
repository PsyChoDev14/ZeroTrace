# Add project specific ProGuard rules here.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Gson rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class lk.novalink.zerotrace.data.model.** { *; }
