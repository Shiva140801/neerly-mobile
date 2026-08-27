-if class com.neerly.mobile.data.dto.FileComplaintRequest
-keepnames class com.neerly.mobile.data.dto.FileComplaintRequest
-if class com.neerly.mobile.data.dto.FileComplaintRequest
-keep class com.neerly.mobile.data.dto.FileComplaintRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.FileComplaintRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.FileComplaintRequest
-keepclassmembers class com.neerly.mobile.data.dto.FileComplaintRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
