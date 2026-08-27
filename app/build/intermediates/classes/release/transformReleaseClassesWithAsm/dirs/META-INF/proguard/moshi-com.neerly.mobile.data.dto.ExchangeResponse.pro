-if class com.neerly.mobile.data.dto.ExchangeResponse
-keepnames class com.neerly.mobile.data.dto.ExchangeResponse
-if class com.neerly.mobile.data.dto.ExchangeResponse
-keep class com.neerly.mobile.data.dto.ExchangeResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
