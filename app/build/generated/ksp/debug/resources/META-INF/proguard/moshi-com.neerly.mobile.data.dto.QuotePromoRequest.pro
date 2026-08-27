-if class com.neerly.mobile.data.dto.QuotePromoRequest
-keepnames class com.neerly.mobile.data.dto.QuotePromoRequest
-if class com.neerly.mobile.data.dto.QuotePromoRequest
-keep class com.neerly.mobile.data.dto.QuotePromoRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.QuotePromoRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.QuotePromoRequest
-keepclassmembers class com.neerly.mobile.data.dto.QuotePromoRequest {
    public synthetic <init>(java.lang.String,java.math.BigDecimal,boolean,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
