-if class com.neerly.mobile.data.dto.RegisterDeviceRequest
-keepnames class com.neerly.mobile.data.dto.RegisterDeviceRequest
-if class com.neerly.mobile.data.dto.RegisterDeviceRequest
-keep class com.neerly.mobile.data.dto.RegisterDeviceRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.RegisterDeviceRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.RegisterDeviceRequest
-keepclassmembers class com.neerly.mobile.data.dto.RegisterDeviceRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
