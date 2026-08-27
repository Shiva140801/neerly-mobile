-if class com.neerly.mobile.data.dto.NotificationResponse
-keepnames class com.neerly.mobile.data.dto.NotificationResponse
-if class com.neerly.mobile.data.dto.NotificationResponse
-keep class com.neerly.mobile.data.dto.NotificationResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.NotificationResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.NotificationResponse
-keepclassmembers class com.neerly.mobile.data.dto.NotificationResponse {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
