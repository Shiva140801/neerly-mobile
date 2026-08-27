-if class com.neerly.mobile.data.dto.WalletTopupRequest
-keepnames class com.neerly.mobile.data.dto.WalletTopupRequest
-if class com.neerly.mobile.data.dto.WalletTopupRequest
-keep class com.neerly.mobile.data.dto.WalletTopupRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.WalletTopupRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.WalletTopupRequest
-keepclassmembers class com.neerly.mobile.data.dto.WalletTopupRequest {
    public synthetic <init>(java.math.BigDecimal,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
