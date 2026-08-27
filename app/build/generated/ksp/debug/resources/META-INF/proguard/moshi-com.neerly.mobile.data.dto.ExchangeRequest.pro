-if class com.neerly.mobile.data.dto.ExchangeRequest
-keepnames class com.neerly.mobile.data.dto.ExchangeRequest
-if class com.neerly.mobile.data.dto.ExchangeRequest
-keep class com.neerly.mobile.data.dto.ExchangeRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.ExchangeRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.ExchangeRequest
-keepclassmembers class com.neerly.mobile.data.dto.ExchangeRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
