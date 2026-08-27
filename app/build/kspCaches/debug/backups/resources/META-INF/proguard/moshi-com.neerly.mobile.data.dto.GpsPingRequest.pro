-if class com.neerly.mobile.data.dto.GpsPingRequest
-keepnames class com.neerly.mobile.data.dto.GpsPingRequest
-if class com.neerly.mobile.data.dto.GpsPingRequest
-keep class com.neerly.mobile.data.dto.GpsPingRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.GpsPingRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.GpsPingRequest
-keepclassmembers class com.neerly.mobile.data.dto.GpsPingRequest {
    public synthetic <init>(double,double,java.lang.Double,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
