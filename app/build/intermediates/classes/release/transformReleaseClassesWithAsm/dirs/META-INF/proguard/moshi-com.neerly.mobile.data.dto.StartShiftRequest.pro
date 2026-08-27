-if class com.neerly.mobile.data.dto.StartShiftRequest
-keepnames class com.neerly.mobile.data.dto.StartShiftRequest
-if class com.neerly.mobile.data.dto.StartShiftRequest
-keep class com.neerly.mobile.data.dto.StartShiftRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.StartShiftRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.StartShiftRequest
-keepclassmembers class com.neerly.mobile.data.dto.StartShiftRequest {
    public synthetic <init>(com.neerly.mobile.data.dto.GpsPoint,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
