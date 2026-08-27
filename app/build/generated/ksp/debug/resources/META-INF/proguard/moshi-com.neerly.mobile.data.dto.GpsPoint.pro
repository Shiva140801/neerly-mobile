-if class com.neerly.mobile.data.dto.GpsPoint
-keepnames class com.neerly.mobile.data.dto.GpsPoint
-if class com.neerly.mobile.data.dto.GpsPoint
-keep class com.neerly.mobile.data.dto.GpsPointJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.GpsPoint
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.GpsPoint
-keepclassmembers class com.neerly.mobile.data.dto.GpsPoint {
    public synthetic <init>(double,double,java.lang.Double,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
